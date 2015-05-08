package edu.psu.yabt.bean;

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

import edu.psu.yabt.entity.Link;
import edu.psu.yabt.notification.LinkType;

@Stateless
@LocalBean
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LinkBean 
{
	@PersistenceContext( unitName = "yabt",
						 properties = {@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION, value = PersistenceUnitProperties.CREATE_ONLY),
										@PersistenceProperty(name = PersistenceUnitProperties.DDL_GENERATION_MODE, value = PersistenceUnitProperties.DDL_DATABASE_GENERATION)})
	private EntityManager em;
	
	public LinkBean()
	{}
	
	public LinkBean(EntityManager em)
	{
		this.em = em;
	}
	
	public void createLink(String sourceYabtId, String targetYabtId, LinkType type)
	{
		Link link = new Link();
		link.setSource(sourceYabtId);
		link.setTarget(targetYabtId);
		link.setLinkType(type);
		
		em.persist(link);
	}
	
	public void removeLink(Link link)
	{
		em.remove(link);
	}
	
	public void removeLink(String sourceYabtId, String targetYabtId, LinkType type)
	{
		Query q = em.createQuery("select l from Link l where l.source = :source and l.target = :target and l.linkType = :linkType", Link.class);
		q.setParameter("source", sourceYabtId);
		q.setParameter("target", targetYabtId);
		q.setParameter("linkType", type);
		removeLink((Link)q.getSingleResult());
	}
	
	public Collection<Link> getLinks(String yabtId)
	{
		return em.createQuery("select l from Link l where l.source = :yabtId or l.target = :yabtId", Link.class)
				.setParameter("yabtId", yabtId)
				.getResultList();
	}
	
	public Link getLink(String src, String tgt, LinkType type)
	{
		return em.createQuery("select l from Link l where l.source = :src and l.target = :tgt and l.linkType = :type", Link.class)
				.setParameter("src", src)
				.setParameter("tgt", tgt)
				.setParameter("type", type)
				.getSingleResult();
	}
}
