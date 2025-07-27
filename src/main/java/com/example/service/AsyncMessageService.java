package com.example.service;

import com.example.dto.User;
import com.example.entity.MessageQueue;
import com.example.entity.Ticket;
import com.example.repository.MessageQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AsyncMessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageService.class);
    
    @Autowired
    private SmsStrikerGateway smsStrikerGateway;
    
    @Autowired
    private CommonFunctionUtils commonFunctionUtils;
    
    @Autowired
    private MessageQueueRepository messageQueueRepository;
    
    /**
     * Handles both SMS and WhatsApp messaging asynchronously
     * If immediate send fails, messages are queued for retry
     */
    @Async
    public void handleTicketMessaging(Ticket ticket, User user, String formattedMessage, 
                                     String smsTemplate, String loginUser) {
        logger.info("Starting async messaging for ticket: {}", ticket.getTicketNumber());
        
        // Try to send SMS immediately
        handleSmsMessaging(ticket, user, formattedMessage, smsTemplate);
        
        // Try to send WhatsApp message immediately
        handleWhatsAppMessaging(ticket, user, loginUser);
        
        logger.info("Async messaging processing completed for ticket: {}", ticket.getTicketNumber());
    }
    
    /**
     * Handles SMS messaging with immediate attempt and queuing on failure
     */
    private void handleSmsMessaging(Ticket ticket, User user, String formattedMessage, String smsTemplate) {
        try {
            // Try immediate send (no retry at this level)
            boolean smsSent = smsStrikerGateway.sendSmsImmediateSync(
                user.getMobile(), formattedMessage, smsTemplate);
            
            if (smsSent) {
                logger.info("SMS sent immediately for ticket: {}", ticket.getTicketNumber());
            } else {
                // Queue for retry
                queueSmsForRetry(ticket, user.getMobile(), formattedMessage, smsTemplate);
            }
            
        } catch (Exception e) {
            logger.warn("SMS immediate send failed for ticket: {} - {}", 
                       ticket.getTicketNumber(), e.getMessage());
            // Queue for retry
            queueSmsForRetry(ticket, user.getMobile(), formattedMessage, smsTemplate);
        }
    }
    
    /**
     * Handles WhatsApp messaging with immediate attempt and queuing on failure
     */
    private void handleWhatsAppMessaging(Ticket ticket, User user, String loginUser) {
        try {
            // Try immediate send (no retry at this level)
            boolean whatsappSent = commonFunctionUtils.sendWhatsAppMsgImmediateSync(
                ticket.getTicketNumber(), ticket.getSubject(), user.getMobile(), 
                loginUser, ticket.getUserAction());
            
            if (whatsappSent) {
                logger.info("WhatsApp message sent immediately for ticket: {}", ticket.getTicketNumber());
            } else {
                // Queue for retry
                queueWhatsAppForRetry(ticket, user.getMobile(), loginUser);
            }
            
        } catch (Exception e) {
            logger.warn("WhatsApp immediate send failed for ticket: {} - {}", 
                       ticket.getTicketNumber(), e.getMessage());
            // Queue for retry
            queueWhatsAppForRetry(ticket, user.getMobile(), loginUser);
        }
    }
    
    /**
     * Queues SMS message for retry
     */
    @Transactional
    private void queueSmsForRetry(Ticket ticket, String mobileNumber, 
                                 String formattedMessage, String smsTemplate) {
        logger.info("Queuing SMS for retry - Ticket: {}, Mobile: {}", 
                   ticket.getTicketNumber(), mobileNumber);
        
        MessageQueue messageQueue = new MessageQueue(
            ticket.getId(),
            mobileNumber,
            formattedMessage,
            smsTemplate,
            MessageQueue.MessageType.SMS
        );
        
        messageQueue.setNextRetryAt(LocalDateTime.now().plusMinutes(1)); // Retry in 1 minute
        messageQueueRepository.save(messageQueue);
        
        logger.info("SMS queued for retry with ID: {}", messageQueue.getId());
    }
    
    /**
     * Queues WhatsApp message for retry
     */
    @Transactional
    private void queueWhatsAppForRetry(Ticket ticket, String mobileNumber, String loginUser) {
        logger.info("Queuing WhatsApp for retry - Ticket: {}, Mobile: {}", 
                   ticket.getTicketNumber(), mobileNumber);
        
        MessageQueue messageQueue = new MessageQueue(
            ticket.getId(),
            mobileNumber,
            ticket.getTicketNumber(),
            ticket.getSubject(),
            ticket.getUserAction(),
            loginUser,
            MessageQueue.MessageType.WHATSAPP
        );
        
        messageQueue.setNextRetryAt(LocalDateTime.now().plusMinutes(1)); // Retry in 1 minute
        messageQueueRepository.save(messageQueue);
        
        logger.info("WhatsApp queued for retry with ID: {}", messageQueue.getId());
    }
}