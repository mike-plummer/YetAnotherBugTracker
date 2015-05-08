
var params = {
		yabtId : undefined,
		username : undefined,
		admin : undefined,
		nextTransitions : undefined,
		taskData : undefined
};

function init(inputYabtId){
	Ext.Ajax.request({
		async: false,
		url: 'service/security/username',
		success: function(response){
			params.username = Ext.JSON.decode(response.responseText).username;
			params.admin = Ext.JSON.decode(response.responseText).admin;
		}
	});

	var windowUrl = window.location.href;
	var yabtIdIndex = windowUrl.indexOf('yabtId=', 0);
	if( yabtIdIndex >= 0 )
	{
		params.yabtId = windowUrl.substring(yabtIdIndex + 7, yabtIdIndex + 16);
	}
	else if(inputYabtId != undefined && inputYabtId != '')
	{
		params.yabtId = inputYabtId;
	}
	else
	{
		params.yabtId = undefined;
	}
	if( params.yabtId != undefined )
	{
		Ext.Ajax.request({
	    	async: false,
			url: 'service/workflow/task',
			method: 'GET',
			params: {
				yabtId: params.yabtId,
				active: true,
				inactive: true
			},
			success: function(response){
				var decoded = Ext.JSON.decode(response.responseText)[0];
				if( decoded != undefined)
				{
					params.nextTransitions = decoded.transitions;
					params.taskData = decoded;
				}
			},
			failure: function(response){
				Ext.Msg.alert("Error", "Specified YABT issue does not exist.");
			}
		});
	}
};

function onStateButtonClick(item, e, con){
	var failCondition = false;
	if( item == '${failure}' )
	{
		failCondition = true;
	}
	Ext.Ajax.request({
		async: false,
		url: 'service/workflow/process/'+params.yabtId,
		params: {
			fail : failCondition
		},
		method: 'PUT',
		success: function(response){
			window.location.reload();
		}
	});
}

function buildDetailToolbar(){

	Ext.QuickTips.init();

	var states = params.nextTransitions;
	var i = 0;
	var items = [];
	if( states != undefined )
	{
		for( i =0; i<states.length; i++ )
		{
			items.push({
				text:states[i].name,
				handler: Ext.Function.pass(onStateButtonClick, states[i].condition, this)
			});
		}
	}
	var menu = Ext.create('Ext.menu.Menu', {
		id: 'mainMenu',
		style: {
			overflow: 'visible'
		}
	});
	if( items.length == 0 )
	{
		Ext.getCmp('mainMenu').disable(true);
	}
	else
	{
		menu.add(items);
	}
	var tb = Ext.create('Ext.toolbar.Toolbar', {
		id : 'toolbar'
	});
	tb.suspendLayouts();

	tb.add(
			{
				text: 'Edit',
				id: 'editButton',
				handler: onEditButtonClick,
				tooltip: 'Edit this item'
			},
			{
				text: 'Link',
				id: 'linkButton',
				handler: displayLinkCreationDialog,
				tooltip: 'Link this item to another'
			},{
				text: 'Attach',
				id: 'attachButton',
				handler: displayUploadAttachmentDialog,
				tooltip: 'Upload a document to attach'
			},{
				text: 'Reassign',
				id: 'reassignButton',
				handler: onReassignButtonClick,
				tooltip: 'Assign this item to another user'
			},
			{
				text:'Advance',
				id:'advanceMenu',
				iconCls: 'bmenu',
				menu: menu,
				tooltip: 'Advance this item to a new state'
			},
			'-',
			Ext.create('Ext.button.Split', {
				text: 'Status Diagram',
				id: 'currentDiagramMenuItem',
				handler: onWorkflowStatusSelect,
				tooltip: {text:'View this item\'s workflow status diagram', title:'Status Diagram'},
				iconCls: 'blist',
				menu : {
					items: [{
						text: 'View Overall Workflow', handler: onWorkflowSelect
					}]
				}
			}),
			'-',
			{
				text: 'Notifications',
				handler: onNotificationsButtonClick,
				tooltip: 'This button allows you to edit/examine item notifications'
			},
			'->',
			{
		    	text: 'Search',
		    	id: 'searchButton',
				handler: onSearchButtonClick,
				tooltip: 'Search for items'
		    }
	);
	tb.resumeLayouts(true);
	if( params.taskData == undefined )	//Invalid YabtId provided, should disable majority of the page
	{
		Ext.getCmp('toolbar').disable(true);
	}
	else if( states.length == 0 )
	{
		Ext.getCmp('editButton').disable();
		Ext.getCmp('reassignButton').disable();
	}
};

function buildDashboardToolbar(){

    Ext.QuickTips.init();

    Ext.Ajax.request({
    	async: false,
		url: 'service/workflow/processDef',
		method: 'GET',
		success: function(response){
			var types = Ext.JSON.decode(response.responseText);
			var i = 0;
		    var processes = [];
		    for( i =0; i<types.length; i++ )
		    {
		    	processes.push({
		    		text:types[i].key,
		    		handler: createProcess
		    	});
		    }
		    
		    var menu = Ext.create('Ext.menu.Menu', {
		        id: 'mainMenu',
		        style: {
		            overflow: 'visible'
		        }
		    });
		    menu.add(processes);

		    var tb = Ext.create('Ext.toolbar.Toolbar', {
		    	id : 'toolbar'
		    });
		    tb.suspendLayouts();

		    tb.add({
		    	text: 'Create',
		    	iconCls: 'bmenu',
		    	menu: menu
		    });
		    tb.add('->');
		    tb.add({
		    	text: 'Search',
		    	id: 'searchButton',
				handler: onSearchButtonClick,
				tooltip: 'Search for items'
		    });
		    tb.resumeLayouts(true);
		}
	});
};

function buildMyAssignmentsPanel(){
	Ext.define('MyAssignmentModel', {
        extend: 'Ext.data.Model',
        fields: ['assignee', {name:'createTime', type:'date', dateFormat:'timestamp'}, 'delegationState', 'description', 'taskDueDateTxt2', 'executionId', 'id', 'name', 'owner', 'parentId', {name:'priority', type:'integer'}, 'processDefinitionId', 'processInstanceId', 'yabtId', 'type', 'definitionKey']
	});
	
    var taskStore = Ext.create('Ext.data.JsonStore', {
        model: 'MyAssignmentModel',
        proxy: {
            type: 'ajax',
            url: 'service/workflow/task',
            method: 'GET',
            extraParams: {
            	assignee: params.username,
            	active: true,
            	inactive: false
            },
            reader: {
                type: 'json'
            }
        }
    });
    taskStore.load();

    var taskPanel = Ext.create('Ext.grid.Panel', {
        width:400,
        id: 'myAssignmentPanel',
        height:300,
        collapsible:true,
        title:'Items Assigned to Me',
        store: taskStore,
		multiSelect : false,
		viewConfig : {
			emptyText : 'No items assigned to you',
		},
		listeners : {
			celldblclick : function(dataview, td, cellIndex, record, tr, rowIndex, e, eOpts) {
				var store = Ext.getCmp('myAssignmentPanel').getStore();
				var dataItem = store.getAt(rowIndex);
				var yabtId = dataItem.get('yabtId');
				window.location.href = "/YABT_Web/detail.html?yabtId="+yabtId;
			}
		},
		columns : [ {
			text : "ID",
			flex : 50,
			dataIndex : 'yabtId'
		}, {
			text : 'Type',
			flex : 50,
			dataIndex : 'type'
		}, {
			text : 'State',
			flex : 50,
			dataIndex : 'name'
		}, {
			text : 'Due Date',
			//xtype : 'datecolumn',
			//format : 'm-d h:i a',
			flex : 50,
			dataIndex : 'taskDueDateTxt2'
		} ]
	});

	// little bit of feedback
	taskPanel.on('selectionchange', function(view, nodes) {
		var l = nodes.length;
		var s = l != 1 ? 's' : '';
		taskPanel.setTitle('Items Assigned to Me <i>(' + l
				+ ' item' + s + ' selected)</i>');
	});
}

function buildChartPanel() {
	var chart = Ext.getCmp('chartCmp');
	Ext.create('Ext.panel.Panel', {
		title : 'Relative Workload',
		collapsible:true,
		id : 'chartPanel',
		width : 400,
		height : 300,
		layout: 'fit',
		items: [
		        chart
		        ]
	});
}

function buildItemOverviewPanel() {
	if( params.taskData != undefined )
	{
		Ext.create('Ext.panel.Panel', {
			title : 'Overview',
			collapsible:true,
			bodyPadding:10,
			id : 'itemOverviewPanel',
			width : 700,
			layout: {
				type: 'table',
				columns: 1
			},
			items :[{
						xtype : 'label',
						html : '<b>Title</b> '
					},{
						xtype : 'text',
						text : params.taskData.title
					},{
						xtype : 'label',
						html : '<b>Description</b> '
					},{
						xtype : 'text',
						text : params.taskData.description
					}]
		});
	}
}

function buildItemLinksPanel() {
	if( params.taskData != undefined )
	{
		Ext.define('LinkModel', {
	        extend: 'Ext.data.Model',
	        fields: ['source', 'target', 'type']
		});
		
	    var linkStore = Ext.create('Ext.data.JsonStore', {
	        model: 'LinkModel',
	        proxy: {
	            type: 'ajax',
	            url: 'service/link/'+params.yabtId,
	            method: 'GET',
	            reader: {
	                type: 'json'
	            }
	        }
	    });
	    linkStore.load();

	    var linkPanel = Ext.create('Ext.grid.Panel', {
			title : 'Links',
			collapsible:true,
			bodyPadding:10,
			id : 'itemLinksPanel',
			width : 700,
			height : 150,
			store: linkStore,
			multiSelect : false,
			viewConfig : {
				emptyText : 'No items linked',
			},
			listeners : {
				celldblclick : function(dataview, td, cellIndex, record, tr, rowIndex, e, eOpts) {
					var store = Ext.getCmp('itemLinksPanel').getStore();
					var dataItem = store.getAt(rowIndex);
					var yabtId = dataItem.get('target');
					window.location.href = "/YABT_Web/detail.html?yabtId="+yabtId;
				}
			},
			columns : [ {
				text : "Source",
				flex : 50,
				dataIndex : 'source'
			}, {
				text : 'Type',
				flex : 100,
				dataIndex : 'type'
			}, {
				text : 'Target',
				flex : 50,
				dataIndex : 'target'
			}]
		});
	}
}

function buildAttachmentsPanel() {
	if( params.taskData != undefined )
	{
		Ext.define('AttachmentModel', {
	        extend: 'Ext.data.Model',
	        fields: ['id', 'name', 'type']
		});
		
	    var attachmentStore = Ext.create('Ext.data.JsonStore', {
	        model: 'AttachmentModel',
	        proxy: {
	            type: 'ajax',
	            url: 'service/workflow/attachments/'+params.yabtId,
	            method: 'GET',
	            reader: {
	                type: 'json'
	            }
	        }
	    });
	   attachmentStore.load();

	    var attachmentPanel = Ext.create('Ext.grid.Panel', {
			title : 'Attachments',
			collapsible:true,
			bodyPadding:10,
			id : 'itemAttachmentsPanel',
			width : 700,
			height : 150,
			store: attachmentStore,
			multiSelect : false,
			viewConfig : {
				emptyText : 'No attachments',
			},
			listeners : {
				celldblclick : function(dataview, td, cellIndex, record, tr, rowIndex, e, eOpts) {
					var store = Ext.getCmp('itemAttachmentsPanel').getStore();
					var dataItem = store.getAt(rowIndex);
					
					var win = Ext.create('widget.window', {
				        title: 'Attachment',
				        closable: true,
				        closeAction: 'destroy',
				        width: 600,
				        minWidth: 350,
				        height: 350,
				        layout: {
				            type: 'border',
				            padding: 5
				        },
				        items: [{
				            xtype : 'component',
				            autoEl : {
				                tag : 'iframe',
				                src : 'service/workflow/attachment/'+dataItem.get('id'),
				            }
				        }]
				    });
					win.show();
				}
			},
			columns : [ {
				text : "ID",
				flex : 50,
				dataIndex : 'id'
			}, {
				text : 'Name',
				flex : 100,
				dataIndex : 'name'
			}, {
				text : 'Type',
				flex : 50,
				dataIndex : 'type'
			}]
		});
	}
}

function buildHistoryPanel(){
	if( params.taskData != undefined )
	{
		Ext.define('ItemActivityEntry', {
	        extend: 'Ext.data.Model',
	        fields: [
	            'id', 'title', 'username', 'message', 'filecount', 'secondaryTitle',
	            {name: 'timestamp', type: 'date', dateFormat: 'time'}
	        ]
	    });
	
	    var itemActivitystore = Ext.create('Ext.data.Store', {
	        model: 'ItemActivityEntry',
	        proxy: {
	            type: 'ajax',
	            method: 'GET',
	            url: 'service/activity/'+params.yabtId,
	            reader: {
	                type: 'json',
	            }
	        },
	        sorters: [{
	            property: 'timestamp',
	            direction: 'DESC'
	        }]
	    });
	    itemActivitystore.load();
		
		Ext.create('Ext.grid.Panel', {
	        width: 700,
	        height: 150,
	        colspan: 2,
	        collapsible: true,
	        title: 'Recent Activity',
	        store: itemActivitystore,
	        id: 'itemHistoryPanel',
	        disableSelection: true,
	        loadMask: true,
	        viewConfig: {
	            trackOver: false,
	            stripeRows: false,
	            plugins: [{
	                ptype: 'preview',
	                bodyField: 'message',
	                expanded: true,
	                pluginId: 'pwvItemPlugin'
	            }]
	        },
	        // grid columns
	        columns:[{
	            text: "Title",
	            dataIndex: 'title',
	            flex: 1,
	            renderer: renderTopic,
	            sortable: false
	        },{
	            text: "Author",
	            dataIndex: 'username',
	            width: 100,
	            hidden: true,
	            sortable: true
	        },{
	            text: "Files",
	            dataIndex: 'filecount',
	            width: 70,
	            align: 'right',
	            sortable: true
	        },{
	            text: "Timestamp",
	            dataIndex: 'timestamp',
	            width: 150,
	            renderer: renderTimestamp,
	            sortable: true
	        }]
	    });
	}
}

function buildItemDetailPanel() {
	if( params.taskData != undefined )
	{
		var priorityText;
		switch(params.taskData.priority)
		{
		case 25:
			priorityText = 'Trivial';
			break;
		case 50:
			priorityText = 'Low';
			break;
		case 75:
			priorityText = 'High';
			break;
		case 100:
			priorityText = 'Critical';
			break;
		}
		
		var totalHours = params.taskData.etc + params.taskData.workLogged;
		var percentWorked = 0;
		if( totalHours > 0 )
		{
			percentWorked = (params.taskData.workLogged / (totalHours + 0.0));	
		}
		
		Ext.create('Ext.panel.Panel', {
			title : 'Details',
			collapsible:true,
			bodyPadding:10,
			id : 'itemDetailPanel',
			width : 700,
			height : 210,
			layout: {
				type: 'table',
				columns: 3
			},
			items :[{
				xtype : 'container',
				layout : {
					type : 'table',
					columns : 2
				},
				defaults: {
			        // applied to each contained panel
			        bodyStyle: 'padding-left:20px'
			    },
				items : [{
					xtype : 'label',
					html : '<b>ID:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : params.taskData.yabtId
				},{
					xtype : 'label',
					html : '<b>Current State:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : params.taskData.name
				},{
					xtype : 'label',
					html : '<b>Assignee:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : params.taskData.assignee
				},{
					xtype : 'label',
					html : '<b>Reporter:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : params.taskData.reporter
				}]
			},{
				xtype: 'container', 
				width: 50
			},{
				xtype : 'container',
				layout : {
					type : 'table',
					columns : 2
				},
				items : [{
					xtype : 'label',
					html : '<b>Type:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : params.taskData.type
				},{
					xtype : 'label',
					html : '<b>Submit Date:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : params.taskData.submitDate
				},{
					xtype : 'label',
					html : '<b>Due Date:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : params.taskData.taskDueDateTxt
				},{
					xtype : 'label',
					html : '<b>Priority:</b> '
				},{
					xtype : 'text',
					padding : '0 0 0 20px',
					text : priorityText
				}]
			},{
				xtype : 'container',
				layout : {
					type : 'table',
					columns : 2
				},
				defaults: {
			        // applied to each contained panel
			        bodyStyle: 'padding-left:20px'
			    },
				items : [{
							xtype : 'label',
							html : '<b>Work Log</b>',
							padding : '20px 0 0 0'
						},{
							xtype : 'container',
							width : 50,
							padding : '20px 0 0 0'
						},{
							xtype : 'label',
							html : 'ETC',
							padding : '10px 0 0 20px'
						},{
							xtype : 'container',
							padding : '10px 0 0 40px',
							html : '<div style="height: 10px; width:'+(200 * percentWorked)+'px;background-color: gray;float:left;"></div><div style="height: 10px; width:'+(200 * (100.0 - percentWorked)/100.0)+'px;background-color: rgb(28, 168, 223);"></div><div style="float: right;">'+params.taskData.etc+' hrs</div>'
						},{
							xtype : 'label',
							html : 'Logged',
							padding : '10px 0 0 20px'
						},{
							xtype : 'container',
							padding : '10px 0 0 40px',
							html : '<div style="height: 10px; width:'+(200 * percentWorked)+'px;background-color: orange;float: left;"></div><div style="height: 10px; width:'+(200 * (100.0 - percentWorked)/100.0)+'px;background-color: gray;"></div><div>'+params.taskData.workLogged+' hrs</div>'
					}]
			}]
		});
	}
	else
	{
		Ext.create('Ext.panel.Panel', {
			title : 'Detail',
			collapsible:true,
			id : 'itemDetailPanel',
			width : 700,
			height : 400
		});
	}
};

function buildGlobalActivityPanel() {	
	Ext.define('ActivityEntry', {
        extend: 'Ext.data.Model',
        fields: [
            'id', 'title', 'username', 'message', 'filecount', 'secondaryTitle',
            {name: 'timestamp', type: 'date', dateFormat: 'time'}
        ]
    });

    var store = Ext.create('Ext.data.Store', {
        model: 'ActivityEntry',
        proxy: {
            type: 'ajax',
            method: 'GET',
            url: 'service/activity',
            reader: {
                type: 'json',
            }
        },
        sorters: [{
            property: 'timestamp',
            direction: 'DESC'
        }]
    });
	store.load();
    
	Ext.create('Ext.grid.Panel', {
        width: 800,
        height: 300,
        colspan: 2,
        collapsible: true,
        title: 'Recent Activity',
        store: store,
        id: 'globalActivityPanel',
        disableSelection: true,
        loadMask: true,
        viewConfig: {
            trackOver: false,
            stripeRows: false,
            plugins: [{
                ptype: 'preview',
                bodyField: 'message',
                expanded: true,
                pluginId: 'pvwPlugin'
            }]
        },
        // grid columns
        columns:[{
            text: "Title",
            dataIndex: 'title',
            flex: 1,
            renderer: renderTopic,
            sortable: false
        },{
            text: "Author",
            dataIndex: 'username',
            width: 100,
            hidden: true,
            sortable: true
        },{
            text: "Files",
            dataIndex: 'filecount',
            width: 70,
            align: 'right',
            sortable: true
        },{
            text: "Timestamp",
            dataIndex: 'timestamp',
            width: 150,
            renderer: renderTimestamp,
            sortable: true
        }]
    });
}

function renderTopic(value, p, record) {
    return Ext.String.format(
        '<b>{0}</b><br/><i>{1}</i>',
        record.data.title,
        record.data.secondaryTitle	//For YABT activities should be "YABT-### Action",
        				//For SVN activities should be "Revision ####, YABT-####
    );
}

function renderTimestamp(value, p, r) {
    return Ext.String.format('{0}<br/>by {1}', Ext.Date.dateFormat(value, 'M j, Y, g:i a'), r.get('username'));
}

//functions to display feedback
function onEditButtonClick(btn){
	displayEditItemDialog();
}

//functions to display feedback
function onReassignButtonClick(btn){
	
	Ext.define('UsernameModel', {
        extend: 'Ext.data.Model',
        fields: [
            'username'
        ]
    });

    var store = Ext.create('Ext.data.Store', {
        model: 'UsernameModel',
        proxy: {
            type: 'ajax',
            method: 'GET',
            url: 'service/security/users',
            reader: {
                type: 'json',
            }
        }
    });
	store.load();
	
	var assigneeComboBox = Ext.create('Ext.form.ComboBox', {
	    fieldLabel: 'Choose Assignee',
	    store: store,
	    name : 'assignee',
	    queryMode: 'local',
	    displayField: 'username',
	    valueField: 'username',
	    editable: false,
	    allowBlank: false
	});
	
	var form = Ext.create('Ext.form.Panel', {
	    title: 'Reassign',
	    width: 350,
	    bodyPadding: 10,
	    url: 'service/workflow/process/reassign',
	    layout : 'anchor',
	    defaults: {
	        anchor: '100%'
	    },
	    items: [ 
	            {
		        xtype: 'hiddenfield',
		        name: 'yabtId',
		        value: params.yabtId
		    },
	        assigneeComboBox
	    ],
	 // Cancel and Submit buttons
	    buttons: [
	    {
	        text: 'Cancel',
	        handler: function() {
	            Ext.getCmp('reassignDialog').close();
	        }
	    }, {
	        text: 'Save',
	        formBind: true, //only enabled once the form is valid
	        disabled: true,
	        handler: function() {
	            var form = this.up('form').getForm();
	            if (form.isValid()) {
	                form.submit({
	                	method: 'PUT',
	                	failure: function(){
	                		Ext.Msg.alert("Failed to save changes!");
	                	},
	                	success: function(){
	                		Ext.getCmp('reassignDialog').close();
	                		window.location.reload();
	                	}
	                });
	            }
	        }
	    }],
	});
	
	var dialog = Ext.create('widget.window', {
		title: 'Reassign',
		id : 'reassignDialog',
        closable: false,
        closeAction: 'destroy',
        width: 300,
        minWidth: 300,
        height: 150,
        layout: 'anchor',
        defaults: {
            anchor: '100%'
        },
        items: [form]
	});
	dialog.show();
}

function onWorkflowSelect(item){
	viewProcessDiagram();
}

function onWorkflowStatusSelect(item){
	viewProcessInstanceDiagram();
}

//function to display the notifications window
function onNotificationsButtonClick(btn){
	buildNotificationPanel();
}

function createProcess(type){
	var processType = type.text;
	Ext.Ajax.request({
		async: false,
		url: 'service/workflow/process/'+processType,
		params: {
			assignee: params.username
		},
		method: 'POST',
		success: function(response){
			var yabtId = Ext.JSON.decode(response.responseText).yabtId;
			if( yabtId == -1 )
			{
				Ext.Msg.alert('Failed!', 'Failed to create '+processType);
			}
			else
			{
				Ext.getCmp('myAssignmentPanel').getStore().load();
				init(yabtId);
				displayEditItemDialog();
			}
		}
	});
} 

function viewProcessDiagram(){
	var win = Ext.create('widget.window', {
        title: 'Workflow',
        closable: true,
        closeAction: 'destroy',
        width: 600,
        minWidth: 350,
        height: 350,
        layout: {
            type: 'border',
            padding: 5
        },
        items: [{
            xtype : 'component',
            autoEl : {
                tag : 'iframe',
                src : 'service/workflow/processDiagram/'+params.taskData.type,
            }
        }]
    });
	win.show();
}

function viewProcessInstanceDiagram() {
	var win = Ext.create('widget.window', {
        title: 'Current Workflow Status',
        closable: true,
        closeAction: 'destroy',
        width: 600,
        minWidth: 350,
        height: 350,
        layout: {
            type: 'border',
            padding: 5
        },
        items: [{
            xtype : 'component',
            autoEl : {
                tag : 'iframe',
                src : 'service/workflow/processDiagram/instance/'+params.yabtId,
            }
        }]
    });
	win.show();
}

function displayUploadAttachmentDialog() {
	var form = Ext.create('Ext.form.Panel', {
	    title: 'Upload Attachment',
	    width: 350,
	    bodyPadding: 10,
	    url: 'service/workflow/attachment/'+params.yabtId,
	    layout : 'anchor',
	    defaults: {
	        anchor: '100%'
	    },
	    items: [ 
	            {
			        xtype: 'hiddenfield',
			        name: 'description',
			        value: ''
		        },
		        {
			        xtype: 'textfield',
			        name: 'name',
			        fieldLabel: 'Name',
			        allowBlank: false
		        },
		        {
		            xtype: 'filefield',
		            name: 'content',
		            fieldLabel: 'File',
		            labelWidth: 50,
		            msgTarget: 'side',
		            allowBlank: false,
		            anchor: '100%',
		            buttonText: 'Select File...'
		        }
	    ],
	 // Cancel and Submit buttons
	    buttons: [
	    {
	        text: 'Cancel',
	        handler: function() {
	            Ext.getCmp('attachmentDialog').close();
	        }
	    }, {
	        text: 'Save',
	        formBind: true, //only enabled once the form is valid
	        disabled: true,
	        handler: function() {
	            var form = this.up('form').getForm();
	            if (form.isValid()) {
	                form.submit({
	                	method: 'POST',
	                	failure: function(){
	                		Ext.getCmp('attachmentDialog').close();
	                		window.location.reload();
	                	}
	                });
	            }
	        }
	    }],
	});
	
	var dialog = Ext.create('widget.window', {
		title: 'Upload Attachment',
		id : 'attachmentDialog',
        closable: false,
        closeAction: 'destroy',
        width: 400,
        minWidth: 350,
        height: 200,
        layout: 'anchor',
        defaults: {
            anchor: '100%'
        },
        items: [form]
	});
	dialog.show();
}

function displayLinkCreationDialog() {
	Ext.define('LinkTypeModel', {
        extend: 'Ext.data.Model',
        fields: ['text', 'type']
	});
	
	Ext.define('YabtItemsModel', {
        extend: 'Ext.data.Model',
        fields: ['yabtId']
	});
	
    var linkTypeStore = Ext.create('Ext.data.JsonStore', {
        model: 'LinkTypeModel',
        proxy: {
            type: 'ajax',
            url: 'service/link/type',
            method: 'GET',
            reader: {
                type: 'json'
            }
        }
    });
    linkTypeStore.load();
    
    var yabtIdStore = Ext.create('Ext.data.JsonStore', {
        model: 'YabtItemsModel',
        proxy: {
            type: 'ajax',
            url: 'service/workflow',
            method: 'GET',
            reader: {
                type: 'json'
            }
        }
    });
    
    var targetComboBox = Ext.create('Ext.form.ComboBox', {
	    fieldLabel: 'Choose Target',
	    store: yabtIdStore,
	    name : 'target',
	    queryMode: 'local',
	    displayField: 'yabtId',
	    valueField: 'yabtId',
	    editable: false,
	    disabled: true,
	    allowBlank: false
	});
    
    var linkTypeComboBox = Ext.create('Ext.form.ComboBox', {
	    fieldLabel: 'Choose Link Type',
	    store: linkTypeStore,
	    name : 'type',
	    queryMode: 'local',
	    displayField: 'text',
	    valueField: 'type',
	    editable: false,
	    allowBlank: false,
	    listeners: {
	    	'select' : function(){
	            yabtIdStore.getProxy().setExtraParam('linkType', linkTypeComboBox.getValue()),
	            yabtIdStore.getProxy().setExtraParam('linkSrc', params.yabtId);
	    		yabtIdStore.load();
	    		targetComboBox.enable();
	    	}
	    }
	});
	
	var form = Ext.create('Ext.form.Panel', {
	    title: 'Create Link',
	    width: 350,
	    bodyPadding: 10,
	    url: 'service/link',
	    layout : 'anchor',
	    defaults: {
	        anchor: '100%'
	    },
	    items: [ 
	            {
		        xtype: 'hiddenfield',
		        name: 'source',
		        value: params.yabtId
		    },
	            linkTypeComboBox,
	            targetComboBox
	    ],
	 // Cancel and Submit buttons
	    buttons: [
	    {
	        text: 'Cancel',
	        handler: function() {
	            Ext.getCmp('linkDialog').close();
	        }
	    }, {
	        text: 'Save',
	        formBind: true, //only enabled once the form is valid
	        disabled: true,
	        handler: function() {
	            var form = this.up('form').getForm();
	            if (form.isValid()) {
	                form.submit({
	                	method: 'POST',
	                	failure: function(){
	                		Ext.Msg.alert("Failed to save changes!");
	                	},
	                	success: function(){
	                		Ext.getCmp('linkDialog').close();
	                		window.location.reload();
	                	}
	                });
	            }
	        }
	    }],
	});
	
	var dialog = Ext.create('widget.window', {
		title: 'Link',
		id : 'linkDialog',
        closable: false,
        closeAction: 'destroy',
        width: 400,
        minWidth: 350,
        height: 200,
        layout: 'anchor',
        defaults: {
            anchor: '100%'
        },
        items: [form]
	});
	dialog.show();
}

function displayEditItemDialog() {
	var priorities = Ext.create('Ext.data.Store', {
	    fields: ['priName', 'priVal'],
	    data : [
	        {"priName":"Critical", "priVal":"100"},
	        {"priName":"High", "priVal":"75"},
	        {"priName":"Low", "priVal":"50"},
	        {"priName":"Trivial", "priVal":"25"}
	    ]
	});
	
	var priorityComboBox = Ext.create('Ext.form.ComboBox', {
	    fieldLabel: 'Choose Priority',
	    store: priorities,
	    name : 'priority',
	    queryMode: 'local',
	    displayField: 'priName',
	    valueField: 'priVal',
	    editable: false,
	    allowBlank: false
	});
	
	switch(params.taskData.priority)
	{
		case 25:
			priorityComboBox.select(priorities.getAt(3));
			break;
		case 50:
			priorityComboBox.select(priorities.getAt(2));
			break;
		case 75:
			priorityComboBox.select(priorities.getAt(1));
			break;
		case 100:
			priorityComboBox.select(priorities.getAt(0));
			break;
	}
	
	var form = Ext.create('Ext.form.Panel', {
	    title: 'Edit Item',
	    width: 350,
	    bodyPadding: 10,
	    url: 'service/workflow/task',
	    layout : 'anchor',
	    defaults: {
	        anchor: '100%'
	    },
	    items: [{
	        xtype: 'textfield',
	        name: 'title',
	        fieldLabel: 'Title',
	        allowBlank: false,
	        value: params.taskData.title
	    }, {
	        xtype: 'textarea',
	        grow: true,
	        name: 'description',
	        fieldLabel: 'Description',
	        allowBlank: true,
	        value: params.taskData.description
	    }, priorityComboBox,
	    {
	    	xtype: 'textfield',
	    	listeners: {
	            focus: {
	            	fn: function(){
		            	Ext.create('widget.window', {
		            		title: 'Choose Due Date',
		            		id : 'dueDateWindow',
		                    closable: false,
		                    closeAction: 'destroy',
		                    width: 400,
		                    modal: true,
		                    minWidth: 350,
		                    height: 250,
		                    layout: 'anchor',
		                    defaults: {
		                        anchor: '100%'
		                    },
		                    items: {
		                    	xtype: 'datepicker',
		                    	minDate: new Date(),
		                    	value: new Date(params.taskData.taskDueDate),
		                    	handler: function(picker, date){
		                    		Ext.getCmp('dueDateField').setValue(date.toUTCString());
		                    		Ext.getCmp('dueDateWindow').close();
		                    	}
		                    }
		            	}).show();
	            	}
	            }
	    	},
	        minDate: new Date(),
	        modal: true,
	        id: 'dueDateField',
	        name: 'dueDate',
	        fieldLabel: 'Due Date',
	        value: new Date(params.taskData.taskDueDate).toUTCString(),
	    },{
	        xtype: 'spinnerfield',
	        name: 'etc',
	        id: 'etcField',
	        editable: false,
	        minValue: 0,
	        fieldLabel: 'Hours Remaining',
	        value: params.taskData.etc,
	        onSpinUp: function() {
	        	this.setValue(parseInt(this.getValue()) + 8);
	        },
	        onSpinDown: function() {
	        	this.setValue(parseInt(this.getValue()) - 8);
	        	if( parseInt(this.getValue()) < 0 )
	        	{
	        		this.setValue(0);
	        	}
	        }
	    },{
	        xtype: 'spinnerfield',
	        name: 'workLogged',
	        id: 'workLoggedField',
	        editable: false,
	        minValue: 0,
	        fieldLabel: 'Hours Logged',
	        value: params.taskData.workLogged,
	        onSpinUp: function() {
	        	this.setValue(parseInt(this.getValue()) + 1);
	        	var etc = Ext.getCmp('etcField');
	        	etc.setValue(Math.max(etc.getValue() - 1, 0));
	        },
	        onSpinDown: function() {
	        	if( parseInt(this.getValue()) > 0 )
	        	{
		        	this.setValue(parseInt(this.getValue()) - 1);
		        	var etc = Ext.getCmp('etcField');
		        	etc.setValue(parseInt(etc.getValue()) + 1);
	        	}
	        }
	    },{
	        xtype: 'hiddenfield',
	        name: 'yabtId',
	        value: params.yabtId
	    }
	    ],
	 // Reset and Submit buttons
	    buttons: [{
	        text: 'Cancel',
	        handler: function() {
	            Ext.getCmp('editDialog').close();
	        }
	    }, {
	        text: 'Reset',
	        handler: function() {
	            this.up('form').getForm().reset();
	        }
	    }, {
	        text: 'Save',
	        formBind: true, //only enabled once the form is valid
	        disabled: true,
	        handler: function() {
	            var form = this.up('form').getForm();
	            if (form.isValid()) {
	            	if( (parseInt(Ext.getCmp('etcField').getValue()) + parseInt(Ext.getCmp('workLoggedField').getValue())) < 8 )
	            	{
	            		Ext.Msg.alert("Validation Error", "ETC and Work Logged must combine to at least 8 hours");
	            	}
	            	else
	            	{
		                form.submit({
		                	method: 'PUT',
		                	failure: function(){
		                		Ext.Msg.alert("Error", "Failed to save changes!");
		                	},
		                	success: function(){
		                		Ext.getCmp('editDialog').close();
		                		window.location.reload();
		                	}
		                });
	            	}
	            }
	        }
	    }],
	});
	
	var dialog = Ext.create('widget.window', {
		title: 'Edit',
		id : 'editDialog',
        closable: false,
        closeAction: 'destroy',
        width: 400,
        minWidth: 350,
        height: 350,
        layout: 'anchor',
        defaults: {
            anchor: '100%'
        },
        items: [form]
	});
	dialog.show();
}

function buildChart() {
	Ext.define('ChartModel', {
		extend: 'Ext.data.Model',
		fields: ['assignee', {name: 'num', type: 'int'}]
	});

	var chartStore = Ext.create('Ext.data.JsonStore', {
		model: 'ChartModel',
		proxy: {
			type: 'ajax',
			url: 'service/chart/workload',
			method: 'GET',
			reader: {
				type: 'json'
			}
		}
	});
	chartStore.load();
	
	Ext.create('Ext.chart.Chart', {
	 	   id: 'chartCmp',
	 	   animate: true,
	 	   store: chartStore,
	 	   shadow: true,
	 	   height: 300,
	 	   width: 400,
	 	   legend: {
	 		   position: 'right'
	 	   },
	 	   theme: 'Base:gradients',
	 	   series: [{
	 		   type: 'pie',
	 		   field: 'num',
	 		   showInLegend: true,
	 		   tips: {
	 			   trackMouse: true,
	 			   width: 140,
	 			   height: 28,
	 			   renderer: function(storeItem, item) {
	 				   this.setTitle(storeItem.get('assignee') + ': ' + storeItem.get('num'));
	 			   }
	 		   },
	 		   highlight: {
	 			   segment: {
	 				   margin: 20
	 			   }
	 		   },
	 		   label: {
	 			   field: 'assignee',
	 			   display: 'rotate',
	 			   contrast: true,
	 			   font: '10px Arial'
	 		   }
	 	   }]
	    });
}

function onSearchButtonClick()
{
	var form = Ext.create('Ext.form.Panel', {
	    title: 'Search',
	    width: 350,
	    bodyPadding: 10,
	    url: 'service/workflow/task',
	    layout : 'anchor',
	    defaults: {
	        anchor: '100%'
	    },
	    items: [{
	        xtype: 'textfield',
	        name: 'title',
	        fieldLabel: 'Title',
	        allowBlank: true,
	    }, {
	        xtype: 'textfield',
	        name: 'description',
	        fieldLabel: 'Description',
	        allowBlank: true,
	    }, {
	        xtype: 'textfield',
	        grow: true,
	        name: 'assignee',
	        fieldLabel: 'Assignee',
	        allowBlank: true,
	    }, {
	    	xtype: 'textfield',
	    	listeners: {
	            focus: {
	            	fn: function(){
		            	Ext.create('widget.window', {
		            		title: 'Choose Due Date',
		            		id : 'dueDateWindow',
		                    closable: false,
		                    closeAction: 'destroy',
		                    width: 400,
		                    modal: true,
		                    minWidth: 350,
		                    height: 250,
		                    layout: 'anchor',
		                    defaults: {
		                        anchor: '100%'
		                    },
		                    items: {
		                    	xtype: 'datepicker',
		                    	value: new Date(),
		                    	handler: function(picker, date){
		                    		Ext.getCmp('dueDateField').setValue(date.toUTCString());
		                    		Ext.getCmp('dueDateWindow').close();
		                    	}
		                    }
		            	}).show();
	            	}
	            }
	    	},
	        modal: true,
	        id: 'dueDateField',
	        name: 'dueDate',
	        fieldLabel: 'Due-Before Date',
	        allowBlank: true
	    },{
	        xtype: 'hiddenfield',
	        name: 'dateMethod',
	        value: 'BEFORE'
	    },{
	        xtype: 'hiddenfield',
	        name: 'active',
	        value: true
	    },{
	        xtype: 'hiddenfield',
	        name: 'inactive',
	        value: true
	    }
	    ],
	 // Reset and Submit buttons
	    buttons: [{
	        text: 'Cancel',
	        handler: function() {
	            Ext.getCmp('searchDialog').close();
	        }
	    },{
	        text: 'Reset',
	        handler: function() {
	            this.up('form').getForm().reset();
	        }
	    },{
	        text: 'Search',
	        formBind: true, //only enabled once the form is valid
	        disabled: true,
	        handler: function() {
	            var form = this.up('form').getForm();
	            if (form.isValid()) {
	                form.submit({
	                	method: 'GET',
	                	failure: function(form, action){
	                		Ext.getCmp('searchDialog').close();
	                		showSearchResultsDialog(action);
	                	},
	                	success: function(form, action){
	                		Ext.getCmp('searchDialog').close();
	                		showSearchResultsDialog(action);
	                	}
	                });
	            }
	        }
	    }],
	});
	var dialog = Ext.create('widget.window', {
		title: 'Search',
		id : 'searchDialog',
        closable: true,
        closeAction: 'destroy',
        width: 400,
        minWidth: 350,
        height: 250,
        layout: 'anchor',
        defaults: {
            anchor: '100%'
        },
        items: [form]
	});
	dialog.show();
}

function showSearchResultsDialog(action)
{
	var searchResults = action.result;

	Ext.define('SearchResultsModel', {
        extend: 'Ext.data.Model',
        fields: ['assignee', {name:'createTime', type:'date', dateFormat:'timestamp'}, 'delegationState', 'description', 'taskDueDateTxt2', 'executionId', 'id', 'name', 'owner', 'parentId', {name:'priority', type:'integer'}, 'processDefinitionId', 'processInstanceId', 'yabtId', 'type', 'definitionKey']
	});
	
    var taskStore = Ext.create('Ext.data.JsonStore', {
        model: 'SearchResultsModel',
        data: []
    });
    taskStore.loadData(searchResults);

    var taskPanel = Ext.create('Ext.grid.Panel', {
        width:400,
        id: 'searchResultsPanel',
        height:300,
        collapsible:true,
        title:'Search Results',
        store: taskStore,
		multiSelect : false,
		viewConfig : {
			emptyText : 'No results',
		},
		listeners : {
			celldblclick : function(dataview, td, cellIndex, record, tr, rowIndex, e, eOpts) {
				var store = Ext.getCmp('searchResultsPanel').getStore();
				var dataItem = store.getAt(rowIndex);
				var yabtId = dataItem.get('yabtId');
				window.location.href = "/YABT_Web/detail.html?yabtId="+yabtId;
			}
		},
		columns : [ {
			text : "ID",
			flex : 50,
			dataIndex : 'yabtId'
		}, {
			text : 'Type',
			flex : 50,
			dataIndex : 'type'
		}, {
			text : 'State',
			flex : 50,
			dataIndex : 'name'
		}, {
			text : 'Due Date',
			//xtype : 'datecolumn',
			//format : 'm-d h:i a',
			flex : 50,
			dataIndex : 'taskDueDateTxt2'
		} ]
	});

	// little bit of feedback
	taskPanel.on('selectionchange', function(view, nodes) {
		var l = nodes.length;
		var s = l != 1 ? 's' : '';
		taskPanel.setTitle('Search Results <i>(' + l
				+ ' item' + s + ' selected)</i>');
	});
	
	var dialog = Ext.create('widget.window', {
		title: 'Search Results',
		id : 'searchResultsDialog',
        closable: true,
        closeAction: 'destroy',
        width: 600,
        minWidth: 350,
        height: 350,
        layout: 'anchor',
        defaults: {
            anchor: '100%'
        },
        items: [taskPanel]
	});
	dialog.show();
}
