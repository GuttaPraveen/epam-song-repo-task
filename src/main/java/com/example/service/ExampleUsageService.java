package com.example.service;

import com.example.dto.TicketDetailsRequest;
import com.example.dto.User;
import com.example.entity.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This service demonstrates how to replace the original problematic code:
 * 
 * Original problematic code:
 * smsStrikerGateway.sendSms(user.getMobile(), formattedMessage, smsTemplate);
 * commonFunctionUtils.sendWhatsAppMsg(ticketDetailsRequest.getTicketNumber(), 
 *                                    ticketDetailsRequest.getSubject(), 
 *                                    user.getMobile(), loginUser, 
 *                                    ticketDetailsRequest.getUserAction());
 * 
 * Problem: If SMS/WhatsApp APIs fail, the entire transaction fails and record is not saved
 * 
 * Solution: Use the resilient pattern implemented in this service
 */
@Service
public class ExampleUsageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExampleUsageService.class);
    
    @Autowired
    private TicketService ticketService;
    
    /**
     * BEFORE: Problematic approach (DON'T USE THIS)
     * This is what was causing the issue in your original code
     */
    @Deprecated
    public void problematicApproach(TicketDetailsRequest ticketDetailsRequest, User user, 
                                   String formattedMessage, String smsTemplate, 
                                   SmsStrikerGateway smsStrikerGateway, 
                                   CommonFunctionUtils commonFunctionUtils) {
        
        // This approach has problems:
        // 1. If SMS fails, WhatsApp doesn't get attempted
        // 2. If WhatsApp fails, entire transaction might rollback
        // 3. No retry mechanism
        // 4. Blocking operations
        
        try {
            // Original problematic code
            smsStrikerGateway.sendSms(user.getMobile(), formattedMessage, smsTemplate);
            commonFunctionUtils.sendWhatsAppMsg(
                ticketDetailsRequest.getTicketNumber(),
                ticketDetailsRequest.getSubject(), 
                user.getMobile(), 
                "loginUser", // This should come from actual login context
                ticketDetailsRequest.getUserAction()
            );
            
            // Save to database - PROBLEM: This happens AFTER messaging
            // If this fails, messages were already sent but record not saved
            // If messaging fails, record doesn't get saved either
            
        } catch (Exception e) {
            logger.error("Messaging failed, record not saved: {}", e.getMessage());
            throw e; // This prevents database save
        }
    }
    
    /**
     * AFTER: Resilient approach (USE THIS PATTERN)
     * This solves all the problems mentioned above
     */
    public Ticket resilientApproach(TicketDetailsRequest ticketDetailsRequest, User user, 
                                   String formattedMessage) {
        
        logger.info("Using resilient messaging pattern for ticket: {}", 
                   ticketDetailsRequest.getTicketNumber());
        
        // This approach has benefits:
        // 1. Database save happens first (guaranteed consistency)
        // 2. Messaging happens asynchronously (non-blocking)
        // 3. Failed messages are queued for retry
        // 4. Exponential backoff retry strategy
        // 5. Individual message failures don't affect other messages
        // 6. Comprehensive error handling and logging
        
        return ticketService.saveTicketWithMessaging(ticketDetailsRequest, user, formattedMessage);
    }
    
    /**
     * Example of how to integrate this into your existing controller/service
     */
    public void integrateIntoExistingCode(TicketDetailsRequest ticketDetailsRequest) {
        
        // 1. Create user object (from your authentication system)
        User user = getCurrentUser(); // Your method to get current user
        
        // 2. Format message (from your template system)
        String formattedMessage = formatMessageFromTemplate(
            ticketDetailsRequest.getTicketNumber(),
            ticketDetailsRequest.getSubject()
        );
        
        // 3. Use resilient approach instead of direct API calls
        Ticket savedTicket = resilientApproach(ticketDetailsRequest, user, formattedMessage);
        
        logger.info("Ticket processed successfully: {} - Record saved: {}, Messages queued for processing", 
                   savedTicket.getTicketNumber(), savedTicket.getId());
    }
    
    // Mock methods to demonstrate integration
    private User getCurrentUser() {
        // Replace with your actual user retrieval logic
        return new User(1L, "Current User", "user@example.com", "+1234567890");
    }
    
    private String formatMessageFromTemplate(String ticketNumber, String subject) {
        // Replace with your actual template formatting logic
        return String.format("Ticket %s created: %s", ticketNumber, subject);
    }
}