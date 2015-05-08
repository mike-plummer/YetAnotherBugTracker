package edu.psu.yabt.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import edu.psu.yabt.notification.Condition;

//This stuff at the top sets up a framework known as Spring which assists
//with using ORM in a Unit Test. Note the "Transactional" - this, among other things,
//causes each Test to rollback any database changes it's made, so each test gets a
//clean database.
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( { "/applicationContext-test.xml" })
@Transactional
public class ModelTest {

	// This is the EntityManager. The EM handles the conversion from Object <-> Database.
	// You can think of it as a abstraction of a database connection. Using it you can create
	// queries, insert new Objects (which become rows in a DB table), update existing data,
	// or delete data from the database
	@PersistenceContext
	private EntityManager entityManager;
	
	//This first test verifies that the EntityManager got built correctly and is available for use.
	// There is a lot of 'magic' that goes into creating the EM - basically, we define a 'persistence.xml'
	// file which defines how we connect to the database, where the DB is, what Entities is should be 
	// concerned with, and whether to map Entities to existing tables or create tables based on them. Typically,
	// an application server (like GlassFish) provides the actual connectivity between the setup of persistence.xml 
	// and the actual database, but since this is a Unit Test I've bootstrapped a special framework known as Spring
	// to manage this stuff for us. The Spring configuration is in 'applicationContext.xml' but you shouldn't 
	// spend too much time on digesting the contents of that file.
	@Test
	public void testDatabaseConfiguration() {
		assertTrue(entityManager != null);
	}
	
	//This test creates a new User object (which is an Entity), then passes it to the EM to persist to the database.
	@Test
	public void testUserCreation() throws Exception {
		User myUser = new User();			//Construct a new User
		myUser.setName("John Doe");		//Set the firstName attribute
		myUser.setUsername("jdoe");			//Set the username attribute
		myUser.setEmail("johndoe@yabt.com");//Set the email attribute
		
		entityManager.persist(myUser);		//Pass our entity instance to the EM. This line performs the 'magic'. Behind the scenes,
											//the User object is converted to SQL and INSERTED into the appropriate table.
											//At this point, the database now contains a new row in the User table.
		
		//This line uses the EM to build a query (it supports native SQL queries or more compact queries using the JQL language which
		// is what I've used here). This query just does a "SELECT * FROM USER", then counts the number of returned values. At this point,
		// since we've only persisted one User, the expected result is that the table contains one row.
		assertTrue( entityManager.createQuery("select u from User u", User.class).getResultList().size() == 1 );
		
		//We now build another query to retrieve the User object that we persisted. Behind the scenes, it performs a SQL query,
		//converts the SQL result, builds a User object using the result, and gives me back the User object.
		User retrievedUser = (User) entityManager.createQuery("select u from User u", User.class).getSingleResult();
		//Print out the retrieved object (for debugging)
		System.out.println(retrievedUser.toString());
		//To verify that the mappings between object and SQL are correct, we check that the object we persisted is equal
		//to the object we got back from our query.
		assertEquals(myUser, retrievedUser);
	}
	
	//This tests that we can update data in the database
	@Test
	public void testUserUpdate() {
		//Create a User
		User myUser = new User();
		myUser.setName("John Doe");
		myUser.setUsername("jdoe");
		myUser.setEmail("johndoe@yabt.com");
		//Persist the user into the database
		entityManager.persist(myUser);		
		
		//Set the firstName of our user to something else
		myUser.setName("Jane Doe");
		//use the EM to merge the changed value into the database. This really performs an UPDATE SQL query
		entityManager.merge(myUser);
		
		//Retrieve our user object from the database
		User retrievedUser = (User) entityManager.createQuery("select u from User u", User.class).getSingleResult();
		//If the update worked, then the retrieved object should not have firstName == Joe
		assertTrue( !"John Doe".equals(retrievedUser.getName()) );
		//Verify that there's still only one User in the database. If there's more, then our UPDATE did something odd
		assertTrue( entityManager.createQuery("select u from User u", User.class).getResultList().size() == 1 );
	}
	
	//Test we can delete from the database
	@Test
	public void testUserDeletion() {
		//Create a new User
		User myUser = new User();
		myUser.setName("John Doe");
		myUser.setUsername("jdoe");
		myUser.setEmail("johndoe@yabt.com");
		//Save to database
		entityManager.persist(myUser);
		
		//Retrieve User from DB to verify it saved
		User retrievedUser = (User) entityManager.createQuery("select u from User u", User.class).getSingleResult();
		//Delete the retrieved user from the database. This really performs a SQL DELETE
		entityManager.remove(retrievedUser);
		//Query the database, verify that there are no User entries in the DB
		assertTrue( entityManager.createQuery("select u from User u", User.class).getResultList().size() == 0 );
	}
	
	@Test
	public void testNotificationConditionCreation() {
		Notification n = new Notification();
		n.setCondition(Condition.ADVANCED);
		n.setSubject("YABT-0001");
		n.setUsername("mplummer");
		entityManager.persist(n);
		
		assertEquals(entityManager.createQuery("select n from Notification n", Notification.class).getResultList().size(), 1);
	}
	
	@Test
	public void testNotificationConditionDeletion() {
		Notification n = new Notification();
		n.setCondition(Condition.ADVANCED);
		n.setSubject("YABT-0002");
		n.setUsername("mplummer");
		entityManager.persist(n);
		
		assertEquals(entityManager.createQuery("select n from Notification n", Notification.class).getResultList().size(), 1);
		
		entityManager.remove(n);
		assertEquals(entityManager.createQuery("select n from Notification n", Notification.class).getResultList().size(), 0);
	}
}
