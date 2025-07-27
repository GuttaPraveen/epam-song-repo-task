package com.example.service;

import com.example.dto.TicketDetailsRequest;
import com.example.dto.User;
import com.example.entity.Ticket;
import com.example.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private AsyncMessageService asyncMessageService;
    
    /**
     * Main method that saves ticket and handles messaging
     * This method ensures DB save happens first, then messaging is handled asynchronously
     */
    @Transactional
    public Ticket saveTicketWithMessaging(TicketDetailsRequest ticketDetailsRequest, 
                                         User user, String formattedMessage) {
        logger.info("Starting ticket save process for ticket: {}", ticketDetailsRequest.getTicketNumber());
        
        try {
            // Step 1: Save to database first (this is the critical operation)
            Ticket savedTicket = saveTicketToDatabase(ticketDetailsRequest);
            logger.info("Ticket saved successfully to database with ID: {}", savedTicket.getId());
            
            // Step 2: Handle messaging asynchronously (non-blocking, with retry capability)
            asyncMessageService.handleTicketMessaging(
                savedTicket, 
                user, 
                formattedMessage, 
                ticketDetailsRequest.getSmsTemplate(),
                ticketDetailsRequest.getLoginUser()
            );
            
            return savedTicket;
            
        } catch (Exception e) {
            logger.error("Error in saveTicketWithMessaging for ticket: {} - {}", 
                        ticketDetailsRequest.getTicketNumber(), e.getMessage(), e);
            throw e; // This will trigger transaction rollback
        }
    }
    
    /**
     * Saves ticket to database with validation
     */
    private Ticket saveTicketToDatabase(TicketDetailsRequest request) {
        // Check if ticket already exists
        if (ticketRepository.existsByTicketNumber(request.getTicketNumber())) {
            throw new RuntimeException("Ticket with number " + request.getTicketNumber() + " already exists");
        }
        
        // Create and save ticket
        Ticket ticket = new Ticket(
            request.getTicketNumber(),
            request.getSubject(),
            request.getUserMobile(),
            request.getUserAction(),
            request.getStatus()
        );
        
        return ticketRepository.save(ticket);
    }
    
    /**
     * Gets ticket by ticket number
     */
    public Ticket getTicketByNumber(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketNumber));
    }
    
    /**
     * Updates ticket status
     */
    @Transactional
    public Ticket updateTicketStatus(String ticketNumber, String newStatus) {
        Ticket ticket = getTicketByNumber(ticketNumber);
        ticket.setStatus(newStatus);
        return ticketRepository.save(ticket);
    }
}