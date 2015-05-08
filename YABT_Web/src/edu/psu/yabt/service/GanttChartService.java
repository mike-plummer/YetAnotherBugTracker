package edu.psu.yabt.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.psu.yabt.bean.LinkBean;
import edu.psu.yabt.bean.WorkflowBean;
import edu.psu.yabt.entity.Link;
import edu.psu.yabt.notification.LinkType;
import edu.psu.yabt.workflow.WorkflowVariables;

@Path("/ganttChart")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class GanttChartService 
{
	private static final Logger logger = LoggerFactory.getLogger(GanttChartService.class);
	
	@EJB
	private WorkflowBean workflowBean;
	
	@EJB
	private LinkBean linkBean;
	
	public GanttChartService()
	{}
	
	public GanttChartService(WorkflowBean workflowBean, LinkBean linkBean)
	{
		this.workflowBean = workflowBean;
		this.linkBean = linkBean;
	}
	
	@GET
	public Response getChartData(@QueryParam("yabtId") final String yabtId)
	{
		ProcessInstance target = workflowBean.createProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult();
		HistoricProcessInstance historicTarget = null;
		Date earliestStartDate = new Date();
		String type = null;
		String title = null;
		int completionPercent = 0;
		int duration = 0;
		Date startDate = null;
		if( target == null )
		{
			historicTarget = workflowBean.createHistoricProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult();
		}
		if( target == null && historicTarget == null )
		{
			logger.error("Invalid yabt ID, id="+yabtId);
			throw new WebApplicationException();
		}
		else
		{
			if( target != null )
			{
				type = target.getProcessDefinitionId();
			}
			else
			{
				type = historicTarget.getProcessDefinitionId();
			}
		}
		
		Collection<String> nodeSet = new HashSet<String>();
		final Collection<Object> edgeList = new ArrayList<Object>();
		
		searchBackwards(nodeSet, edgeList, yabtId, type);
		searchForwards(nodeSet, edgeList, yabtId, type);

		final Collection<Object> sortedNodes = new ArrayList<Object>();
		for( Iterator<String> iter = nodeSet.iterator(); iter.hasNext(); )
		{
			final String curEl = iter.next();
			target = null;
			historicTarget = null;
			type = null;
			target = workflowBean.createProcessInstanceQuery().processInstanceBusinessKey(curEl).singleResult();
			if( target == null )
			{
				historicTarget = workflowBean.createHistoricProcessInstanceQuery().processInstanceBusinessKey(curEl).singleResult();
			}
			if( target == null && historicTarget == null )
			{
				logger.error("Invalid yabt ID, id="+curEl);
				throw new WebApplicationException();
			}
			else
			{
				if( target != null )
				{
					type = target.getProcessDefinitionId();
					final HistoricTaskInstance firstTsk = workflowBean.createHistoricTaskInstanceQuery().processInstanceId(target.getId()).orderByHistoricActivityInstanceStartTime().asc().list().get(0);
					startDate = firstTsk.getStartTime();
					if( startDate.before(earliestStartDate) )
					{
						earliestStartDate = startDate;
					}
					title = (String) workflowBean.getProcessVariable(target.getId(), WorkflowVariables.TITLE);
					Integer etc = (Integer) workflowBean.getProcessVariable(target.getId(), WorkflowVariables.ETC);
					Integer wl = (Integer) workflowBean.getProcessVariable(target.getId(), WorkflowVariables.WORK_LOGGED);
					
					duration = (etc == null ? 0 : etc) + (wl == null ? 0 : wl);
					if( duration != 0 )
					{
						completionPercent = (int) (((wl == null ? 0 : wl) / (duration * 1.0d)) * 100);
					}
				}
				else
				{
					type = historicTarget.getProcessDefinitionId();
					startDate = historicTarget.getStartTime();
					if( startDate.before(earliestStartDate) )
					{
						earliestStartDate = startDate;
					}
					title = (String) workflowBean.getHistoricProcessVariable(historicTarget.getId(), WorkflowVariables.TITLE);
					Integer wl = (Integer) workflowBean.getHistoricProcessVariable(historicTarget.getId(), WorkflowVariables.WORK_LOGGED);
					duration = wl == null ? 0 : wl;
					completionPercent = 100;
				}
			}
			final Integer durVal = duration;
			final Integer complVal = completionPercent;
			final Date startVal = startDate;
			final String itemTitle = title;
			if( type.startsWith("Requirement"))
			{
				sortedNodes.add(new Object(){
					public String nodeId = curEl;
					public String type = "Requirement";
					public Integer length = durVal;
					public String title = itemTitle;
					public Integer percentComplete = complVal;
					public Date startDt = startVal;
				});
			}
			else if( type.startsWith("Assignment") )
			{
				sortedNodes.add(new Object(){
					public String nodeId = curEl;
					public String type = "Assignment";
					public Integer length = durVal;
					public String title = itemTitle;
					public Integer percentComplete = complVal;
					public Date startDt = startVal;
				});
			}
			else if( type.startsWith("Test"))
			{
				sortedNodes.add(new Object(){
					public String nodeId = curEl;
					public String type = "Test";
					public Integer length = durVal;
					public String title = itemTitle;
					public Integer percentComplete = complVal;
					public Date startDt = startVal;
				});
			}
		}
		final Date projStartDate = earliestStartDate;
		return Response.ok(new Object(){
			public Object[] nodes = sortedNodes.toArray();
			public Object[] limits = edgeList.toArray();
			public Object projectStartDate = projStartDate.getTime();
		}).build();
	}
	
	private void searchForwards(Collection<String> nodes, Collection<Object> edges, String yabtId, String type)
	{
		if( type.startsWith("Test") )
		{
			return;
		}
		Queue<String> nodesToIndexBfs = new ArrayBlockingQueue<String>(1000);
		nodesToIndexBfs.add(yabtId);
		while( !nodesToIndexBfs.isEmpty() )
		{
			String currentNode = nodesToIndexBfs.poll();
			nodes.add(currentNode);
			Collection<Link> currentLinks = linkBean.getLinks(currentNode);
			for( final Link l : currentLinks )
			{
				if( (type.startsWith("Requirement") && l.getLinkType() == LinkType.IS_IMPLEMENTED_BY) ||
				    l.getLinkType() == LinkType.IS_TESTED_BY || l.getLinkType() == LinkType.IS_BLOCKED_BY || l.getLinkType() == LinkType.DEPENDS_ON )
				{
					if( nodes.contains(l.getTarget()) )
					{
						continue;
					}
					else
					{
						nodesToIndexBfs.add(l.getTarget());
					}
					edges.add(new Object(){
						public String src = l.getSource();
						public String tgt = l.getTarget();
						public String txt = l.getLinkType().getText();
					});
				}
				else if( (type.startsWith("Requirement") && l.getLinkType() == LinkType.IMPLEMENTS) ||
					    l.getLinkType() == LinkType.TESTS || l.getLinkType() == LinkType.BLOCKS || l.getLinkType() == LinkType.IS_DEPENDENCY_OF)
				{
					if( nodes.contains(l.getSource()) )
					{
						continue;
					}
					else
					{
						nodesToIndexBfs.add(l.getSource());
					}
					edges.add(new Object(){
						public String src = l.getTarget();
						public String tgt = l.getSource();
						public String txt = l.getLinkType().getInverseLinkType().getText();
					});
				}
			}
		}
	}
	
	private void searchBackwards(Collection<String> nodes, Collection<Object> edges, String yabtId, String type)
	{
		if( type.startsWith("Requirement") )
		{
			return;
		}
		Queue<String> nodesToIndexBfs = new ArrayBlockingQueue<String>(1000);
		nodesToIndexBfs.add(yabtId);
		while( !nodesToIndexBfs.isEmpty() )
		{
			String currentNode = nodesToIndexBfs.poll();
			nodes.add(currentNode);
			Collection<Link> currentLinks = linkBean.getLinks(currentNode);
			for( final Link l : currentLinks )
			{
				if( l.getLinkType() == LinkType.IS_IMPLEMENTED_BY || l.getLinkType() == LinkType.IS_BLOCKED_BY || 
						(type.startsWith("Test") && l.getLinkType() == LinkType.IS_TESTED_BY ) || l.getLinkType() == LinkType.DEPENDS_ON)
				{
					if( nodes.contains(l.getSource()) )
					{
						continue;
					}
					else
					{
						nodesToIndexBfs.add(l.getSource());
					}
					edges.add(new Object(){
						public String src = l.getSource();
						public String tgt = l.getTarget();
						public String txt = l.getLinkType().getText();
					});
				}
				else if( l.getLinkType() == LinkType.IMPLEMENTS || l.getLinkType() == LinkType.BLOCKS ||
						(type.startsWith("Test") && l.getLinkType() == LinkType.TESTS ) || l.getLinkType() == LinkType.IS_DEPENDENCY_OF)
				{
					if( nodes.contains(l.getTarget()) )
					{
						continue;
					}
					else
					{
						nodesToIndexBfs.add(l.getTarget());
					}
					edges.add(new Object(){
						public String src = l.getTarget();
						public String tgt = l.getSource();
						public String txt = l.getLinkType().getInverseLinkType().getText();
					});
				}
			}
		}
	}
}
