package com.example.controller;

import com.example.dto.TicketDetailsRequest;
import com.example.dto.User;
import com.example.entity.Ticket;
import com.example.service.TicketService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    
    @Autowired
    private TicketService ticketService;
    
    /**
     * Creates a new ticket with messaging (demonstrates the resilient pattern)
     * This endpoint shows how to save the record first and handle messaging failures gracefully
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTicket(
            @Valid @RequestBody TicketDetailsRequest ticketDetailsRequest) {
        
        logger.info("Received request to create ticket: {}", ticketDetailsRequest.getTicketNumber());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a mock user (in real application, this would come from authentication)
            User user = new User(1L, "John Doe", "john@example.com", ticketDetailsRequest.getUserMobile());
            
            // Format message for SMS (in real application, this might come from templates)
            String formattedMessage = String.format(
                "Your ticket %s has been created. Subject: %s. Status: %s",
                ticketDetailsRequest.getTicketNumber(),
                ticketDetailsRequest.getSubject(),
                ticketDetailsRequest.getStatus()
            );
            
            // Save ticket with messaging - this method ensures DB save happens first
            Ticket savedTicket = ticketService.saveTicketWithMessaging(
                ticketDetailsRequest, user, formattedMessage);
            
            // Return success response immediately (messaging happens asynchronously)
            response.put("success", true);
            response.put("message", "Ticket created successfully");
            response.put("ticket", savedTicket);
            response.put("note", "SMS and WhatsApp notifications are being sent asynchronously");
            
            logger.info("Ticket created successfully: {}", savedTicket.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating ticket: {} - {}", ticketDetailsRequest.getTicketNumber(), e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Failed to create ticket: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Updates ticket status with messaging
     */
    @PutMapping("/{ticketNumber}/status")
    public ResponseEntity<Map<String, Object>> updateTicketStatus(
            @PathVariable String ticketNumber,
            @RequestParam String newStatus,
            @RequestParam(required = false) String loginUser) {
        
        logger.info("Received request to update ticket status: {} to {}", ticketNumber, newStatus);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Ticket updatedTicket = ticketService.updateTicketStatus(ticketNumber, newStatus);
            
            // In a real application, you might want to send notifications for status updates too
            // You can use the same pattern here
            
            response.put("success", true);
            response.put("message", "Ticket status updated successfully");
            response.put("ticket", updatedTicket);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating ticket status: {} - {}", ticketNumber, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Failed to update ticket status: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Gets ticket by ticket number
     */
    @GetMapping("/{ticketNumber}")
    public ResponseEntity<Map<String, Object>> getTicket(@PathVariable String ticketNumber) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Ticket ticket = ticketService.getTicketByNumber(ticketNumber);
            
            response.put("success", true);
            response.put("ticket", ticket);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting ticket: {} - {}", ticketNumber, e.getMessage());
            
            response.put("success", false);
            response.put("message", "Ticket not found: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Messaging Service");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}