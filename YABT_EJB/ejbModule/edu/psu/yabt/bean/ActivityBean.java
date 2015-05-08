package edu.psu.yabt.bean;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.persistence.Query;

import org.eclipse.persistence.config.PersistenceUnitProperties;

import edu.psu.yabt.entity.Activity;

@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ActivityBean 
{
	@PersistenceContext( unitName = "yabt",
						 properties = {@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION, value = PersistenceUnitProperties.CREATE_ONLY),
										@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION_MODE, value = PersistenceUnitProperties.DDL_DATABASE_GENERATION)})
	private EntityManager em;
	
	public ActivityBean()
	{}
	
	public ActivityBean(EntityManager em)
	{
		this.em = em;
	}
	
	public void createActivity(String yabtId, String title, String author, String timestamp)
	{
		Activity activity = new Activity();
		activity.setYabtId(yabtId);
		activity.setTitle(title);
		activity.setAuthor(author);
		activity.setTimestamp(Calendar.getInstance().getTime().getTime());
		
		em.persist(activity);
	}
	
	public void removeActivity(Activity activity)
	{
		em.remove(activity);
	}
	
	public void removeActivity(String yabtId, String title, String author, long timestamp)
	{
		Query q = em.createQuery("select l from Activity l where l.yabtId = :yabtId and l.title = :title and l.author = :author and l.timestamp = :timestamp", Activity.class);
		q.setParameter("yabtId", yabtId);
		q.setParameter("title", title);
		q.setParameter("author", author);
		q.setParameter("timestamp", timestamp);
		removeActivity((Activity)q.getSingleResult());
	}
	
	public Collection<Activity> getActivities()
	{
		return em.createQuery("select l from Activity l ", Activity.class)
				.getResultList();
	}
	
	public Collection<Activity> getActivitesById(String yabtId){
		return em.createQuery("select l from Activity l where l.yabtId = :yabtId", Activity.class)
				.setParameter("yabtId", yabtId)
				.getResultList();	
	}
	
	public Activity getActivity(String yabtId, String title, String author, long timestamp)
	{
		return em.createQuery("select l from Activity l where l.yabtId = :yabtId and l.title = :title and l.author = :author and l.timestamp = :timestamp", Activity.class)
				.setParameter("yabtId", yabtId)
				.setParameter("title", title)
				.setParameter("author", author)
				.setParameter("timestamp", timestamp)
				.getSingleResult();
	}
}
