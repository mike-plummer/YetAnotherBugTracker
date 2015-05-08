function buildFlowChart(container)
{
	var style = new Object();
	style[mxConstants.STYLE_SHAPE] = mxConstants.SHAPE_RECTANGLE;
	style[mxConstants.STYLE_OPACITY] = 50;
	style[mxConstants.STYLE_FONTCOLOR] = '#774400';
	style[mxConstants.STYLE_ROUNDED] = true;
	
	// Creates the graph inside the given container
	var graph = new mxGraph(container);
	
	graph.getStylesheet().putCellStyle('ROUNDED',style);
	// Gets the default parent for inserting new cells. This
	// is normally the first child of the root (ie. layer 0).
	var parent = graph.getDefaultParent();

	// Adds cells to the model in a single step
	graph.getModel().beginUpdate();
	try
	{
		var nodes;
		var edges;
		Ext.Ajax.request({
			async: false,
			url: 'service/flowChart',
			params : {
				yabtId : params.yabtId
			},
			method : 'GET',
			success: function(response){
				nodes = Ext.JSON.decode(response.responseText).nodes;
				edges = Ext.JSON.decode(response.responseText).edges;
			}
		});
		
		var reqVertPosition = -40;
		var asgnVertPosition = -40;
		var tstVertPosition = -40;
		
		var vertices = [];
		
		for( var n in nodes )
		{
			var horzPosition = 0;
			var vertPosition = 0;
			if( nodes[n].type == 'Requirement')
			{
				horzPosition = 20;
				reqVertPosition += 60;
				vertPosition = reqVertPosition;
			}
			else if( nodes[n].type == 'Assignment' )
			{
				horzPosition = 270;
				asgnVertPosition += 60;
				vertPosition = asgnVertPosition;
			}
			else if( nodes[n].type == 'Test' )
			{
				horzPosition = 520;
				tstVertPosition += 60;
				vertPosition = tstVertPosition;
			}
			var vertex;
			if( nodes[n].nodeId == params.yabtId )
			{
				vertex = graph.insertVertex(parent, nodes[n].nodeId, nodes[n].nodeId, horzPosition, vertPosition, 80, 30, 'ROUNDED;strokeColor=red;fillColor=green');
			}
			else
			{
				vertex = graph.insertVertex(parent, nodes[n].nodeId, nodes[n].nodeId, horzPosition, vertPosition, 80, 30, 'ROUNDED');
			}
			vertices.push( vertex );
		}
		
		for( var e in edges )
		{
			var src, tgt;
			for( var v in vertices )
			{
				if( vertices[v].id == edges[e].src )
				{
					src = vertices[v];
				}
				else if( vertices[v].id == edges[e].tgt )
				{
					tgt = vertices[v];
				}
			}
			
			graph.insertEdge( parent, null, edges[e].txt, src, tgt);
		}
	}
	finally
	{
		// Updates the display
		graph.getModel().endUpdate();
		graph.setEnabled(false);
	}
}

function buildHistoricChart() 
{
	var assignees = null;
	var data = null;
	Ext.Ajax.request({
		async: false,
		url: 'service/chart/historic',
		method: 'GET',
		success: function(response){
			assignees = Ext.decode(response.responseText).assignees;
			data = Ext.decode(response.responseText).data;
		}
	});
	
	var mdlFields = [];
	mdlFields.push({name: 'date'});
	for( var a in assignees )
	{
		var nm = assignees[a];
		nm = nm.replace(/[^a-z0-9]/gi, '_');
		mdlFields.push({name: nm});
	}

	Ext.define('HistoricChartModel', {
        extend: 'Ext.data.Model',
        fields: mdlFields
	});
	
	var chartStore = Ext.create('Ext.data.SimpleStore', {
		model: 'HistoricChartModel'
	});
	
	for( var day in data )
	{
		var row = [];
		row.push(data[day].date);
		for( var a in assignees )
		{
			var flag = false;
			for( var d in data[day].data )
			{
				if( data[day].data[d].assignee == assignees[a] )
				{
					row.push(data[day].data[d].total);
					flag = true;
					break;
				}
			}
			if( !flag )
			{
				row.push(0);
			}
		}
		
		chartStore.loadData([row], true);
	}
	
	for( var a in assignees )
	{
		assignees[a] = assignees[a].replace(/[^a-z0-9]/gi, '_');
	}
	
	var chart = Ext.create('Ext.chart.Chart', {
        style: 'background:#fff',
        animate: true,
        id: 'historicChartCmp',
        store: chartStore,
        legend: {
            position: 'bottom'
        },
        axes: [{
            type: 'Numeric',
            position: 'left',
            fields: assignees,
            title: '# of Items',
            grid: {
                odd: {
                    opacity: 1,
                    fill: '#ddd',
                    stroke: '#bbb',
                    'stroke-width': 1
                }
            },
            minimum: 0,
            adjustMinimumByMajorUnit: 0
        }, {
            type: 'Category',
            position: 'bottom',
            fields: ['date'],
            title: 'Date',
            grid: true,
            label: {
                rotate: {
                    degrees: 315
                }
            }
        }],
        series: [{
            type: 'area',
            highlight: false,
            axis: 'left',
            xField: 'date',
            yField: assignees,
            style: {
                opacity: 0.93
            }
        }]
    });
	
	Ext.create('Ext.panel.Panel', {
		width: 800,
        height: 350,
        colspan: 2,
        title: 'Historic Workload',
        collapsible:true,
        layout: 'fit',
        id: 'historicChartPanel',
        items: [chart]
	});
}