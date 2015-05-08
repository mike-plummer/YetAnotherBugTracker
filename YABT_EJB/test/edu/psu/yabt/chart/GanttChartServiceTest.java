package edu.psu.yabt.chart;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.Response;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import edu.psu.yabt.bean.LinkBean;
import edu.psu.yabt.bean.WorkflowBean;
import edu.psu.yabt.service.GanttChartService;
import edu.psu.yabt.workflow.EngineTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( { "/applicationContext-test.xml" })
@Transactional
public class GanttChartServiceTest {

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
		if( engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("Assignment").list().isEmpty() )
		{
			engine.getRepositoryService().createDeployment()
			  .addInputStream("Assignment.bpmn", EngineTest.class.getResourceAsStream("/edu/psu/yabt/workflows/Assignment.bpmn"))
			  .deploy();
		}
	}
	
	@Test
	public void testGanttChart() 
	{
		LinkBean linkBean = new LinkBean(entityManager);
		WorkflowBean workflowBean = new WorkflowBean(engine);
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Assignment", "Test2");
		
		GanttChartService svc = new GanttChartService(workflowBean, linkBean);
		Response resp = svc.getChartData(inst.getBusinessKey());
		Object entity = resp.getEntity();
		assertTrue(entity != null);
		assertTrue(entity.getClass().getFields().length == 3);
	}
	
	@Test
	public void testEmptyGanttService()
	{
		try
		{
			GanttChartService svc = new GanttChartService();
			svc.getChartData("");
			fail("GanttChartService didn't fail!");
		}
		catch(Exception e)
		{
			//Expected
		}
	}
}
