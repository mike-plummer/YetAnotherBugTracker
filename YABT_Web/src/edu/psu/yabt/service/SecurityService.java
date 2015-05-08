package edu.psu.yabt.service;

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.activiti.engine.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import edu.psu.yabt.bean.SecurityBean;
import edu.psu.yabt.bean.WorkflowBean;

@Path("/security")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class SecurityService {

	private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
	
	@EJB
	private WorkflowBean engineBean;
	
	@EJB
	private SecurityBean securityBean;
	
	@GET
	@Path("/users")
	public Response getAllUsernames()
	{
		Collection<edu.psu.yabt.entity.User> users = securityBean.getUsers();
		Collection<Object> rets = new ArrayList<Object>();
		for( final edu.psu.yabt.entity.User u : users )
		{
			rets.add(new Object(){
				public String username = u.getUsername();
			});
		}
		return Response.ok(rets).build();
	}
	
	@GET
	@Path("/username")
	public Response getUsername(@Context final HttpServletRequest config)
	{
		final String user = config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName();
		User u = null;
		try
		{
			u = engineBean.createUserQuery().userId(user).singleResult();
		}
		catch(NoResultException e)
		{}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if( u == null )
		{
			engineBean.addUser(user, "", "");
		}
		if( securityBean.getUser(user) == null )
		{
			if( auth == null || auth.getPrincipal() == null )
			{
				securityBean.createUser(user, "", "");
				logger.info("No authentication context, default dev env behavior. username="+user);
			}
			else if( auth.getPrincipal() instanceof InetOrgPerson )
			{
				InetOrgPerson userDetails = (InetOrgPerson) auth.getPrincipal();
				logger.info("Received InetOrgPerson="+userDetails);
				securityBean.createUser(user, userDetails.getMail(), userDetails.getDisplayName());
				logger.info("Registered full LDAP user type with workflow system, username="+user+", name="+userDetails.getDisplayName()+", email="+userDetails.getMail());
			}
			else if( auth.getPrincipal() instanceof LdapUserDetails )
			{
				LdapUserDetails userDetails = (LdapUserDetails) auth.getPrincipal();
				securityBean.createUser(user, "", "");
				logger.info("Registered basic LDAP user type with workflow system, username="+user);
			}
			else if( auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User )
			{
				//TODO Does the Spring object give us anything?
				org.springframework.security.core.userdetails.User userDetails = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
				securityBean.createUser(user, "", "");
				logger.info("Registered default user type with workflow system, username="+user);
			}
			else
			{
				securityBean.createUser(user, "", "");
				logger.info("Registered fallback user type with workflow system, username="+user);
			}
		}
		return Response.ok(new Object(){
			public final String username = user;
			public final String admin = Boolean.toString(config.isUserInRole("ROLE_YABT_ADMIN"));
		}).build();
	}
}
