function buildNotificationPanel()
{
	var registeredNotifications;
	Ext.Ajax.request({
		async: false,
		url: 'service/notification',
		params : {
			yabtId : params.yabtId
		},
		method : 'GET',
		success: function(response){
			registeredNotifications =Ext.JSON.decode(response.responseText);
			var reassignSelected = false, 
				linkSelected = false, 
				updateSelected = false, 
				advanceSelected = false, 
				completeSelected = false;

			for( var n in registeredNotifications )
			{
				if( registeredNotifications[n].notificationType.indexOf('REASSIGNED') != -1 )
				{
					reassignSelected = true;
				}
				else if( registeredNotifications[n].notificationType.indexOf('LINKED') != -1 )
				{
					linkSelected = true;
				}
				else if( registeredNotifications[n].notificationType.indexOf('UPDATED') != -1 )
				{
					updateSelected = true;
				}
				else if( registeredNotifications[n].notificationType.indexOf('ADVANCED') != -1 )
				{
					advanceSelected = true;
				}
				else if( registeredNotifications[n].notificationType.indexOf('COMPLETED') != -1 )
				{
					completeSelected = true;
				}
			}
			
			var form = Ext.create('Ext.form.Panel', {
				width: 350,
				bodyPadding: 10,
				url: 'service/notification',
				layout : 'anchor',
				defaults: {
					anchor: '100%'
				},
				items:[{
					xtype: 'text',
					text: 'Select the conditions you want to be notified of.'
				},{
					xtype: 'checkboxgroup',
					fieldLabel: 'Notification Conditions',
					// Arrange checkboxes into two columns, distributed vertically
					columns: 2,
					vertical: true,
					items: [
					        { boxLabel: 'Reassignment', name: 'REASSIGNED', inputValue: 'REASSIGNED', checked: reassignSelected},
					        { boxLabel: 'Links', name: 'LINKED', inputValue: 'LINKED', checked: linkSelected },
					        { boxLabel: 'Updates', name: 'UPDATED', inputValue: 'UPDATED', checked: updateSelected },
					        { boxLabel: 'Advancement', name: 'ADVANCED', inputValue: 'ADVANCED', checked: advanceSelected },
					        { boxLabel: 'Completion', name: 'COMPLETED', inputValue: 'COMPLETED', checked: completeSelected }
					        ]
				},{
					xtype: 'hiddenfield',
					name: 'yabtId',
					value: params.yabtId
				}],
				// Cancel and Submit buttons
				buttons: [
				          {
				        	  text: 'Cancel',
				        	  handler: function() {
				        		  Ext.getCmp('notificationDialog').close();
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
				        					  Ext.getCmp('notificationDialog').close();
				        					  window.location.reload();
				        				  }
				        			  });
				        		  }
				        	  }
				          }],
			});

			var dialog = Ext.create('widget.window', {
				title: 'Notification Conditions',
				id : 'notificationDialog',
				closable: false,
				closeAction: 'destroy',
				width: 400,
				minWidth: 400,
				height: 200,
				layout: 'anchor',
				defaults: {
					anchor: '100%'
				},
				items: [form]
			});
			dialog.show();
		}
	});
}