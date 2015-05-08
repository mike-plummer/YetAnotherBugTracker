package edu.psu.yabt.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNLogEntry;

import edu.psu.yabt.bean.RepositoryBean;
import edu.psu.yabt.bean.WorkflowBean;
import edu.psu.yabt.bean.ActivityBean;
import edu.psu.yabt.entity.Activity;

@Path("/activity")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class ActivityService {

	private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);
	
	//Regex pattern to support 'tagged' SVN commits - used to check if commit comment contains YABT ID
	private static final Pattern yabtPattern = Pattern.compile("^YABT-[0-9]{1,4}", Pattern.CASE_INSENSITIVE);
	
	@EJB
	private WorkflowBean engineBean;
	
	@EJB
	private RepositoryBean repoBean;
	
	@EJB
	private ActivityBean activityBean;

	@GET
	@Path("/{yabtId}")
	public Response getActivityFeed(@PathParam("yabtId") String yabtId)
	{
		Collection<SVNLogEntry> entries = new ArrayList<SVNLogEntry>();

		logger.info("Returning item activity data for yabtId="+yabtId);
		List<Object> returns = new ArrayList<Object>();
		// Get Activity events
		Collection<Activity> activities = activityBean.getActivitesById(yabtId);
		logger.info("Activites returned. Count: "+activities.size());
		for (final Activity act : activities)
		{
			returns.add(new Object(){
				public long timestamp = act.getTimestamp();
				public String username = act.getAuthor();
				public String message = act.getTitle();
				public String filecount = "N/A";
				public String id = act.getYabtId() + act.getTimestamp();
				public String title = "Item Activity";
				public String secondaryTitle = act.getYabtId();
			});		
		}
		logger.info("Returning activity data, numEntries="+returns.size());
		return Response.ok(returns).build();	
	}
	
	/**
	 * Retrieves data on recent activity, returns as JSON.
	 * 
	 * Pulls SVN data and recent workflow actions
	 */
	@GET
	public Response getActivityFeed()
	{
		Collection<SVNLogEntry> entries = new ArrayList<SVNLogEntry>();
		try 
		{
			//Pull all revisions from SVN repo
			entries = repoBean.getEntriesSinceRevision(0);
			logger.info("Retrieved "+entries.size()+" revisions from repository");
		}
		catch (Exception e) 
		{
			logger.error("Failed to fetch recent SVN activity", e);
		}
		List<Object> returns = new ArrayList<Object>();
		for( final SVNLogEntry entry : entries )
		{
			String idValue = "Revision "+entry.getRevision();
			if( entry.getMessage() != null )
			{
				Matcher m = yabtPattern.matcher(entry.getMessage());
				if( m.matches() )
				{
					idValue += ", "+m.group();
				}
			}
			final String identifier = idValue;
			
			returns.add(new Object(){
				public long timestamp = entry.getDate().getTime();
				public String username = entry.getAuthor();
				public String message = entry.getMessage();
				public String filecount = "" + entry.getChangedPaths().size();
				public String id = identifier;
				public String title = "Files committed";
				public String secondaryTitle = identifier;
			});
		}
		
		// Get Activity events
		Collection<Activity> activities = activityBean.getActivities();
		logger.info("Activites returned. Count: "+activities.size());
		for (final Activity act : activities)
		{
			returns.add(new Object(){
				public long timestamp = act.getTimestamp();
				public String username = act.getAuthor();
				public String message = act.getTitle();
				public String filecount = "N/A";
				public String id = act.getYabtId() + act.getTimestamp();
				public String title = "Item Activity";
				public String secondaryTitle = act.getYabtId();
			});		
		}
		logger.info("Returning activity data, numEntries="+returns.size());
		return Response.ok(returns).build();
	}
}
