package edu.psu.yabt.notification;

import java.util.Collection;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.masukomi.aspirin.Aspirin;
import org.masukomi.aspirin.core.AspirinInternal;
import org.masukomi.aspirin.core.listener.AspirinListener;
import org.masukomi.aspirin.core.listener.ResultState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This uses the ASPIRIN library to send simple messages
 */
public class Notifier 
{
	private static final Logger logger = LoggerFactory.getLogger(Notifier.class);
	
	private static final String LOCAL_HOSTNAME = ""; //TODO
	
	//Restrictions by ISPs and most email providers reject email sent by SMTP servers on dynamic IPs.
	//Thus, we're temporarily using GMail SMTP relays, which you have to authenticate against.
	//DO NOT COMMIT CODE WITH YOUR GMAIL CREDENTIALS!!!
	
	private static final String YOUR_FULL_GMAIL_ADDRESS = "";	//TODO
	private static final String YOUR_GMAIL_PASSWORD = "";		//TODO Enter your password. If you have 2-factor authentication enabled then this should be an application-specific password
	
	public static final void generateNotificationUsingExternalSMTP(Collection<String> recipientEmailAddresses, String bodyContent) throws AddressException, MessagingException
	{
		final String username = YOUR_FULL_GMAIL_ADDRESS;
		final String password = YOUR_GMAIL_PASSWORD;
 
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
 
		Session session = Session.getInstance(props, new Authenticator() 
			{
				protected PasswordAuthentication getPasswordAuthentication() 
				{
					return new PasswordAuthentication(username, password);
				}
			}
		);
 
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(YOUR_FULL_GMAIL_ADDRESS));
		for( String emailAddress : recipientEmailAddresses )
		{
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
		}
		message.setSubject("YABT Notification");
		message.setContent(buildContent(bodyContent), "text/html");

		Transport.send(message);
	}
	
	public static final void generateNotificationUsingStandaloneSMTP(Collection<String> recipientEmailAddresses, String bodyContent) throws AddressException, MessagingException
	{
		logger.info("Starting email attempt...");
        MimeMessage message = AspirinInternal.createNewMimeMessage();

        message.setFrom(new InternetAddress("admin@yabt.org"));			//Who this message will be from
        for( String emailAddress : recipientEmailAddresses )
        {
        	message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));	//You can set any number
        																						//of recipients by adding addtl.
        																						//lines like this one
        }
        message.setSubject("YABT Notification");			//Subject line
        message.setContent(buildContent(bodyContent), "text/html");
        Aspirin.add(message);								//Queue message for delivery. It can can sit there for anywhere
        													//between a few seconds and a few minutes
        logger.info("Email queued for delivery");
	}
	
	private static final String buildContent(String content)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<div style=\"height: 50px;background: #F4F8F9;background-image: -moz-linear-gradient(#7CBC25, #608E1D);background-image: -webkit-gradient(linear, 0% 0%, 0% 100%, from(#7CBC25), to(#608E1D));background-image: -webkit-linear-gradient(#7CBC25, #608E1D); filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#7CBC25', endColorstr='#608E1D');\">\n");
		sb.append("<span>\n");
		sb.append("<span>\n");
		sb.append("<a style=\"display: block;width: 221px;height: 30px;float: left;margin: 15px 0 0 5px;text-indent: -5000px;\" href=\"http://localhost:8080/YABT_Web/dashboard.html\"><img src=\"http://localhost:8080/YABT_Web/resources/img/logo.png\"/></a>\n");
		sb.append("</span>\n");
		sb.append("<span style=\"color: #FFFFFF;vertical-align: bottom;text-shadow: 1px 1px 3px #000000;font-size: 200%;margin: 15px 0 10px 5px;font-family: Helvetica,sans-serif;position: absolute;\">Yet Another Bug Tracker</span>\n");
		sb.append("</span>\n");
		sb.append("</div>\n");
		sb.append("<br />\n<br />\n");
		sb.append(content);
		return sb.toString();
	}
	
	static
	{
		Aspirin.getConfiguration().setHostname(LOCAL_HOSTNAME);			//I'm not 100% sure this is required, but I know
																		//I got a successful test with it set
		Aspirin.getConfiguration().setDeliveryBounceOnFailure(false);	//Don't bother sending failures back to us
		
		/*
		 * This just prints the configuration to the console for debugging purposes
		 */
		StringBuilder buff = new StringBuilder();
		buff.append("Aspirin Configuration").append("\n");
		buff.append("DeliveryAttemptCount: "+Aspirin.getConfiguration().getDeliveryAttemptCount()).append("\n");
		buff.append("DeliveryAttemptDelay: "+Aspirin.getConfiguration().getDeliveryAttemptDelay()).append("\n");
		buff.append("DeliveryThreadsActiveMax: "+Aspirin.getConfiguration().getDeliveryThreadsActiveMax()).append("\n");
		buff.append("DeliveryThreadsIdleMax: "+Aspirin.getConfiguration().getDeliveryThreadsIdleMax()).append("\n");
		buff.append("DeliveryTimeout: "+Aspirin.getConfiguration().getDeliveryTimeout()).append("\n");
		buff.append("Encoding: "+Aspirin.getConfiguration().getEncoding()).append("\n");
		buff.append("Expiry: "+Aspirin.getConfiguration().getExpiry()).append("\n");
		buff.append("Hostname: "+Aspirin.getConfiguration().getHostname()).append("\n");
		logger.info(buff.toString());
		/*
		 * End debug
		 */
		
		/*
		 * Adds a callback listener for mail results. This prints any result of a mail operation to the console
		 */
		Aspirin.addListener(new AspirinListener(){

			@Override
			public void delivered(String arg0, String arg1, ResultState arg2,
					String arg3) {
				logger.info("Mail result! arg0="+arg0+", arg1="+arg1+", result="+arg2.toString()+", arg3="+arg3);
			}
		});
	}
}
