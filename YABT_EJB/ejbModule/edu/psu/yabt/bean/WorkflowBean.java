package edu.psu.yabt.bean;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.TaskServiceImpl;
import org.activiti.engine.impl.bpmn.diagram.ProcessDiagramGenerator;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import edu.psu.yabt.activiti.cmd.CreateAttachmentCmd;
import edu.psu.yabt.notification.Condition;

/**
 * Session Bean implementation class WorkflowBean
 */
@Stateless
@LocalBean
public class WorkflowBean {
	private static final Logger logger = LoggerFactory.getLogger(WorkflowBean.class);
	private static ProcessEngine engine;
	
	private static String lastBusinessKey = "YABT-0000";
	
	@EJB
	private LinkBean linkBean;
	
	@EJB
	private ListenerBean eventBean;
	
    /**
     * Default constructor. 
     */
    public WorkflowBean() {
    	logger.info("Building workflow bean...");
        if( engine == null )
        {
        	logger.info("Initializing process engine using activiti.config.xml...");
	    	engine = ProcessEngineConfiguration
	        	    .createProcessEngineConfigurationFromResourceDefault()
	        	    .buildProcessEngine();
	    	logger.info("Process engine initialized!");
	    	// Get Activiti services
	        RepositoryService repositoryService = engine.getRepositoryService();
	        if( repositoryService.createDeploymentQuery().list().isEmpty() )
	        {
		        logger.info("Loading workflow definitions...");
		        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		        try {
					for( Resource r : resolver.getResources("classpath:edu/psu/yabt/workflows/**/*.bpmn") )
					{
						// Deploy the process definition
						repositoryService.createDeployment()
						  .addInputStream(r.getFilename(), r.getInputStream())
						  .deploy();
						logger.info("Loaded workflow definition '"+r.getFilename()+"'");
					}
				} catch (IOException e) {
					logger.error("Error loading workflow definitions", e);
				}
	        }
	        logger.info("Workflows loaded!");
        }
    }
    
    public WorkflowBean(ProcessEngine testModeEngine)
    {
    	engine = testModeEngine;
    }

    public String getEngineName()
    {
    	return getEngine().getName();
    }
    
    private ProcessEngine getEngine()
    {
    	return engine;
    }
    
    public ProcessInstanceQuery createProcessInstanceQuery()
    {
    	return getEngine().getRuntimeService().createProcessInstanceQuery();
    }
    
    public TaskQuery createTaskQuery()
    {
    	return getEngine().getTaskService().createTaskQuery();
    }
    
    public ProcessDefinitionQuery createProcessDefinitionQuery()
    {
    	return getEngine().getRepositoryService().createProcessDefinitionQuery();
    }
    
    public HistoricTaskInstanceQuery createHistoricTaskInstanceQuery()
    {
    	return getEngine().getHistoryService().createHistoricTaskInstanceQuery();
    }
    
    public HistoricProcessInstanceQuery createHistoricProcessInstanceQuery()
    {
    	return getEngine().getHistoryService().createHistoricProcessInstanceQuery();
    }
    
    public List<Attachment> getProcessInstanceAttachments(String processInstanceId)
    {
    	return getEngine().getTaskService().getProcessInstanceAttachments(processInstanceId);
    }
    
    public Attachment getAttachment(String attachmentId)
    {
    	return getEngine().getTaskService().getAttachment(attachmentId);
    }
    
    public InputStream getAttachmentContent(String attachmentId)
    {
    	return getEngine().getTaskService().getAttachmentContent(attachmentId);
    }
    
    public Picture getUserPicture(String username)
    {
    	return getEngine().getIdentityService().getUserPicture(username);
    }
    
    public UserQuery createUserQuery()
    {
    	return getEngine().getIdentityService().createUserQuery();
    }
    
    public ProcessDefinitionEntity getDeployedProcessDefinition(String processDefinitionId)
    {
    	return (ProcessDefinitionEntity) ((RepositoryServiceImpl) getEngine().getRepositoryService())
        .getDeployedProcessDefinition(processDefinitionId);
    }
    
    public void setProcessVariable(String username, String processInstanceId, String varName, Object varValue)
    {
    	getEngine().getRuntimeService().setVariable(processInstanceId, varName, varValue);
    }
    
    public Object getProcessVariable(String processInstanceId, String varName )
    {
    	return getEngine().getRuntimeService().getVariable(processInstanceId, varName);
    }
    
    public Object getHistoricProcessVariable(String processInstanceId, String varName )
    {
    	HistoricVariableInstance varInst = getEngine().getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).variableName(varName).singleResult();
    	return varInst == null ? null : varInst.getValue();
    }
    
    public void updateTask(String username, Task task)
    {
    	getEngine().getTaskService().saveTask(task);
    }
    
    public void reassignTask(String username, String taskId, String newAssignee)
    {
    	getEngine().getTaskService().setAssignee(taskId, newAssignee);
    	eventBean.notifyListeners(username, Condition.REASSIGNED, createProcessInstanceQuery().processInstanceId(createTaskQuery().taskId(taskId).singleResult().getProcessInstanceId()).singleResult().getBusinessKey());
    }
    
    public String startProcess(String username, String type)
    {
		final ProcessInstance process = getEngine().getRuntimeService()
				.startProcessInstanceByKey(type, findNextBusinessKey(lastBusinessKey));
		
		Task tsk = createTaskQuery().processInstanceBusinessKey(process.getBusinessKey()).singleResult();
		tsk.setDueDate(new Date());
		getEngine().getTaskService().saveTask(tsk);
		
		eventBean.notifyListeners(username, Condition.CREATED, process.getBusinessKey());
		return process.getBusinessKey();
    }
    
    private static synchronized String findNextBusinessKey(String latestBusinessKey)
    {
    	List<HistoricProcessInstance> instances = engine.getHistoryService().createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().desc().list();
    	String instBusKey = "YABT-0000";
    	if( !instances.isEmpty() )
    	{
    		instBusKey = instances.get(0).getBusinessKey();
    	}
    	String[] deadParts = instBusKey.split("-");
    	int num = Integer.parseInt(deadParts[1]);
    	
    	
    	lastBusinessKey = String.format("YABT-%04d", num + 1);
    	return lastBusinessKey;
    }
    
    public void advanceProcess( String username, String yabtId, boolean failure )
    {
	    TaskService taskService = getEngine().getTaskService();
		Task taskToComplete = taskService.createTaskQuery().processInstanceBusinessKey(yabtId).active().singleResult();
		
		taskService.setVariable(taskToComplete.getId(), "failure", failure);
		getEngine().getTaskService().complete(taskToComplete.getId());
		Task newTask = taskService.createTaskQuery().processInstanceBusinessKey(yabtId).active().singleResult();
		if( newTask != null )
		{
			newTask.setDueDate(taskToComplete.getDueDate());
			taskService.saveTask(newTask);
			taskService.claim(newTask.getId(), taskToComplete.getAssignee());
			eventBean.notifyListeners(username, Condition.ADVANCED, yabtId);
		}
		else
		{
			eventBean.notifyListeners(username, Condition.COMPLETED, yabtId);
		}
    }
    
    public String uploadAttachment( String username, String yabtId, String mimeType, String name, String description, InputStream content )
    {
    	String processInstId = getEngine().getRuntimeService().createProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult().getId();
    	CreateAttachmentCmd cmd = new CreateAttachmentCmd(mimeType, null, processInstId, name, description, content, null);
    	Attachment att = ((TaskServiceImpl)getEngine().getTaskService()).getCommandExecutor().execute(cmd);

    	eventBean.notifyListeners(username, Condition.UPDATED, yabtId);
    	return att.getId();
    }
    
    public void addUser( String username, String email, String name )
    {
    	User user = getEngine().getIdentityService().newUser(username);
    	user.setEmail(email == null ? "" : email);
    	String[] names = name.split(" ");
    	user.setFirstName(names[0] == null ? "" : names[0]);
    	if( names.length > 1 )
    	{
    		user.setLastName(names[1] == null ? "" : names[1]);
    	}
    	getEngine().getIdentityService().saveUser(user);
    	eventBean.notifyListeners(username, Condition.NEW_USER, null);
    }
    
    public void setUserAvatar( String username, byte[] bytes, String mimeType )
    {
    	Picture picture = new Picture(bytes, mimeType);
    	getEngine().getIdentityService().setUserPicture(username, picture);
    }
    
    public Picture getUserAvatar( String username )
    {
    	return getEngine().getIdentityService().getUserPicture(username);
    }
    
    public InputStream buildProcessDiagram(String type)
    {
    	RepositoryService repositoryService = getEngine().getRepositoryService();
		ProcessDefinition def = repositoryService.createProcessDefinitionQuery().processDefinitionName(type).singleResult();
		if( def != null )
		{
			final ReadOnlyProcessDefinition pde =
					((RepositoryServiceImpl) getEngine().getRepositoryService()).getDeployedProcessDefinition(def.getId());
			
			if( pde != null )
			{
				return ProcessDiagramGenerator.generatePngDiagram((ProcessDefinitionEntity)pde);
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
    }
    
    public InputStream buildProcessInstanceDiagram(String yabtId)
    {
    	RuntimeService runtimeService = getEngine().getRuntimeService();
		ProcessInstance inst = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult();
		String processDefId = null;
		List<String> activeActivityIds = new ArrayList<String>();
		if( inst == null )
		{
			HistoricProcessInstance histInst = getEngine().getHistoryService().createHistoricProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult();
			if( histInst == null )
			{
				return null;
			}
			else
			{
				processDefId = histInst.getProcessDefinitionId();
				activeActivityIds.add(getEngine().getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(histInst.getId()).orderByHistoricActivityInstanceEndTime().desc().list().get(0).getActivityId());
			}
		}
		else
		{
			processDefId = inst.getProcessDefinitionId();
			activeActivityIds = runtimeService.getActiveActivityIds(inst.getId());
		}
		
		if( processDefId != null )
		{
			final ReadOnlyProcessDefinition pde =
					((RepositoryServiceImpl) getEngine().getRepositoryService()).getDeployedProcessDefinition(processDefId);
			
			if( pde != null )
			{
				return ProcessDiagramGenerator.generateDiagram((ProcessDefinitionEntity)pde, "png", activeActivityIds);
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
    }
}
