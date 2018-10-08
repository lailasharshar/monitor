package com.sharshar.monitor.services;

import com.sendgrid.*;
import com.sharshar.monitor.exceptions.NotificationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by lsharshar on 10/5/2018.
 */
@Service
public class NotificationService {
	private static Logger logger = LogManager.getLogger();

	@Value( "${sendgrid.api_key}" )
	private String apiKey;

	@Value( "${notification.sendFrom}" )
	private String sendFrom;

	@Value( "${notification.sendTo}" )
	private String sendTo;

	@Value( "${notification.smsAddress}" )
	private String smsAddress;

	public void notifyMe(String subject, String contentString) throws NotificationException {
		Email from = new Email(sendFrom);
		Email to = new Email(sendTo);
		Content content = new Content("text/html", contentString);
		Mail mail = new Mail(from, subject, to, content);
		processMsg(mail);
	}

	public void textMe(String subject, String val) throws NotificationException {
		Email from = new Email(sendFrom);
		Email to = new Email(smsAddress);
		Content content = new Content("text/plain", val);
		logger.info("Texting: " + subject + ", " + val);
		Mail mail = new Mail(from, subject, to, content);
		processMsg(mail);
	}

	private void processMsg(Mail mail) throws NotificationException {

		SendGrid sg = new SendGrid(apiKey);
		Request request = new Request();
		Response response = null;
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			response = sg.api(request);
		} catch (IOException ex) {
			logger.error(response.getStatusCode());
			logger.error(response.getBody());
			logger.error(response.getHeaders());
			if (mail.getContent() != null && mail.getContent().size() > 0) {
				logger.error(mail.getSubject() + " - " + mail.getContent().get(0).getValue());
			}
			throw new NotificationException("Error sending notification to me", ex);
		}
	}
}
