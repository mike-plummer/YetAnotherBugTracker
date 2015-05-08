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

import com.sun.jersey.api.client.ClientResponse.Status;

import edu.psu.yabt.bean.WorkflowBean;
import edu.psu.yabt.service.ChartService;
import edu.psu.yabt.workflow.EngineTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( { "/applicationContext-test.xml" })
@Transactional
public class ChartServiceTest {

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
	public void testWorkloadChart() 
	{
		WorkflowBean workflowBean = new WorkflowBean(engine);
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Assignment");
		
		ChartService svc = new ChartService(workflowBean);
		assertTrue(svc.getWorkloadChartData().size() > 0);
	}

	@Test
	public void testHistoricChart() 
	{
		WorkflowBean workflowBean = new WorkflowBean(engine);
		ProcessInstance inst = engine.getRuntimeService().startProcessInstanceByKey("Assignment");
		
		ChartService svc = new ChartService(workflowBean);
		Response resp = svc.getHistoricChartData();
		assertTrue(resp.getStatus() == Status.OK.getStatusCode());
		Object entity = resp.getEntity();
		assertTrue(entity != null);
		assertTrue(entity.getClass().getFields().length == 2);
	}
	
	@Test
	public void testEmptyWorkloadChartService()
	{
		try
		{
			ChartService svc = new ChartService();
			svc.getWorkloadChartData();
			fail("ChartService didn't fail!");
		}
		catch(Exception e)
		{
			//Expected
		}
	}
	
	@Test
	public void testEmptyHistoricChartService()
	{
		try
		{
			ChartService svc = new ChartService();
			svc.getHistoricChartData();
			fail("ChartService didn't fail!");
		}
		catch(Exception e)
		{
			//Expected
		}
	}
}
