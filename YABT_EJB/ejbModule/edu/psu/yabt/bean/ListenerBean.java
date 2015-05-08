package edu.psu.yabt.bean;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.Query;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.psu.yabt.entity.Link;
import edu.psu.yabt.entity.Notification;
import edu.psu.yabt.entity.User;
import edu.psu.yabt.notification.Condition;
import edu.psu.yabt.notification.Notifier;
import edu.psu.yabt.notification.WorkflowListener;
import edu.psu.yabt.util.StringUtils;

@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ListenerBean 
{
	private static final Logger logger = LoggerFactory.getLogger(ListenerBean.class);
	
	private static final BlockingQueue<Runnable> eventQueue = new ArrayBlockingQueue<Runnable>(200);
	private static final ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 10, 2, TimeUnit.SECONDS, eventQueue, new ThreadPoolExecutor.CallerRunsPolicy());
	
	private static final Collection<WeakReference<WorkflowListener>> listeners = new CopyOnWriteArrayList<WeakReference<WorkflowListener>>();
	
	@EJB
	private WorkflowBean workflowBean;
	
	@EJB
	private SecurityBean securityBean;

	@EJB
	private ActivityBean activityBean;
	
	@PersistenceContext( unitName = "yabt",
			 properties = {@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION, value = PersistenceUnitProperties.CREATE_ONLY),
							@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION_MODE, value = PersistenceUnitProperties.DDL_DATABASE_GENERATION)})
	private EntityManager em;
	
	public ListenerBean()
	{}
	
	/**
	 * Roll through registered notifications, attempt to send emails based on {@code Condition}.
	 * @param username
	 * @param condition
	 * @param subject
	 */
	@SuppressWarnings("unchecked")
	public void notifyListeners(final String username, final Condition condition, final String subject)
	{
		//If we have registered any other objects interested in receiving notifications, notify them that
		//an event occurred
		for( final WeakReference<WorkflowListener> listener : listeners )
		{
			pool.execute(new Runnable(){
				public void run(){
					listener.get().workflowEventOccurred(username, condition, subject);
				}
			});
		}

		//Pull all Notifications from the database
		Query q = em.createQuery("select n from Notification n where n.condition = :condition and n.subject = :subject", Notification.class);
		q.setParameter("condition", condition);
		q.setParameter("subject", subject);
		Collection<Notification> notifications = q.getResultList();
		
		Task task = null;
		ProcessDefinition def = null;
		String bodyText = "";
		//A non-null subject indicates a workflow item was updated. Subject will be null in certain circumstances
		//such as a new user being registered.
		if( !StringUtils.isNullOrEmpty(subject))
		{
			//Try to pull the Task that this event applies to
			task = workflowBean.createTaskQuery().processInstanceBusinessKey(subject).singleResult();
			if( task != null )
			{
				//Assuming we found a matching task (we should assuming we had a non-null subject and the event isn't a COMPLETE event),
				//pull the process type the task is a part of so we can reference it in the email(s)
				def = workflowBean.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
			}
		}
		//Based on the Condition type, build the body text of the email
		switch( condition )
		{
			case REASSIGNED:
				bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a> has been reassigned to user "+task.getAssignee()+".";
				break;
			case CREATED:
				bodyText = "A new "+def.getKey()+" has been created and assigned to "+task.getAssignee()+".";
				break;
			case LINKED:
				bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a> has been linked to a new item by user "+username+".";
				break;
			case UPDATED:
				bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a> has been updated by user "+username+".";
				break;
			case ADVANCED:
				bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a> has been advanced to state '"+task.getName()+"' by user "+username+".";
				break;
			case COMPLETED:
				bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a> has been completed by user "+username+".";
				break;
			case NEW_USER:
				bodyText = "New user "+username+" has been registered with the system.";
				break;
			default:
				logger.warn("Unknown notification condition encountered, condition="+condition);
		}
		/* Generate activity entries based on the relevant conditions
		 */
		switch (condition)
		{
			case REASSIGNED:
			case CREATED:
			case COMPLETED:
				logger.info("Generating an activity entry. YabtID=" + subject + ", desc=" + bodyText + ", user=" + username);
				activityBean.createActivity(subject, bodyText, username, null);
				break;
			default:
				logger.info("Activity entry not generated for all conditions (" + condition + ").");
				break;
		}
		
		//For each Notification from the list (including the one we built above)
		for( Notification notification : notifications )
		{
			User user = securityBean.getUser(notification.getUsername());		
			
			logger.info("Generating a notification. id="+notification.getSubject()+", condition="+notification.getCondition()+", username="+notification.getUsername());
			//Attempt to pull the email address of the user for this Notification
			
			String recipient = user != null ? user.getEmail() : "";
			if( StringUtils.isNullOrEmpty(recipient) )
			{
				try 
				{
					//Try to build and validate username to see if it's an email address
					InternetAddress emailAddr = new InternetAddress(user.getUsername());
					emailAddr.validate();
					//If it validates as an email, then use it
					recipient = user.getUsername();
				} 
				catch (AddressException ex) 
				{
					//If username not an email, and no email provided, then skip
					logger.warn("No email address found for user '"+username+"'. Skipping notification.");
					continue;
				}
			}
			//Using the email address and body text we built above, try to send an email
			try {
				Notifier.generateNotificationUsingExternalSMTP(Arrays.asList(new String[]{recipient}), bodyText);
			} catch (AddressException e) {
				logger.error("Invalid address specified for notification, notification="+notification, e);
			} catch (MessagingException e) {
				logger.error("Error sending email, notification="+notification, e);
			}
		}
		//If this event Updated, Advanced, or Completed an assigned task, then we should notify the assignees of linked
		//items since this may alter what they need to do for their assignments.
		if( condition == Condition.UPDATED || condition == Condition.ADVANCED || condition == Condition.COMPLETED )
		{
			logger.info("Detected update, advance, or complete event. Checking links...");
			//Pull all Links where the subject of this event is either the source or target
			q = em.createQuery("select l from Link l where l.source = :source or l.target = :target", Link.class);
			q.setParameter("source", subject);
			q.setParameter("target", subject);
			Collection<Link> links = q.getResultList();
			logger.info("Found "+links.size()+" links to source of event");
			
			//For each Link...
			for( Link l : links )
			{
				String notifyYabtId = null;
				String relation = null;
				//If this event is the 'source' of the link then pull the 'target' of the link from the database
				//and build the link type text so we can use it in the email
				if( subject.equals(l.getSource()))
				{
					notifyYabtId = l.getTarget();
					relation = l.getLinkType().getText();
				}
				//Otherwise, pull the 'source' of the link from the database and build the link type text
				//using the inverse of the link definition (to keep the grammar correct)
				else
				{
					notifyYabtId = l.getSource();
					relation = l.getLinkType().getInverseLinkType().getText();
				}
				//Pull the appropriate task out of the database
				Task linkedTask = workflowBean.createTaskQuery().processInstanceBusinessKey(notifyYabtId).singleResult();
				//Assuming we found a task (we should unless it has been completed already)...
				if( linkedTask != null )
				{
					logger.info("Generating a notification based on link. id="+subject+", assignee="+linkedTask.getAssignee()+", condition="+condition);
					bodyText = "";
					//Build body text for the email including the item that was updated, the link type, and the item assigned to the person being notified which is
					//linked to the updated item in this event
					switch( condition )
					{
						case UPDATED:
							bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a>, which "+relation+" your assignment <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+notifyYabtId+"\">"+notifyYabtId+"</a>, has been updated.";
							break;
						case ADVANCED:
							bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a>, which "+relation+" your assignment <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+notifyYabtId+"\">"+notifyYabtId+"</a>, has been advanced to a new state.";
							break;
						case COMPLETED:
							bodyText = "Item <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+subject+"\">"+subject+"</a>, which "+relation+" your assignment <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId="+notifyYabtId+"\">"+notifyYabtId+"</a>, has been completed.";
							break;
					}
					//Try to pull email address of assignee of linked task
					User user = securityBean.getUser(linkedTask.getAssignee());
					if( user == null )
					{
						continue;
					}
					String recipient = user.getEmail();
					//If no email address available, skip this notification
					if( StringUtils.isNullOrEmpty(recipient) )
					{
						try 
						{
							//Try to build and validate username to see if it's an email address
							InternetAddress emailAddr = new InternetAddress(user.getUsername());
							emailAddr.validate();
							//If it validates as an email, then use it
							recipient = user.getUsername();
						} 
						catch (AddressException ex) 
						{
							//If username not an email, and no email provided, then skip
							logger.warn("No email address found for user '"+username+"'. Skipping notification.");
							continue;
						}
					}
					//Try to send notification
					try {
						Notifier.generateNotificationUsingExternalSMTP(Arrays.asList(new String[]{recipient}), bodyText);
					} catch (AddressException e) {
						logger.error("Invalid address specified for notification", e);
					} catch (MessagingException e) {
						logger.error("Error sending email", e);
					}
				}
			}
		}
	}

	public void addListener(WorkflowListener listener)
	{
		listeners.add(new WeakReference<WorkflowListener>(listener));
	}
	
	public void removeListener(WorkflowListener listener)
	{
		for( Iterator<WeakReference<WorkflowListener>> iter = listeners.iterator(); iter.hasNext(); )
		{
			WeakReference<WorkflowListener> ref = iter.next();
			if( ref == null || ref.get() == null || ref.get().equals(listener) )
			{
				listeners.remove(ref);
			}
		}
	}
}
