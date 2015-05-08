package edu.psu.yabt.service;

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import edu.psu.yabt.bean.LinkBean;
import edu.psu.yabt.bean.ListenerBean;
import edu.psu.yabt.entity.Link;
import edu.psu.yabt.notification.Condition;
import edu.psu.yabt.notification.LinkType;

@Path("/link")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class LinkService 
{
	@EJB
	private LinkBean linkBean;
	
	@EJB
	private ListenerBean listenerBean;
	
	public LinkService()
	{}
	
	@GET
	@Path("/{yabtId}")
	public Collection<Object> getLinks(@PathParam("yabtId") String yabtId)
	{
		Collection<Object> rets = new ArrayList<Object>();
		for( final Link lnk : linkBean.getLinks(yabtId) )
		{
			if( lnk.getSource().equals(yabtId) )
			{
				rets.add(new Object(){
					public final Object source = lnk.getSource();
					public final Object target = lnk.getTarget();
					public final Object type = lnk.getLinkType().getText();
				});
			}
			else
			{
				rets.add(new Object(){
					public final Object source = lnk.getTarget();
					public final Object target = lnk.getSource();
					public final Object type = lnk.getLinkType().getInverseLinkType().getText();
				});
			}
		}
		return rets;
	}
	
	@POST
	public Response createLink(@Context final HttpServletRequest config,
							   @FormParam("source") String src,
							   @FormParam("target") String tgt,
							   @FormParam("type") String type)
	{
		try
		{
			linkBean.createLink(src, tgt, LinkType.valueOf(type));
			listenerBean.notifyListeners(config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName(), Condition.LINKED, src);
			return Response.ok().build();
		}
		catch(Exception e)
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@DELETE
	public Response deleteLink(@FormParam("source") String src,
							   @FormParam("target") String tgt,
							   @FormParam("type") String type)
	{
		try
		{
			linkBean.removeLink(src, tgt, LinkType.valueOf(type));
			return Response.ok().build();
		}
		catch(Exception e)
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@GET
	@Path("/type")
	public Collection<Object> getLinkTypes()
	{
		Collection<Object> rets = new ArrayList<Object>();
		for( final LinkType t : LinkType.values() )
		{
			rets.add(new Object(){
				public final Object text = t.getText();
				public final Object type = t.name();
			});
		}
		return rets;
	}
}
