package com.martinmelis.web.farefinder.modules;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import dataTypes.RoundTripFare;


public class MailSender {

	
	
	public void sendMail(String reciept, RoundTripFare fare) {

		final String username = "farenotification@gmail.com";
		final String password = "lokojoho486";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		  });

		try {

			String messageString = "New fare accounted:\n" + fare.getOrigin().getCityName() + "\tto\t" + fare.getDestination().getCityName() + "\n"
								+ "Price:\t" + fare.getPrice() + "\n"
								+ "Sale:\t" + fare.getSaleRatio() + "\n"
								+ "Booking URL:\t" + fare.getBookingURL() + "\n";
			
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("notification@farefinder.com"));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(reciept));
			message.setSubject("Farefinder - FARE NOTIFICATION!");
			message.setText(messageString);

			Transport.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		
}
}
