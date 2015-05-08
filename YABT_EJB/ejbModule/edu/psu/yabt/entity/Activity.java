package edu.psu.yabt.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity	
@Table( name="ACTIVITY" )
public class Activity 
{
	@Id
	@Column( name="YABTID")
	private String yabtId;
	
	@Id
	@Column( name="TITLE" )
	private String title;
	
	@Id
	@Column( name="AUTHOR")
	private String author;
	
	@Id
	@Column( name="TIMESTAMP" )
	private long timestamp;
	
	public Activity()				//Empty constructor (required by the ORM)
	{}

	public String getYabtId(){
		return yabtId;
	}
	
	public void setYabtId(String yabtId) {
		this.yabtId = yabtId;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int)timestamp;
		result = prime * result + ((author == null) ? 0 : author.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((yabtId == null) ? 0 : yabtId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Activity other = (Activity) obj;
		if (!(timestamp == other.timestamp))
			return false;
		if (author == null) {
			if (other.author != null)
				return false;
		} else if (!author.equals(other.author))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (yabtId == null) {
			if (other.yabtId != null)
				return false;
		} else if (!yabtId.equals(other.yabtId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Activity [yabtId =" + yabtId + ", title=" + title + ", author=" + author + ", timestamp="
				+ timestamp + "]";
	}
}
