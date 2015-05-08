package edu.psu.yabt.service;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.psu.yabt.bean.WorkflowBean;

@Path("/chart")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class ChartService 
{
	private static final Logger logger = LoggerFactory.getLogger(ChartService.class);
	
	@EJB
	private WorkflowBean workflowBean;
	
	public ChartService()
	{}
	
	public ChartService(WorkflowBean bean)
	{
		this.workflowBean = bean;
	}
	
	@GET
	@Path("/workload")
	public Collection<Object> getWorkloadChartData()
	{
		Collection<Task> tasks = workflowBean.createTaskQuery().list();
		Map<String, Integer> data = new HashMap<String, Integer>();
		for( final Task t : tasks )
		{
			Integer val = data.get(t.getAssignee());
			if( val == null )
			{
				val = Integer.valueOf(0);
			}
			val++;
			data.put(t.getAssignee(), val);
		}
		Collection<Object> rets = new ArrayList<Object>();
		for(final Entry<String, Integer> entry : data.entrySet() )
		{
			rets.add(new Object(){
				public final String assignee = entry.getKey() == null ? "Unassigned" : entry.getKey();
				public final Integer num = entry.getValue();
			});
		}
		return rets;
	}
	
	@GET
	@Path("/historic")
	public Response getHistoricChartData()
	{
		List<HistoricProcessInstance> activeProcesses = workflowBean.createHistoricProcessInstanceQuery().orderByProcessInstanceBusinessKey().asc().unfinished().list();
		List<HistoricProcessInstance> historicTasksByStartTime = workflowBean.createHistoricProcessInstanceQuery().orderByProcessInstanceStartTime().asc().list();
		final Set<String> asgnees = new HashSet<String>();
		
		Calendar earliestStart = Calendar.getInstance();
		final Map<Long, Map<String, Integer>> cumStarted = new HashMap<Long, Map<String, Integer>>();
		
		int historicBase = 0;
		
		if( historicTasksByStartTime != null && !historicTasksByStartTime.isEmpty() )
		{
			
			earliestStart.setTime(historicTasksByStartTime.get(0).getStartTime());
			earliestStart.set(Calendar.HOUR_OF_DAY, 0);
			earliestStart.set(Calendar.MINUTE, 0);
			earliestStart.set(Calendar.SECOND, 0);
			earliestStart.set(Calendar.MILLISECOND, 0);
			
			logger.info("Set historic early start to "+DateFormat.getDateInstance().format(earliestStart.getTime()));
			
			Calendar walkingDate = (Calendar)earliestStart.clone();
			Calendar stopDate = Calendar.getInstance();
			
			while(walkingDate.before(stopDate))
			{
				logger.info("Walking "+DateFormat.getDateInstance().format(walkingDate.getTime()));
				for( HistoricProcessInstance inst : workflowBean.createHistoricProcessInstanceQuery().startedBefore(walkingDate.getTime()).finished().list() )
				{
					logger.info("Analyzing instance...");
					if( cumStarted.get(walkingDate.getTimeInMillis()) == null )
					{
						logger.info("First instance for this day, building new total map");
						cumStarted.put(walkingDate.getTimeInMillis(), new HashMap<String, Integer>());
					}
					Map<String, Integer> dayTotal = cumStarted.get(walkingDate.getTimeInMillis());
					String userId = inst.getStartUserId() == null || inst.getStartUserId().equals("") ? " " : inst.getStartUserId();
					if( dayTotal.get(userId) == null )
					{
						logger.info("Adding new assignee to this day, assignee="+userId);
						dayTotal.put(userId, 0);
						asgnees.add(userId);
					}
					logger.info("Incrementing user="+userId);
					dayTotal.put(userId, dayTotal.get(userId) + 1);
				}
				walkingDate.set(Calendar.DAY_OF_YEAR, walkingDate.get(Calendar.DAY_OF_YEAR) + 1);
			}
		}
		if( activeProcesses != null && !activeProcesses.isEmpty() )
		{
			logger.info("Looking at active processes...");
			Calendar earliestActiveStart = Calendar.getInstance();
			earliestActiveStart.setTime(workflowBean.createHistoricTaskInstanceQuery().processInstanceId(activeProcesses.get(0).getId()).orderByHistoricActivityInstanceStartTime().asc().list().get(0).getStartTime());
			if( earliestStart == null || earliestStart.after(earliestActiveStart) )
			{
				earliestStart = earliestActiveStart;
			}
			for( HistoricProcessInstance inst : activeProcesses )
			{
				logger.info("Analyzing instance...");
				HistoricTaskInstance firstTsk =  workflowBean.createHistoricTaskInstanceQuery().processInstanceId(inst.getId()).orderByHistoricActivityInstanceStartTime().asc().list().get(0);
				Task tsk =  workflowBean.createTaskQuery().processInstanceId(inst.getId()).singleResult();
				Calendar tskStart = Calendar.getInstance();
				tskStart.setTime(firstTsk.getStartTime());
				tskStart.set(Calendar.HOUR_OF_DAY, 0);
				tskStart.set(Calendar.MINUTE, 0);
				tskStart.set(Calendar.SECOND, 0);
				tskStart.set(Calendar.MILLISECOND, 0);
				if( cumStarted.get(tskStart.getTimeInMillis()) == null )
				{
					cumStarted.put(tskStart.getTimeInMillis(), new HashMap<String, Integer>());
				}
				Map<String, Integer> ttl = cumStarted.get(tskStart.getTimeInMillis());
				String userId = tsk.getAssignee() == null || tsk.getAssignee().equals("") ? " " : tsk.getAssignee();
				if( ttl.get(userId) == null )
				{
					ttl.put(userId, 0);
					asgnees.add(userId);
				}
				ttl.put(userId, ttl.get(userId) + 1);
			}
		}
		
		Calendar oneDayEarly = (Calendar)earliestStart.clone();
		oneDayEarly.set(Calendar.DAY_OF_YEAR, oneDayEarly.get(Calendar.DAY_OF_YEAR) - 1);
		cumStarted.put(oneDayEarly.getTimeInMillis(), new HashMap<String, Integer>());
		
		final Object[] entries = new Object[cumStarted.size()];
		List<Long> sortedDates = new ArrayList<Long>();
		sortedDates.addAll(cumStarted.keySet());
		Collections.sort(sortedDates);
		int i = 0;
		for( final Long dt : sortedDates )
		{
			logger.info("Inserting data for date="+DateFormat.getDateInstance().format(new Date(dt)));
			final Map<String, Integer> curTotals = cumStarted.get(dt);
			if( curTotals == null )
			{
				logger.warn("Somehow got a null result from Date="+DateFormat.getDateInstance().format(new Date(dt)));
			}
			else
			{
				final Object[] totals = new Object[asgnees.size()];
				int j = 0;
				for( final String asgnee : asgnees )
				{
					int assigneeTotal = curTotals.get(asgnee) == null ? 0 : curTotals.get(asgnee);
					if( i > 0 )
					{
						Long previousTime = sortedDates.get(i - 1);
						assigneeTotal += cumStarted.get(previousTime).get(asgnee) == null ? 0 : cumStarted.get(previousTime).get(asgnee);
						curTotals.put(asgnee, assigneeTotal);
					}
					final int at = assigneeTotal;
					totals[j] = new Object(){
						public String assignee = asgnee;
						public int total = at;
					};
					j++;
				}
				entries[i] = new Object(){
					public String date  = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(dt));
					public Object data = totals;
				};
				i++;
			}
		}
		
		Object ret =  new Object(){
			public final Object[] assignees = asgnees.toArray();
			public final Object[] data = entries;
		};
		
		return Response.ok(ret).build();
	}
}
