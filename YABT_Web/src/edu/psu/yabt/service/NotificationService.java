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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.psu.yabt.bean.NotificationBean;
import edu.psu.yabt.entity.Notification;
import edu.psu.yabt.notification.Condition;
import edu.psu.yabt.util.StringUtils;

@Path("/notification")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class NotificationService 
{
	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
	
	@EJB
	private NotificationBean notificationBean;
	
	public NotificationService()
	{}
	
	@GET
	public Collection<Object> getNotifications(@Context final HttpServletRequest config, 
											   @QueryParam("yabtId") String yabtId)
	{
		String username = config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName();
		Collection<Object> rets = new ArrayList<Object>();
		for( final Notification n : notificationBean.getNotifications(username, yabtId) )
		{
			rets.add(new Object(){
				public final Object notificationType = n.getCondition();
			});
		}
		return rets;
	}
	
	@POST
	public Response createNotifications(@Context final HttpServletRequest config,
							   @FormParam("yabtId") String yabtId,
							   @FormParam("REASSIGNED") String REASSIGN,
							   @FormParam("LINKED") String LINK,
							   @FormParam("UPDATED") String UPDATE,
							   @FormParam("ADVANCED") String ADVANCE,
							   @FormParam("COMPLETED") String COMPLETE)
	{
		String username = config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName();
		try
		{
			if( !StringUtils.isNullOrEmpty(REASSIGN) )
			{
				notificationBean.createNotification(username, Condition.REASSIGNED, yabtId);
				logger.info("Created Notification, subject="+yabtId+", user="+username+", condition="+Condition.REASSIGNED);
			}
			else
			{
				notificationBean.removeNotification(username, Condition.REASSIGNED, yabtId);
				logger.info("Deleted Notification, subject="+yabtId+", user="+username+", condition="+Condition.REASSIGNED);
			}
			if( !StringUtils.isNullOrEmpty(LINK) )
			{
				notificationBean.createNotification(username, Condition.LINKED, yabtId);
				logger.info("Created Notification, subject="+yabtId+", user="+username+", condition="+Condition.LINKED);
			}
			else
			{
				notificationBean.removeNotification(username, Condition.LINKED, yabtId);
				logger.info("Deleted Notification, subject="+yabtId+", user="+username+", condition="+Condition.LINKED);
			}
			if( !StringUtils.isNullOrEmpty(UPDATE) )
			{
				notificationBean.createNotification(username, Condition.UPDATED, yabtId);
				logger.info("Created Notification, subject="+yabtId+", user="+username+", condition="+Condition.UPDATED);
			}
			else
			{
				notificationBean.removeNotification(username, Condition.UPDATED, yabtId);
				logger.info("Deleted Notification, subject="+yabtId+", user="+username+", condition="+Condition.UPDATED);
			}
			if( !StringUtils.isNullOrEmpty(ADVANCE) )
			{
				notificationBean.createNotification(username, Condition.ADVANCED, yabtId);
				logger.info("Created Notification, subject="+yabtId+", user="+username+", condition="+Condition.ADVANCED);
			}
			else
			{
				notificationBean.removeNotification(username, Condition.ADVANCED, yabtId);
				logger.info("Deleted Notification, subject="+yabtId+", user="+username+", condition="+Condition.ADVANCED);
			}
			if( !StringUtils.isNullOrEmpty(COMPLETE) )
			{
				notificationBean.createNotification(username, Condition.COMPLETED, yabtId);
				logger.info("Created Notification, subject="+yabtId+", user="+username+", condition="+Condition.COMPLETED);
			}
			else
			{
				notificationBean.removeNotification(username, Condition.COMPLETED, yabtId);
				logger.info("Deleted Notification, subject="+yabtId+", user="+username+", condition="+Condition.COMPLETED);
			}
			return Response.ok().build();
		}
		catch(Exception e)
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@DELETE
	public Response deleteNotification(@Context final HttpServletRequest config,
							   @FormParam("yabtId") String yabtId,
							   @FormParam("condition") String condition)
	{
		String username = config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName();
		try
		{
			notificationBean.removeNotification(username, Condition.valueOf(condition), yabtId);
			return Response.ok().build();
		}
		catch(Exception e)
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
}
