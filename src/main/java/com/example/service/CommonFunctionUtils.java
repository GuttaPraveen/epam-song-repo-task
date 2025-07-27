package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommonFunctionUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(CommonFunctionUtils.class);
    
    @Value("${messaging.whatsapp.api-url}")
    private String whatsappApiUrl;
    
    @Value("${messaging.whatsapp.api-key}")
    private String whatsappApiKey;
    
    @Value("${messaging.whatsapp.timeout:30000}")
    private int timeout;
    
    private final RestTemplate restTemplate;
    
    public CommonFunctionUtils() {
        this.restTemplate = new RestTemplate();
    }
    
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean sendWhatsAppMsg(String ticketNumber, String subject, String mobileNumber, 
                                  String loginUser, String userAction) {
        logger.info("Attempting to send WhatsApp message to: {} for ticket: {}", 
                   mobileNumber, ticketNumber);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + whatsappApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", mobileNumber);
            requestBody.put("ticketNumber", ticketNumber);
            requestBody.put("subject", subject);
            requestBody.put("loginUser", loginUser);
            requestBody.put("userAction", userAction);
            
            // Create formatted message
            String message = String.format(
                "Ticket Update\nTicket: %s\nSubject: %s\nAction: %s\nProcessed by: %s",
                ticketNumber, subject, userAction, loginUser
            );
            requestBody.put("message", message);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(whatsappApiUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("WhatsApp message sent successfully to: {} for ticket: {}", 
                           mobileNumber, ticketNumber);
                return true;
            } else {
                logger.error("WhatsApp API returned error status: {} for mobile: {} ticket: {}", 
                           response.getStatusCode(), mobileNumber, ticketNumber);
                throw new RuntimeException("WhatsApp API returned error: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error sending WhatsApp message to: {} for ticket: {} - {}", 
                        mobileNumber, ticketNumber, e.getMessage());
            throw e;
        }
    }
    
    @Recover
    public boolean recoverSendWhatsAppMsg(Exception ex, String ticketNumber, String subject, 
                                         String mobileNumber, String loginUser, String userAction) {
        logger.error("All WhatsApp retry attempts failed for mobile: {} ticket: {} - {}", 
                    mobileNumber, ticketNumber, ex.getMessage());
        // This will be handled by the async message queue service
        return false;
    }
    
    // Synchronous method that doesn't use retry (for immediate feedback)
    public boolean sendWhatsAppMsgImmediateSync(String ticketNumber, String subject, 
                                               String mobileNumber, String loginUser, String userAction) {
        logger.info("Sending WhatsApp message synchronously to: {} for ticket: {}", 
                   mobileNumber, ticketNumber);
        
        try {
            return sendWhatsAppMsgWithoutRetry(ticketNumber, subject, mobileNumber, loginUser, userAction);
        } catch (Exception e) {
            logger.warn("Immediate WhatsApp send failed, will be queued for retry: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean sendWhatsAppMsgWithoutRetry(String ticketNumber, String subject, 
                                               String mobileNumber, String loginUser, String userAction) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + whatsappApiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("to", mobileNumber);
        requestBody.put("ticketNumber", ticketNumber);
        requestBody.put("subject", subject);
        requestBody.put("loginUser", loginUser);
        requestBody.put("userAction", userAction);
        
        String message = String.format(
            "Ticket Update\nTicket: %s\nSubject: %s\nAction: %s\nProcessed by: %s",
            ticketNumber, subject, userAction, loginUser
        );
        requestBody.put("message", message);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(whatsappApiUrl, request, String.class);
        
        return response.getStatusCode().is2xxSuccessful();
    }
}