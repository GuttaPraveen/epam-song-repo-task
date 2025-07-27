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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class SmsStrikerGateway {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsStrikerGateway.class);
    
    @Value("${messaging.sms.api-url}")
    private String smsApiUrl;
    
    @Value("${messaging.sms.api-key}")
    private String smsApiKey;
    
    @Value("${messaging.sms.timeout:30000}")
    private int timeout;
    
    private final RestTemplate restTemplate;
    
    public SmsStrikerGateway() {
        this.restTemplate = new RestTemplate();
        // Set timeout
        this.restTemplate.getRequestFactory();
    }
    
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean sendSms(String mobileNumber, String message, String template) {
        logger.info("Attempting to send SMS to: {} with template: {}", mobileNumber, template);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + smsApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", mobileNumber);
            requestBody.put("message", message);
            requestBody.put("template", template);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(smsApiUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS sent successfully to: {}", mobileNumber);
                return true;
            } else {
                logger.error("SMS API returned error status: {} for mobile: {}", 
                           response.getStatusCode(), mobileNumber);
                throw new RuntimeException("SMS API returned error: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error sending SMS to: {} - {}", mobileNumber, e.getMessage());
            throw e;
        }
    }
    
    @Recover
    public boolean recoverSendSms(Exception ex, String mobileNumber, String message, String template) {
        logger.error("All SMS retry attempts failed for mobile: {} - {}", mobileNumber, ex.getMessage());
        // This will be handled by the async message queue service
        return false;
    }
    
    // Synchronous method that doesn't use retry (for immediate feedback)
    public boolean sendSmsImmediateSync(String mobileNumber, String message, String template) {
        logger.info("Sending SMS synchronously to: {}", mobileNumber);
        
        try {
            return sendSmsWithoutRetry(mobileNumber, message, template);
        } catch (Exception e) {
            logger.warn("Immediate SMS send failed, will be queued for retry: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean sendSmsWithoutRetry(String mobileNumber, String message, String template) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + smsApiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("to", mobileNumber);
        requestBody.put("message", message);
        requestBody.put("template", template);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(smsApiUrl, request, String.class);
        
        return response.getStatusCode().is2xxSuccessful();
    }
}