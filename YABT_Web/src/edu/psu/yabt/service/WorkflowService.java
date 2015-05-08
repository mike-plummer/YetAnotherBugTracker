package edu.psu.yabt.service;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;

import edu.psu.yabt.bean.ListenerBean;
import edu.psu.yabt.bean.WorkflowBean;
import edu.psu.yabt.notification.Condition;
import edu.psu.yabt.notification.LinkType;
import edu.psu.yabt.util.StringUtils;
import edu.psu.yabt.workflow.WorkflowVariables;

/**
 * Provides JSON-based web-services to enable creation, control, and status on workflow components.
 * Services can be accessed using standard HTTP methods as annotated using the following URL:
 * 	http://{hostname}:8080/YABT_Web/service/workflow/{Method Level Path}
 * 
 * The following activities can be completed:
 * - Get Process Engine Name
 * - Get List of Active Processes(Requirements, Assignments, and Tests)
 * - Get List of Deployed Processes(Types of Processes that can be started)
 * - Start new Instance of Process by Type
 * - Advance a Process to the next stage
 * - Get list of pending tasks in processes
 * - Update a pending task
 * - Get diagram for process of type
 * - Upload a file to attach to a process
 * - Get list of attachments by process
 * - Retrieve an attachment
 * 
 * @author mplummer
 *
 */
@Path("/workflow")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class WorkflowService {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);
	
	private SimpleDateFormat utcFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
	
	@EJB
	private WorkflowBean engineBean;

	@EJB
	private ListenerBean listenerBean;
	
	/**
	 * Get name of current process engine instance
	 * @return String
	 */
	@GET
	@Path("/name")
	public String getProcessEngineName() {
		return engineBean.getEngineName();
	}

	@GET
	public Collection<Object> getYabtIds(@QueryParam("linkType") String linkType, final @QueryParam("linkSrc") String linkSrcYabtId )
	{
		Collection<ProcessInstance> procs = null;
		
		String linkSrcType = engineBean.createProcessInstanceQuery().processInstanceBusinessKey(linkSrcYabtId).singleResult().getProcessDefinitionId();
		linkSrcType = linkSrcType.substring(0, linkSrcType.indexOf(":"));
		ProcessInstanceQuery query = engineBean.createProcessInstanceQuery();
		Collection<Object> rets = new ArrayList<Object>();
		if( !StringUtils.isNullOrEmpty(linkType) )
		{
			LinkType l = LinkType.valueOf(linkType);
			switch(l)
			{
				case BLOCKS:
				case IS_BLOCKED_BY:
				case IS_RELATED_TO:
					break;
				case IMPLEMENTS:
					if(!"Assignment".equalsIgnoreCase(linkSrcType))
					{
						return rets;
					}
					query = query.processDefinitionKey("Requirement");
					break;
				case IS_IMPLEMENTED_BY:
					if(!"Requirement".equalsIgnoreCase(linkSrcType))
					{
						return rets;
					}
					query = query.processDefinitionKey("Assignment");
					break;
				case TESTS:
					if(!"Test".equalsIgnoreCase(linkSrcType))
					{
						return rets;
					}
					query = query.processDefinitionKey("Assignment");
					break;
				case IS_TESTED_BY:
					if(!"Assignment".equalsIgnoreCase(linkSrcType))
					{
						return rets;
					}
					query = query.processDefinitionKey("Test");
					break;
				case IS_DEPENDENCY_OF:
				case DEPENDS_ON:
					query = query.processDefinitionKey(linkSrcType);
					break;
			}
		}
		procs = query.list();
		
		for( final ProcessInstance inst : procs )
		{
			if( inst == null || inst.getBusinessKey() == null || inst.getBusinessKey().equals(linkSrcYabtId) )
			{
				continue;
			}
			rets.add(new Object(){
				public final Object yabtId = inst.getBusinessKey();
			});
		}
		return rets;
	}
	
	/**
	 * Get data on processes. All params are optional and if provided restrict search results.
	 * @param type String Only match processes of given type (Default: Requirement, Assignment, Test)
	 * @param yabtId String YABT ID value for process instance (Format: YABT-####)
	 * @param active boolean Return active processes only, default true
	 * @return JSON
	 */
	@GET
	@Path("/process")
	public List<Object> getProcesses(@QueryParam("type") String type, 
									 @QueryParam("key") String yabtId,
									 @DefaultValue("true") @QueryParam("active") boolean active) {
		ProcessInstanceQuery query = engineBean.createProcessInstanceQuery();
		
		if( !StringUtils.isNullOrEmpty(yabtId) )
		{
			query = query.processInstanceBusinessKey(yabtId);
		}
		if( active )
		{
			query = query.active();
		}
		if( !StringUtils.isNullOrEmpty(type) )
		{
			query = query.processDefinitionKey(type);
		}
		List<ProcessInstance> instances = query.list();
		List<Object> returns = new ArrayList<Object>();
		for( final ProcessInstance inst : instances )
		{
			returns.add(new Object(){
				public Object businessKey = inst.getBusinessKey();
				public Object id = inst.getId();
				public Object processDefinitionId = inst.getProcessDefinitionId();
				public Object processInstanceId = inst.getProcessInstanceId();
			});
		}
		return returns;
	}
	
	/**
	 * Get data on deployed process types. Default types include Requirement,Assignment,Test. All
	 * params are optional and if provided restrict search results
	 * @param type String Only match processes of given type (Default: Requirement, Assignment, Test)
	 * @param active boolean Return active deployments only, default true
	 * @return JSON
	 */
	@GET
	@Path("/processDef")
	public List<Object> getProcessDefinitions(@QueryParam("type") String type, 
											  @DefaultValue("true") @QueryParam("active") boolean active) {
		ProcessDefinitionQuery query = engineBean.createProcessDefinitionQuery();
		if( !StringUtils.isNullOrEmpty(type) )
		{
			query = query.processDefinitionName(type);
		}
		if( active )
		{
			query = query.active();
		}
		
		List<ProcessDefinition> defs = query.list();
		List<Object> returns = new ArrayList<Object>();
		for( final ProcessDefinition def : defs )
		{
			returns.add(new Object(){
				public Object category = def.getCategory();
				public Object name = def.getName();
				public Object id = def.getId();
				public Object key = def.getKey();
				public Object deploymentId = def.getDeploymentId();
				public Object description = def.getDescription();
				public Object diagramResourceName = def.getDiagramResourceName();
				public Object resourceName = def.getResourceName();
				public Object version = def.getVersion();
			});
		}
		return returns;
	}

	/**
	 * Start a new process instance of the specified type and assigns the first task to the specified user 
	 * @param type String Type to start. Default types include Requirement,Assignment, and Test
	 * @param user String username to assign first task to
	 * @return JSON New process' YABT ID
	 */
	@POST
	@Path("/process/{type}")
	public Object startProcess(@Context final HttpServletRequest config,
							   @PathParam("type") String type,
							   @FormParam("assignee") String assignee ) {
		logger.info("Creating process, type="+type+", assignee="+assignee);
		final String busKey = engineBean.startProcess(config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName(), type);
		Task tsk = engineBean.createTaskQuery().processInstanceBusinessKey(busKey).active().singleResult();
		engineBean.reassignTask(assignee, tsk.getId(), assignee);
		return new Object(){
			public String yabtId = busKey;
		};
	}
	
	@PUT
	@Path("/process/reassign")
	public Response reassign(@Context final HttpServletRequest config,
							 @FormParam("yabtId") String yabtId,
							 @FormParam("assignee") String assignee )
	{
		Task tsk = engineBean.createTaskQuery().processInstanceBusinessKey(yabtId).singleResult();
		engineBean.reassignTask(config.getUserPrincipal().getName(), tsk.getId(), assignee);
		return Response.ok().build();
	}
	
	/**
	 * Move process instance to the next state. 'fail' param is optional and defaults to false,
	 * specify as true if process should, if available, take the failure branch of the workflow
	 * @param yabtId String YABT ID of process to advance
	 * @param failure boolean optional If true, take failure branch if available
	 * @return HTTP response indicating outcome
	 */
	@PUT
	@Path("/process/{yabtId}")
	public Response advanceProcess(@Context final HttpServletRequest config,
								   @PathParam("yabtId") String yabtId, 
								   @DefaultValue("false") @FormParam("fail") boolean failure )
	{
		try
		{
			engineBean.advanceProcess(config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName(), yabtId, failure);
			return Response.ok().build();
		}
		catch(Exception e)
		{
			logger.error("Error advancing process, yabtId="+yabtId+", failure="+failure, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Get current tasks (a Task is the current state of a process). All parameters are optional, if provided
	 * they serve to filter the search results.
	 * @param assignee String username task(s) are assigned to
	 * @param dueDate String Date parseable by DateFormat.parse(..)
	 * @param dateMethod String Search method to use on dueDate. Options: BEFORE, AFTER
	 * @param yabtId String YABT ID of process
	 * @param description String perform a SQL 'LIKE' operation on the Task description with this value
	 * @param type String Return tasks for given process type. Default types: Requiremnt, Assignment, Test
	 * @return
	 * @throws ParseException
	 */
	@GET
	@Path("/task")
	public Response getTasks(
			@QueryParam("assignee") String assignee,
			@QueryParam("dueDate") String dueDate,
			@QueryParam("dateMethod") String dateMethod,
			@QueryParam("yabtId") String yabtId,
			@QueryParam("title") String title,
			@QueryParam("description") String description,
			@QueryParam("type") String type,
			@QueryParam("active") Boolean active,
			@QueryParam("inactive") Boolean inactive) throws ParseException 
	{
		logger.info("Searching for tasks");
		logger.info("Params: assignee="+assignee+", dueDate="+dueDate+", dateMethod="+dateMethod+", yabtId="+yabtId+", description="+description+", type="+type);
		
		List<Task> tsks = new ArrayList<Task>();
		List<HistoricTaskInstance> historicTasks = new ArrayList<HistoricTaskInstance>();
		List<Object> returns = new ArrayList<Object>();
		
		if( active )
		{
			TaskQuery query = engineBean.createTaskQuery();
			query = query.active();
			if (!StringUtils.isNullOrEmpty(assignee)) {
				query = query.taskAssignee(assignee);
			}
			if (!StringUtils.isNullOrEmpty(dueDate)) {
				Date date = utcFormat.parse(dueDate);
				if (!StringUtils.isNullOrEmpty(dateMethod)) {
					if ("BEFORE".equals(dateMethod)) {
						query = query.dueBefore(date);
					} else if ("AFTER".equals(dateMethod)) {
						query = query.dueAfter(date);
					}
				} else {
					query = query.dueDate(date);
				}
			}
			if( !StringUtils.isNullOrEmpty(yabtId) )
			{
				query = query.processInstanceBusinessKey(yabtId);
			}
			if( !StringUtils.isNullOrEmpty(title))
			{
				query = query.processVariableValueEqualsIgnoreCase(WorkflowVariables.TITLE, title);
			}
			if( !StringUtils.isNullOrEmpty(description))
			{
				query = query.processVariableValueEqualsIgnoreCase(WorkflowVariables.DESCRIPTION, description);
			}
			if( !StringUtils.isNullOrEmpty(type) )
			{
				query = query.processDefinitionName(type);
			}
			
			tsks = query.list();
		}
		
		if( inactive )
		{
			HistoricTaskInstanceQuery historicQuery = engineBean.createHistoricTaskInstanceQuery();
			historicQuery = historicQuery.processFinished();
			if (!StringUtils.isNullOrEmpty(assignee)) {
				historicQuery = historicQuery.taskAssignee(assignee);
			}
			if (!StringUtils.isNullOrEmpty(dueDate)) {
				Date date = utcFormat.parse(dueDate);
				if (!StringUtils.isNullOrEmpty(dateMethod)) {
					if ("BEFORE".equals(dateMethod)) {
						historicQuery = historicQuery.taskDueBefore(date);
					} else if ("AFTER".equals(dateMethod)) {
						historicQuery = historicQuery.taskDueAfter(date);
					}
				} else {
					historicQuery = historicQuery.taskDueDate(date);
				}
			}
			if( !StringUtils.isNullOrEmpty(title))
			{
				//TODO Doesn't search case-insensitive
				historicQuery = historicQuery.processVariableValueEquals(WorkflowVariables.TITLE, title);
			}
			if( !StringUtils.isNullOrEmpty(description))
			{
				//TODO Doesn't search case-insensitive
				historicQuery = historicQuery.processVariableValueEquals(WorkflowVariables.DESCRIPTION, description);
			}
			if( !StringUtils.isNullOrEmpty(yabtId) )
			{
				final HistoricProcessInstance inst = engineBean.createHistoricProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult();
				if( inst != null )
				{
					historicQuery = historicQuery.processInstanceId(inst.getId());
				}
			}
			if( !StringUtils.isNullOrEmpty(type) )
			{
				historicQuery = historicQuery.processDefinitionName(type);
			}
			
			historicTasks = historicQuery.list();
		}
		
		logger.info("Found "+historicTasks.size()+" historic matches, parsing into JSON");
		
		for( HistoricTaskInstance historicTask : historicTasks )	//Either bad ID provided or process has already been completed
		{
			final HistoricProcessInstance inst = engineBean.createHistoricProcessInstanceQuery().processInstanceId(historicTask.getProcessInstanceId()).singleResult();
			if( inst == null )
			{
				continue;
			}
			logger.info("Found historic process matching YabtId="+yabtId);
			final HistoricTaskInstance tsk = engineBean.createHistoricTaskInstanceQuery().processInstanceId(inst.getId()).orderByHistoricTaskInstanceEndTime().desc().list().get(0);
			final HistoricTaskInstance firstTsk = engineBean.createHistoricTaskInstanceQuery().processInstanceId(inst.getId()).orderByHistoricActivityInstanceStartTime().asc().list().get(0);
			final ProcessDefinition def = engineBean.createProcessDefinitionQuery().processDefinitionId(inst.getProcessDefinitionId()).singleResult();
			Object varEntity = engineBean.getHistoricProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.DESCRIPTION);
			final String desc = (varEntity == null) ? "No Description" : (String)varEntity;
			varEntity = engineBean.getHistoricProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.TITLE);
			final String t = (varEntity == null) ? "No Title" : (String)varEntity;
			varEntity =  engineBean.getHistoricProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.ETC);
			final Integer est = (varEntity == null) ? 0 : (Integer)varEntity;
			varEntity =  engineBean.getHistoricProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.WORK_LOGGED);
			final Integer wl = (varEntity == null) ? 0 : (Integer)varEntity;
			final DateFormat df = new SimpleDateFormat("MM-dd-yyyy");
			returns.add(new Object(){
				public Object assignee = tsk.getAssignee();
				public Object taskStartTime = tsk.getStartTime() == null ? "" : utcFormat.format(tsk.getStartTime());
				public Object delegationState = "N/A";
				public Object description = desc;
				public Object taskDueDate = tsk.getDueDate();
				public Object taskDueDateTxt = tsk.getDueDate() == null ? "" : utcFormat.format(tsk.getDueDate());
				public Object taskDueDateTxt2 = tsk.getDueDate() == null ? "" : df.format(tsk.getDueDate());
				public Object executionId = tsk.getExecutionId();
				public Object id = tsk.getId();
				public Object name = tsk.getName();
				public Object title = t;
				public Object owner = tsk.getOwner();
				public Object parentId = tsk.getParentTaskId();
				public Object priority = tsk.getPriority();
				public Object processDefinitionId = tsk.getProcessDefinitionId();
				public Object processInstanceId = tsk.getProcessInstanceId();
				public Object yabtId = inst.getBusinessKey();
				public Object type = def.getName();
				public Object definitionKey = tsk.getTaskDefinitionKey();
				public Object submitDate = firstTsk.getStartTime() == null ? "" : utcFormat.format(firstTsk.getStartTime());
				public Object etc = est;
				public Object workLogged = wl;
				public Object[] transitions = new Object[0];
			});
		}
		
		logger.info("Found "+tsks.size()+" matches, parsing into JSON");
		
		for( final Task tsk : tsks )
		{
			final ProcessInstance inst = engineBean.createProcessInstanceQuery().processInstanceId(tsk.getProcessInstanceId()).singleResult();
			ExecutionEntity entity = (ExecutionEntity)inst;
			final ProcessDefinitionEntity pde = engineBean.getDeployedProcessDefinition(tsk.getProcessDefinitionId());
			List<PvmTransition> transitions = pde.findActivity(entity.getActivityId()).getOutgoingTransitions();
			final List<Object> destNodes = new ArrayList<Object>();
			for( final PvmTransition transition : transitions )
			{
				Object exp = transition.getProperty(BpmnParse.PROPERTYNAME_CONDITION_TEXT);
				final Object cond = (exp == null) ? "": exp;
				destNodes.add(new Object(){
					public final String name = (String) transition.getDestination().getProperty("name");
					public final String condition = cond.toString();
				});
			}
			final HistoricTaskInstance firstTsk = engineBean.createHistoricTaskInstanceQuery().processInstanceId(inst.getId()).orderByHistoricActivityInstanceStartTime().asc().list().get(0);
			String descVar = (String)engineBean.getProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.DESCRIPTION);
			final String desc = (descVar == null) ? "No Description" : descVar;
			String titleVar = (String)engineBean.getProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.TITLE);
			final String t = (titleVar == null) ? "No Title" : titleVar;
			Integer etcVar = (Integer)engineBean.getProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.ETC);
			final Integer est = (etcVar == null ) ? 0 : etcVar;
			Integer logVar = (Integer)engineBean.getProcessVariable(tsk.getProcessInstanceId(), WorkflowVariables.WORK_LOGGED);
			final Integer wl = (logVar == null) ? 0 : logVar;
			final DateFormat df = new SimpleDateFormat("MM-dd-yyyy");
			returns.add(new Object(){
				public Object assignee = tsk.getAssignee();
				public Object taskStartTime = tsk.getCreateTime() == null ? "" : utcFormat.format(tsk.getCreateTime());
				public Object delegationState = "N/A";
				public Object description = desc;
				public Object taskDueDate = tsk.getDueDate();
				public Object taskDueDateTxt = tsk.getDueDate() == null ? "" : utcFormat.format(tsk.getDueDate());
				public Object taskDueDateTxt2 = tsk.getDueDate() == null ? "" : df.format(tsk.getDueDate());
				public Object executionId = tsk.getExecutionId();
				public Object id = tsk.getId();
				public Object title = t;
				public Object name = tsk.getName();
				public Object owner = tsk.getOwner();
				public Object parentId = tsk.getParentTaskId();
				public Object priority = tsk.getPriority();
				public Object processDefinitionId = tsk.getProcessDefinitionId();
				public Object processInstanceId = tsk.getProcessInstanceId();
				public Object yabtId = inst.getBusinessKey();
				public Object type = pde.getName();
				public Object definitionKey = tsk.getTaskDefinitionKey();
				public Object submitDate = firstTsk.getStartTime() == null ? "" : utcFormat.format(firstTsk.getStartTime());
				public Object reporter = firstTsk.getAssignee();
				public Object etc = est;
				public Object workLogged = wl;
				public Object[] transitions = destNodes.toArray();
			});
		}
		
		return Response.ok(returns).build();
	}
	
	@PUT
	@Path("/task")
	public Response editTask(@Context final HttpServletRequest config,
							 @FormParam("yabtId") String yabtId,
							 @FormParam("title") String title,
							 @FormParam("description") String description,
							 @FormParam("dueDate") String dueDate,
							 @FormParam("etc") Integer etc,
							 @FormParam("workLogged") Integer workLogged,
							 @FormParam("priority") int priority)
	{
		String username = config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName();
		Task task = engineBean.createTaskQuery().processInstanceBusinessKey(yabtId).singleResult();
		engineBean.setProcessVariable(username, task.getProcessInstanceId(), WorkflowVariables.TITLE, title);
		engineBean.setProcessVariable(username, task.getProcessInstanceId(), WorkflowVariables.DESCRIPTION, description);
		engineBean.setProcessVariable(username, task.getProcessInstanceId(), WorkflowVariables.ETC, etc == null ? 0 : etc);
		engineBean.setProcessVariable(username, task.getProcessInstanceId(), WorkflowVariables.WORK_LOGGED, workLogged == null ? 0 : workLogged);
		try {
			task.setDueDate(utcFormat.parse(dueDate));
		} catch (ParseException e) {
			logger.error("Failed to parse due date", e);
		}
		task.setPriority(priority);
		engineBean.updateTask(username, task);
		listenerBean.notifyListeners(username, Condition.UPDATED, engineBean.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult().getBusinessKey());
		logger.info("Saved task edit, title="+title+", description="+description+", pri="+priority);
		return Response.ok().build();
	}
	
	/**
	 * Update the specified task, set property to value
	 * @param taskId String Task ID to update (Note: Not a YABT ID)
	 * @param property String Property to set
	 * @param value String Value to set
	 * @return HTTP response code
	 */
	@PUT
	@Path("/task/{taskId}/{property}/{value}")
	public Response updateTask(@Context final HttpServletRequest config,
							   @PathParam("taskId") String taskId, 
							   @PathParam("property") String property, 
							   @PathParam("value") String value)
	{
		try
		{
			String username = config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName();
			Task task = engineBean.createTaskQuery().taskId(taskId).singleResult();
			ProcessInstance inst = engineBean.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
			if( property.equals("ASSIGNEE") )
			{
				engineBean.reassignTask(username, taskId, value);
			}
			else if( property.equals("OWNER") )
			{
				task.setOwner(value);
				engineBean.updateTask(username, task);
			}
			else if( property.equals("PRIORITY") )
			{
				task.setPriority(Integer.parseInt(value));
				engineBean.updateTask(username, task);
			}
			else if( property.equals("DESCRIPTION") )
			{
				engineBean.setProcessVariable(username, inst.getProcessInstanceId(), WorkflowVariables.DESCRIPTION, value);
			}
			else if( property.equals("DUEDATE") )
			{
				task.setDueDate(utcFormat.parse(value));
				engineBean.updateTask(username, task);
			}
			else if( property.equals("TITLE") )
			{
				engineBean.setProcessVariable(username, inst.getProcessInstanceId(), WorkflowVariables.TITLE, value);
			}
			return Response.ok().build();
		}
		catch(Exception e)
		{
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Retrieve PNG image of specified process type's workflow
	 * @param type String Process type. Defaults:Requiremnt,Assignment,Test
	 * @return image/png
	 */
	@GET
	@Path("/processDiagram/instance/{yabtId}")
	@Produces("image/png")
	public Response getProcessInstanceDiagram(@PathParam("yabtId") String yabtId)
	{
		InputStream diagram = engineBean.buildProcessInstanceDiagram(yabtId);
		if( diagram != null )
		{
			
		    return Response.ok(diagram).build();
		}
		else
		{
			return Response.noContent().build();
		}
	}
	
	/**
	 * Retrieve PNG image of specified process type's workflow
	 * @param type String Process type. Defaults:Requiremnt,Assignment,Test
	 * @return image/png
	 */
	@GET
	@Path("/processDiagram/{type}")
	@Produces("image/png")
	public Response getProcessDiagram(@PathParam("type") String type)
	{
		InputStream diagram = engineBean.buildProcessDiagram(type);
		if( diagram != null )
		{
			
		    return Response.ok(diagram).build();
		}
		else
		{
			return Response.noContent().build();
		}
	}
	
	/**
	 * Upload a file and attach to the specified process instance. Requires submission as Multipart Form Data.
	 * @param yabtId String YABT ID of process to attach to
	 * @param name String Name of attachment - not required to be the filename
	 * @param description String Description of attachment. Optional
	 * @param content InputStream file content
	 * @param fileDetail Not caller provided - form metadata
	 * @return String Attachment ID
	 */
	@POST
	@Path("/attachment/{yabtId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadAttachment(@Context final HttpServletRequest config,
								     @PathParam("yabtId") String yabtId,
									 @FormDataParam("name") String name, 
									 @FormDataParam("description") String description, 
									 @FormDataParam("content") InputStream content,
									 @FormDataParam("content") FormDataBodyPart body)
	{
		String attachmentId = engineBean.uploadAttachment(config.getUserPrincipal() == null ? "" : config.getUserPrincipal().getName(), yabtId, body.getMediaType().toString(), name, description, content);
		return Response.ok(attachmentId).build();
	}
	
	/**
	 * Get list of attachments on given process instance
	 * @param yabtId String YABT ID of process instance
	 * @return JSON
	 */
	@GET
	@Path("/attachments/{yabtId}")
	public Response getAttachments(@PathParam("yabtId") String yabtId)
	{
		ProcessInstance processInst = engineBean.createProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult();
		String processInstanceId = null;
		if( processInst == null )
		{
			processInstanceId = engineBean.createHistoricProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult().getId();
		}
		else
		{
			processInstanceId = processInst.getId();
		}
		
		List<Attachment> attachments = engineBean.getProcessInstanceAttachments(processInstanceId);
		List<Object> returns = new ArrayList<Object>();
		for( final Attachment att : attachments )
		{
			returns.add(new Object(){
				public final Object name = att.getName();
				public final Object description = att.getDescription();
				public final Object id = att.getId();
				public final Object processInstanceId = att.getProcessInstanceId();
				public final Object taskId = att.getTaskId();
				public final Object type = att.getType();
				public final Object url = att.getUrl();
			});
		}
		return Response.ok(returns).build();
	}
	
	/**
	 * Retrieve attachment
	 * @param attachmentId String ID of attachment
	 * @return Streamed file content
	 */
	@GET
	@Path("/attachment/{attachmentId}")
	public Response getAttachment(@PathParam("attachmentId") String attachmentId)
	{
		if( StringUtils.isNullOrEmpty(attachmentId) )
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		Attachment att = engineBean.getAttachment(attachmentId);
		if( att == null )
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		return Response.ok(engineBean.getAttachmentContent(attachmentId))
				.type(att.getType()).build();
	}
	
	/**
	 * Register a user too allow items to be assigned to them. All parameters are optional except username
	 * @param username String Username to apply to this user. Must be unique.
	 * @param email String Email address
	 * @param firstName String Real name of this user, ie "John Doe"
	 * @return HTTP response
	 */
	@POST
	@Path("/user/{username}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response registerUser(@FormDataParam("username") String username,
								 @DefaultValue("") @FormDataParam("email") String email,
								 @DefaultValue("") @FormDataParam("name") String name)
	{
		if( StringUtils.isNullOrEmpty(username) )
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		try
		{
			engineBean.addUser(username, email, name);
			return Response.ok().build();
		}
		catch(RuntimeException e)
		{
			logger.error("Username already exists in system", e);
			return Response.status(Status.BAD_REQUEST).build();
		}
		
	}
	
	/**
	 * Upload a picture to associate with a user.
	 * @param username String username to associate with
	 * @param content InputStream Multi-part file data
	 * @param fileDetail Multi-part metadata, implicity provided
	 * @return
	 */
	@POST
	@Path("/user/avatar/{username}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadUserAvatar(@PathParam("username") String username, 
									 @FormDataParam("content") InputStream content,
			 						 @FormDataParam("content") FormDataContentDisposition fileDetail)
	{
		try
		{
			int numBytes = (int)fileDetail.getSize();
			int bytesRead = 0;
			byte[] bytes = new byte[numBytes];
			do
			{
				bytesRead += content.read(bytes, bytesRead, numBytes - bytesRead);
			}
			while( content.available() > 0 );
			engineBean.setUserAvatar(username, bytes, fileDetail.getType());
			return Response.ok().build();
		}
		catch(IOException e)
		{
			logger.error("Error trying to read in avatar", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Get picture associated with user.
	 * @param username String username to get picture for
	 * @return Raw image data stream
	 */
	@GET
	@Path("/user/avatar/{attachmentId}")
	public Response getUserAvatar(@PathParam("username") String username)
	{
		if( StringUtils.isNullOrEmpty(username) )
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		Picture picture = engineBean.getUserPicture(username);
		if( picture == null )
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		return Response.ok(picture.getInputStream())
				.type(picture.getMimeType()).build();
	}
	
	/**
	 * Retrieve information on user
	 * @param username String username to get information on
	 * @return JSON
	 */
	@GET
	@Path("/user/{username}")
	public Response getUser(@PathParam("username") String username)
	{
		if( StringUtils.isNullOrEmpty(username) )
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		final User user = engineBean.createUserQuery().userId(username).singleResult();
		if( user == null )
		{
			return Response.status(Status.BAD_REQUEST).build();
		}
		return Response.ok(new Object(){
			public final Object username = user.getId();
			public final Object firstName = user.getFirstName();
			public final Object lastName = user.getLastName();
			public final Object email = user.getEmail();
		}).build();
	}
	
	/**
	 * Get list of all registered usernames in workflow system
	 * @return JSON
	 */
	@GET
	@Path("/users")
	public Response getUsers()
	{
		final List<User> users = engineBean.createUserQuery().list();
		final List<Object> returns = new ArrayList<Object>();
		for( final User user : users )
		{
			returns.add(new Object(){
				public final Object username = user.getId();
				public final Object firstName = user.getFirstName();
				public final Object lastName = user.getLastName();
				public final Object email = user.getEmail();
			});
		}
		return Response.ok(returns).build();
	}
}
