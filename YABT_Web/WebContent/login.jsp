<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
	<title>Login Page</title>
	<meta HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
	<META HTTP-EQUIV="Pragma" CONTENT="no-cache">
	
	<link rel="stylesheet" type="text/css" href="resources/css/ext-all.css" />
	<link rel="stylesheet" type="text/css" href="resources/css/yabt.css" />
	
	<script type="text/javascript" src="resources/js/ext-all-debug.js"></script>
	<script type="text/javascript" src="resources/js/yabt.js"></script>
	
	<script>       
	Ext.onReady(function() {
		
		var form = Ext.create('Ext.form.Panel', {
		    title: 'Enter Credentials',
		    standardSubmit: true,
		    bodyPadding: 10,
		    url: 'j_spring_security_check',
		    layout : 'anchor',
		    width: 375,
		    listeners: {
		        afterRender: function(thisForm, options){
		            this.keyNav = Ext.create('Ext.util.KeyNav', this.el, {                    
		                enter: function() {
				            var form = this.getForm();
				            if (form.isValid()) {
				                form.submit({
				                	method: 'POST'
				                });
				            }
				        },
		                scope: this
		            });
		        }
		    },
		    defaults: {
		        anchor: '100%'
		    },
		    items: [ 
		            {
				        xtype: 'textfield',
				        name: 'j_username',
				        fieldLabel: 'Username',
				        allowBlank: false,
				        id: 'usernameField'
			        },
			        {
				        xtype: 'textfield',
				        inputType: 'password',
				        name: 'j_password',
				        fieldLabel: 'Password',
				        allowBlank: true
			        }
		    ],
		 // Cancel and Submit buttons
		    buttons: [{
		        text: 'Submit',
		        formBind: true, //only enabled once the form is valid
		        disabled: true,
		        handler: function() {
		            var form = this.up('form').getForm();
		            if (form.isValid()) {
		                form.submit({
		                	method: 'POST'
		                });
		            }
		        }
		    }],
		});
		
		var win = Ext.create('widget.window', {
	        title: 'Login',
	        closable: false,
	        closeAction: 'destroy',
	        width: 400,
	        height: 200,
	        layout: {
	            type: 'border',
	            padding: 5
	        },
	        items: [form]
	    });
		win.show();
		
		Ext.getCmp('usernameField').focus(false, 500);
	});
	</script>
	
</head>
<body>
	<header>
		<div class="wrapper">
			<span>
				<span class="logo">
					<a href="dashboard.html">Yet Another Bug Tracker</a>
				</span>
				<span class="appName">Yet Another Bug Tracker</span>
			</span> 
		</div>
		<!--.wrapper-->
	</header>
	<div id="content">
		<c:if test="${not empty param.login_error}">
			<div class="message">
				Your login attempt was not successful, try again.<br /> 
				Reason : ${sessionScope["SPRING_SECURITY_LAST_EXCEPTION"].message}
			</div>
		</c:if>
	 	<c:if test="${not empty param.logout}">
			<div class="message">
				You have successfully logged out.<br /> 
				To continue using YABT you will need log back in.
			</div>
		</c:if>
	</div>
</body>
</html>