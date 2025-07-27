package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_queue")
public class MessageQueue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    
    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;
    
    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 3;
    
    @Column(name = "last_error")
    private String lastError;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    // Additional fields for WhatsApp
    @Column(name = "ticket_number")
    private String ticketNumber;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "user_action")
    private String userAction;
    
    @Column(name = "login_user")
    private String loginUser;
    
    // Additional fields for SMS
    @Column(name = "sms_template")
    private String smsTemplate;
    
    public enum MessageType {
        SMS, WHATSAPP
    }
    
    public enum MessageStatus {
        PENDING, PROCESSING, SENT, FAILED, EXPIRED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = MessageStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public MessageQueue() {}
    
    // Constructor for SMS
    public MessageQueue(Long ticketId, String mobileNumber, String messageContent, 
                       String smsTemplate, MessageType messageType) {
        this.ticketId = ticketId;
        this.mobileNumber = mobileNumber;
        this.messageContent = messageContent;
        this.smsTemplate = smsTemplate;
        this.messageType = messageType;
        this.status = MessageStatus.PENDING;
    }
    
    // Constructor for WhatsApp
    public MessageQueue(Long ticketId, String mobileNumber, String ticketNumber, 
                       String subject, String userAction, String loginUser, MessageType messageType) {
        this.ticketId = ticketId;
        this.mobileNumber = mobileNumber;
        this.ticketNumber = ticketNumber;
        this.subject = subject;
        this.userAction = userAction;
        this.loginUser = loginUser;
        this.messageType = messageType;
        this.status = MessageStatus.PENDING;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getMobileNumber() {
        return mobileNumber;
    }
    
    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
    
    public String getMessageContent() {
        return messageContent;
    }
    
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public MessageStatus getStatus() {
        return status;
    }
    
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }
    
    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }
    
    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
    
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
    
    public String getUserAction() {
        return userAction;
    }
    
    public void setUserAction(String userAction) {
        this.userAction = userAction;
    }
    
    public String getLoginUser() {
        return loginUser;
    }
    
    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }
    
    public String getSmsTemplate() {
        return smsTemplate;
    }
    
    public void setSmsTemplate(String smsTemplate) {
        this.smsTemplate = smsTemplate;
    }
}