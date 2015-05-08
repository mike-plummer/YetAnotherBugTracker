package edu.psu.yabt.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import edu.psu.yabt.notification.LinkType;

@Entity	
@Table( name="LINK" )
public class Link 
{
	@Id
	@Column( name="SOURCE" )
	private String source;
	
	@Id
	@Column( name="TARGET")
	private String target;
	
	@Id
	@Column( name="LINK_TYPE" )
	@Enumerated(EnumType.STRING)
	private LinkType linkType;
	
	public Link()				//Empty constructor (required by the ORM)
	{}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public LinkType getLinkType() {
		return linkType;
	}

	public void setLinkType(LinkType linkType) {
		this.linkType = linkType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((linkType == null) ? 0 : linkType.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		Link other = (Link) obj;
		if (linkType != other.linkType)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Link [source=" + source + ", target=" + target + ", linkType="
				+ linkType + "]";
	}
}
