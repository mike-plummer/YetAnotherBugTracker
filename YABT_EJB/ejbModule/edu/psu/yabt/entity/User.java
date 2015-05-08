package edu.psu.yabt.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/*
 * This class is an ORM Entity. It represents a Table in the Database. Instances of this class
 * that get build represent rows in that DB table. To save data to the DB, build a new instance
 * of this class, populate it with data, then give it to an entityManager (see ModelTest.java)
 * to persist to the DB. To retrieve data from the database, build a query with an entitymanager,
 * which will convert the contents of the database into Entity objects as appropriate.
 * 
 * This class is tied-in to the ORM framework by the persistence.xml file.
 */

@Entity					//This marks the class as a database Entity
@Table( name="USER" )	//This tells the ORM to map this Entity to a table named USER in the DB
public class User 
{
	@Id							//This marks this field as the Primary Key
	@Column( name="USERNAME" )	//Map this field to a named column in the DB table
	private String username;	//Regular Java variable
	
	@Column( name="NAME")
	private String name;
	
	@Column( name="EMAIL" )
	private String email;
	
	public User()				//Empty constructor (required by the ORM)
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	/*
	 * End Get and Set methods
	 */
	
	/*
	 * Don't worry about these methods - they're useful for determining equality between
	 * Java objects
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
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
		User other = (User) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	/*
	 * End Don't worry about these methods
	 */
	
	//This method just generates a textual representation of this object - this comes in handy
	//for debug output
	@Override
	public String toString() {
		return "User [username=" + username + ", name=" + name
				 + ", email=" + email + "]";
	}
}
