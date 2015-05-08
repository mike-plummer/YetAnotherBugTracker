package edu.psu.yabt.notification;

public interface WorkflowListener 
{
	public void workflowEventOccurred(String username, Condition condition, String subject);
}
