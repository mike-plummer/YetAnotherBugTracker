package edu.psu.yabt.bean;

import java.util.Collection;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.Query;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.psu.yabt.entity.Notification;
import edu.psu.yabt.notification.Condition;

@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class NotificationBean 
{
	private static final Logger logger = LoggerFactory.getLogger(NotificationBean.class);
	
	@PersistenceContext( unitName = "yabt",
						 properties = {@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION, value = PersistenceUnitProperties.CREATE_ONLY),
										@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION_MODE, value = PersistenceUnitProperties.DDL_DATABASE_GENERATION)})
	private EntityManager em;
	
	public NotificationBean()
	{}
	
	public void createNotification(String username, Condition condition, String yabtId)
	{
		if( getNotification(username, condition, yabtId) == null )
		{
			Notification notification = new Notification();
			notification.setUsername(username);
			notification.setCondition(condition);
			notification.setSubject(yabtId);
			em.persist(notification);
			logger.info("Persisted notification");
		}
		else
		{
			logger.info("Notification already exists, skipped.");
		}
	}
	
	public void removeNotification(Notification notification)
	{
		em.remove(notification);
	}
	
	public void removeNotification(String username, Condition condition, String yabtId)
	{
		Query q = em.createQuery("delete from Notification l where l.username = :username and l.condition = :condition and l.subject = :subject", Notification.class);
		q.setParameter("username", username);
		q.setParameter("condition", condition);
		q.setParameter("subject", yabtId);
		q.executeUpdate();
	}
	
	public Collection<Notification> getNotifications(String username)
	{
		return em.createQuery("select l from Notification l where l.username = :username", Notification.class)
				.setParameter("username", username)
				.getResultList();
	}
	
	public Collection<Notification> getNotifications(String username, String yabtId)
	{
		logger.info("Retrieving notifications for user="+username+", yabtId="+yabtId);
		return em.createQuery("select l from Notification l where l.username = :username and l.subject = :subject", Notification.class)
				.setParameter("username", username)
				.setParameter("subject", yabtId)
				.getResultList();
	}
	
	public Notification getNotification(String username, Condition condition, String yabtId)
	{
		try
		{
			logger.info("Retrieving notifications for user="+username+", condition="+condition+", yabtId="+yabtId);
			return em.createQuery("select l from Notification l where l.username = :username and l.condition = :condition and l.subject = :subject", Notification.class)
					.setParameter("username", username)
					.setParameter("condition", condition)
					.setParameter("subject", yabtId)
					.getSingleResult();
		}
		catch(NoResultException e)
		{
			logger.info("Failed to find notification");
			return null;
		}
	}
}
