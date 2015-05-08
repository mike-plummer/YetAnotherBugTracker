package edu.psu.yabt.notification;

import java.util.Arrays;

import org.junit.Test;

public class NotificationTest {
	
	@Test
	public void test() throws Exception {
		Notifier.generateNotificationUsingExternalSMTP(Arrays.asList(new String[]{""}), "This is a unit test! Link to <a href=\"http://localhost:8080/YABT_Web/detail.html?yabtId=YABT-0001\">YABT-0001</a>");
		
//		Thread.sleep(120000);
	}

}
