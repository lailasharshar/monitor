package com.sharshar.monitor.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

/**
 * Used to monitor a remote server
 *
 * Created by lsharshar on 10/5/2018.
 */
@Service
public class Monitor {
	Logger logger = LogManager.getLogger();

	@Autowired
	NotificationService notificationService;

	@Value( "${monitor.url}" )
	private String url;

	@Value( "${monitor.successMessage}" )
	private String successMessage;

	@Value( "${monitor.alertTime}" )
	private long alertTime;

	private static Date lastSuccess = new Date();
	private boolean inError = false;

	@Scheduled(fixedRate = 5000)
	public void checkOnService() {
		Date now = new Date();
		RestTemplate restTemplate = new RestTemplate();
		String errorMsg = "";
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			if (successMessage.equalsIgnoreCase(response.getBody())) {
				lastSuccess = now;
				if (inError) {
					logger.info("We're back up");
					notificationService.textMe("We're back up", "No more error");
				}
				inError = false;
			} else {
				errorMsg = response.getBody() + " ";
			}
		} catch (Exception ex) {
			long downTime = (now.getTime() - lastSuccess.getTime()) / 1000;
			errorMsg += ex.getMessage();
			logger.error("Unable to reach remote server in " + downTime + " seconds - " + ex.getMessage());
		}
		if (now.getTime() - lastSuccess.getTime() > alertTime) {
			if (inError) {
				logger.error("Still down but don't send another error message");
			} else {
				try {
					logger.error("We've exceeded our limit - notify me");
					inError = true;
					notificationService.textMe("Server error", errorMsg);
				} catch (Exception ex) {
					logger.error("Unable to send message - " + ex.getMessage(), ex);
				}
			}
		}
	}
}
