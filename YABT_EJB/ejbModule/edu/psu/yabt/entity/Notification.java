package edu.psu.yabt.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import edu.psu.yabt.notification.Condition;

@Entity	
@Table( name="NOTIFICATION" )
public class Notification 
{
	@Id
	@Column( name="USERNAME" )
	private String username;
	
	@Id
	@Column( name="CONDITION")
	@Enumerated(EnumType.STRING)
	private Condition condition;
	
	@Id
	@Column( name="SUBJECT" )
	private String subject;
	
	public Notification()				//Empty constructor (required by the ORM)
	{}
	
	/*
	 * This block is simply Get and Set methods for the variables above.
	 */
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	/*
	 * End Get and Set methods
	 */

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "Notification [username=" + username + ", condition="
				+ condition + ", subject=" + subject + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Notification other = (Notification) obj;
		if (condition != other.condition)
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
}
