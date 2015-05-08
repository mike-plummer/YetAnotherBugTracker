function buildGanttChart(container)
{
	var nodes;
	var limits;
	var startDate;
	Ext.Ajax.request({
		async: false,
		url: 'service/ganttChart',
		params : {
			yabtId : params.yabtId
		},
		method : 'GET',
		success: function(response){
			nodes = Ext.JSON.decode(response.responseText).nodes;
			limits = Ext.JSON.decode(response.responseText).limits;
			startDate = new Date(Ext.JSON.decode(response.responseText).projectStartDate);
		}
	});
	
    var project = new GanttProjectInfo(1, "Project", startDate);

    var tasks = [];
    for( var n in nodes )
    {
    	var tsk = new GanttTaskInfo(nodes[n].nodeId, nodes[n].nodeId + ': ' + nodes[n].title, new Date(nodes[n].startDt), nodes[n].length == 0 ? 8 : nodes[n].length, nodes[n].percentComplete, "");
    	tasks.push(tsk);
    }
    
    for( var i = 0; i < tasks.length; i++ )
    {
    	dateAdj = false;
	    for( var t in tasks )
		{
	    	var latestDate = 0;
	    	var predecessor = null;
		    for( var l in limits )
		    {
		    	if( tasks[t].Id == limits[l].tgt )
		    	{
		    		var endTime = 0;
		    		for( var t2 in tasks )
		    		{
		    			if( tasks[t2].Id == limits[l].src )
		    			{
		    				endTime = tasks[t2].EST.getTime();
		    				endTime += tasks[t2].Duration * 60 * 60 * 1000;
		    				if( latestDate < endTime )
		    				{
		    					latestDate = endTime;
		    					predecessor = tasks[t2];
		    				}
		    				break;
		    			}
		    		}
		    		if( predecessor != null )
		    		{
		    			tasks[t].PredecessorTaskId = predecessor.Id;
		    			tasks[t].PredecessorTask = predecessor;
		    			var numDays = Math.ceil( predecessor.Duration / 8.0 );
		    			tasks[t].EST = new Date(predecessor.EST.getTime() + (numDays * 24) * 60 * 60 * 1000);
		    		}
		    		break;
		    	}
			}
	    }
    }
    	
	for( var idx in tasks )
	{
		var t = tasks[idx];
		project.addTask(t);
	}
    
    var gantt = new GanttChart();
    gantt.setImagePath("resources/dhtmlxgantt/imgs/");
    gantt.setEditable(false);
    gantt.addProject(project);
    gantt.create(container);
}