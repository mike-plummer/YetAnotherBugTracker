package edu.psu.yabt.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import edu.psu.yabt.entity.Link;
import edu.psu.yabt.notification.LinkType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( { "/applicationContext-test.xml" })
@Transactional
public class EngineTest {

	static ProcessEngine engine;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@BeforeClass
	public static void createEngine()
	{
		if( ProcessEngines.getDefaultProcessEngine() != null )
		{
			engine = ProcessEngines.getDefaultProcessEngine();
		}
		else
		{
			engine = ProcessEngineConfiguration
	        	    .createProcessEngineConfigurationFromResourceDefault()
	        	    .buildProcessEngine();
		}
	}
	
	@Test
	public void testEngineCreation() 
	{
		assertTrue(engine != null);
	}

	@Test
	public void testEngineName() 
	{
		assertEquals(engine.getName(),"default");
	}
	
	@Test
	public void testLoadProcesses()
	{
		if( engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("Requirement").list().isEmpty() )
		{
			engine.getRepositoryService().createDeployment()
				  .addInputStream("Requirement.bpmn", EngineTest.class.getResourceAsStream("/edu/psu/yabt/workflows/Requirement.bpmn"))
				  .deploy();
		}
		if( engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("Assignment").list().isEmpty() )
		{
			engine.getRepositoryService().createDeployment()
			  .addInputStream("Assignment.bpmn", EngineTest.class.getResourceAsStream("/edu/psu/yabt/workflows/Assignment.bpmn"))
			  .deploy();
		}
		if( engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("Test").list().isEmpty() )
		{
			engine.getRepositoryService().createDeployment()
			  .addInputStream("Test.bpmn", EngineTest.class.getResourceAsStream("/edu/psu/yabt/workflows/Test.bpmn"))
			  .deploy();
		}
		
		assertEquals(engine.getRepositoryService().createDeploymentQuery().list().size(), 3);
	}
	
	@Test
	public void testTaskService() 
	{
		assertTrue(engine.getTaskService() != null);
	}
	
	@Test
	public void testRuntimeService() 
	{
		assertTrue(engine.getRuntimeService() != null);
	}
	
	@Test
	public void testRepositoryService() 
	{
		assertTrue(engine.getRepositoryService() != null);
	}
	
	@Test
	public void testIdentityService() 
	{
		assertTrue(engine.getIdentityService() != null);
	}
	
	@Test
	public void testAssignmentStart()
	{
		engine.getRuntimeService().startProcessInstanceByKey("Assignment");
		assertTrue(engine.getRuntimeService().createProcessInstanceQuery().processDefinitionKey("Assignment").list().size() > 0);
	}
	
	@Test
	public void testRequirementStart()
	{
		engine.getRuntimeService().startProcessInstanceByKey("Requirement");
		assertTrue(engine.getRuntimeService().createProcessInstanceQuery().processDefinitionKey("Requirement").list().size() > 0);
	}
	
	@Test
	public void testTestStart()
	{
		engine.getRuntimeService().startProcessInstanceByKey("Test");
		assertTrue(engine.getRuntimeService().createProcessInstanceQuery().processDefinitionKey("Test").list().size() > 0);
	}
	
	@Test
	public void testReassignment() {
		User user = engine.getIdentityService().newUser("joe");
		engine.getIdentityService().saveUser(user);
		user = engine.getIdentityService().newUser("jane");
		engine.getIdentityService().saveUser(user);
		
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Test");
		Task task = engine.getTaskService().createTaskQuery().processInstanceId(inst.getProcessInstanceId()).singleResult();
		assertTrue(task.getAssignee() == null);
		engine.getTaskService().claim(task.getId(), "joe");
		task = engine.getTaskService().createTaskQuery().processInstanceId(inst.getProcessInstanceId()).singleResult();
		assertEquals(task.getAssignee(), "joe");
		engine.getTaskService().setAssignee(task.getId(), "jane");
		task = engine.getTaskService().createTaskQuery().processInstanceId(inst.getProcessInstanceId()).singleResult();
		assertEquals(task.getAssignee(), "jane");
	}
	
	@Test
	public void testDescription() {
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Test");
		engine.getRuntimeService().setVariable(inst.getProcessInstanceId(), WorkflowVariables.DESCRIPTION, "Description");
		
		assertEquals(engine.getRuntimeService().getVariable(inst.getProcessInstanceId(), WorkflowVariables.DESCRIPTION), "Description");
	}
	
	@Test
	public void testTitle() {
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Test");
		engine.getRuntimeService().setVariable(inst.getProcessInstanceId(), WorkflowVariables.TITLE, "Title");
		
		assertEquals(engine.getRuntimeService().getVariable(inst.getProcessInstanceId(), WorkflowVariables.TITLE), "Title");
	}
	
	@Test
	public void testEtc() {
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Test");
		engine.getRuntimeService().setVariable(inst.getProcessInstanceId(), WorkflowVariables.ETC, 50);
		
		assertEquals(engine.getRuntimeService().getVariable(inst.getProcessInstanceId(), WorkflowVariables.ETC), 50);
	}
	
	@Test
	public void testRequirementLink() {
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Requirement");
		ProcessInstance inst2 = engine.getRuntimeService().startProcessInstanceByKey("Assignment");
		Link link = new Link();
		link.setSource(inst.getProcessInstanceId());
		link.setTarget(inst2.getProcessInstanceId());
		link.setLinkType(LinkType.IS_RELATED_TO);
		
		entityManager.persist(link);
		
		assertEquals(entityManager.createQuery("select l from Link l", Link.class).getResultList().size(), 1);
	}
	
	@Test
	public void testAssignmentLink() {
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Assignment");
		ProcessInstance inst2 = engine.getRuntimeService().startProcessInstanceByKey("Test");
		Link link = new Link();
		link.setSource(inst.getProcessInstanceId());
		link.setTarget(inst2.getProcessInstanceId());
		link.setLinkType(LinkType.IS_RELATED_TO);
		
		entityManager.persist(link);
		
		assertEquals(entityManager.createQuery("select l from Link l", Link.class).getResultList().size(), 1);
	}
	
	@Test
	public void testTestLink() {
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Test");
		ProcessInstance inst2 = engine.getRuntimeService().startProcessInstanceByKey("Assignment");
		Link link = new Link();
		link.setSource(inst.getProcessInstanceId());
		link.setTarget(inst2.getProcessInstanceId());
		link.setLinkType(LinkType.IS_RELATED_TO);
		
		entityManager.persist(link);
		
		assertEquals(entityManager.createQuery("select l from Link l", Link.class).getResultList().size(), 1);
	}
}
