package edu.psu.yabt.service;

import java.util.ArrayList;
import java.util.Collection;
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
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.psu.yabt.bean.LinkBean;
import edu.psu.yabt.bean.WorkflowBean;
import edu.psu.yabt.entity.Link;
import edu.psu.yabt.notification.LinkType;

@Path("/flowChart")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class FlowChartService 
{
	private static final Logger logger = LoggerFactory.getLogger(FlowChartService.class);
	
	@EJB
	private WorkflowBean workflowBean;
	
	@EJB
	private LinkBean linkBean;
	
	public FlowChartService()
	{}
	
	public FlowChartService(WorkflowBean workflowBean, LinkBean linkBean)
	{
		this.workflowBean = workflowBean;
		this.linkBean = linkBean;
	}
	
	@GET
	public Response getChartData(@QueryParam("yabtId") final String yabtId)
	{
		ProcessInstance target = workflowBean.createProcessInstanceQuery().processInstanceBusinessKey(yabtId).singleResult();
		HistoricProcessInstance historicTarget = null;
		String type = null;
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
				}
				else
				{
					type = historicTarget.getProcessDefinitionId();
				}
			}
			
			if( type.startsWith("Requirement"))
			{
				sortedNodes.add(new Object(){
					public String nodeId = curEl;
					public String type = "Requirement";
				});
			}
			else if( type.startsWith("Assignment") )
			{
				sortedNodes.add(new Object(){
					public String nodeId = curEl;
					public String type = "Assignment";
				});
			}
			else if( type.startsWith("Test"))
			{
				sortedNodes.add(new Object(){
					public String nodeId = curEl;
					public String type = "Test";
				});
			}
		}
		
		return Response.ok(new Object(){
			public Object[] nodes = sortedNodes.toArray();
			public Object[] edges = edgeList.toArray();
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
				    l.getLinkType() == LinkType.IS_TESTED_BY )
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
					    l.getLinkType() == LinkType.TESTS )
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
				if( l.getLinkType() == LinkType.IS_IMPLEMENTED_BY ||
						(type.startsWith("Test") && l.getLinkType() == LinkType.IS_TESTED_BY ))
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
				else if( l.getLinkType() == LinkType.IMPLEMENTS ||
						(type.startsWith("Test") && l.getLinkType() == LinkType.TESTS ))
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
