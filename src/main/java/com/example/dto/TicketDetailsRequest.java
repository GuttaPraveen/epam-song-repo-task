package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TicketDetailsRequest {
    
    @NotBlank(message = "Ticket number is required")
    private String ticketNumber;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "User mobile is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{10,15}$", message = "Invalid mobile number format")
    private String userMobile;
    
    private String userAction;
    
    private String status = "CREATED";
    
    private String smsMessage;
    
    private String smsTemplate;
    
    private String loginUser;
    
    // Constructors
    public TicketDetailsRequest() {}
    
    public TicketDetailsRequest(String ticketNumber, String subject, String userMobile, 
                               String userAction, String smsMessage, String smsTemplate, String loginUser) {
        this.ticketNumber = ticketNumber;
        this.subject = subject;
        this.userMobile = userMobile;
        this.userAction = userAction;
        this.smsMessage = smsMessage;
        this.smsTemplate = smsTemplate;
        this.loginUser = loginUser;
    }
    
    // Getters and Setters
    public String getTicketNumber() {
        return ticketNumber;
    }
    
    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getUserMobile() {
        return userMobile;
    }
    
    public void setUserMobile(String userMobile) {
        this.userMobile = userMobile;
    }
    
    public String getUserAction() {
        return userAction;
    }
    
    public void setUserAction(String userAction) {
        this.userAction = userAction;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSmsMessage() {
        return smsMessage;
    }
    
    public void setSmsMessage(String smsMessage) {
        this.smsMessage = smsMessage;
    }
    
    public String getSmsTemplate() {
        return smsTemplate;
    }
    
    public void setSmsTemplate(String smsTemplate) {
        this.smsTemplate = smsTemplate;
    }
    
    public String getLoginUser() {
        return loginUser;
    }
    
    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }
}