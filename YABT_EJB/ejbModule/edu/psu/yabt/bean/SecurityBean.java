
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

import edu.psu.yabt.entity.User;

@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SecurityBean 
{
	@PersistenceContext( unitName = "yabt",
			properties = {@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION, value = PersistenceUnitProperties.CREATE_ONLY),
			@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION_MODE, value = PersistenceUnitProperties.DDL_DATABASE_GENERATION)})
	private EntityManager em;

	public SecurityBean()
	{}

	public void createUser(String username, String email, String name)
	{
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setName(name);

		em.persist(user);
	}

	public void removeUser(User user)
	{
		em.remove(user);
	}

	public void removeUser(String username)
	{
		Query q = em.createQuery("select u from User u where u.username = :username", User.class);
		q.setParameter("username", username);
		removeUser((User)q.getSingleResult());
	}

	public Collection<User> getUsers()
	{
		return em.createQuery("select u from User u", User.class)
				.getResultList();
	}

	public User getUser(String username)
	{
		try
		{
			return em.createQuery("select u from User u where u.username = :username", User.class)
					.setParameter("username", username)
					.getSingleResult();
		}
		catch(NoResultException e)
		{
			return null;
		}
	}
}

