package com.example.service;

import com.example.entity.MessageQueue;
import com.example.repository.MessageQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageRetryScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageRetryScheduler.class);
    
    @Autowired
    private MessageQueueRepository messageQueueRepository;
    
    @Autowired
    private SmsStrikerGateway smsStrikerGateway;
    
    @Autowired
    private CommonFunctionUtils commonFunctionUtils;
    
    /**
     * Processes failed messages every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void processFailedMessages() {
        logger.info("Starting scheduled retry of failed messages");
        
        try {
            List<MessageQueue> failedMessages = messageQueueRepository.findMessagesReadyForRetry(
                MessageQueue.MessageStatus.FAILED, LocalDateTime.now());
            
            List<MessageQueue> pendingMessages = messageQueueRepository.findMessagesReadyForRetry(
                MessageQueue.MessageStatus.PENDING, LocalDateTime.now());
            
            logger.info("Found {} failed messages and {} pending messages for retry", 
                       failedMessages.size(), pendingMessages.size());
            
            // Process failed messages
            for (MessageQueue message : failedMessages) {
                processMessage(message);
            }
            
            // Process pending messages
            for (MessageQueue message : pendingMessages) {
                processMessage(message);
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled message retry process: {}", e.getMessage(), e);
        }
        
        logger.info("Scheduled retry of failed messages completed");
    }
    
    /**
     * Processes individual message with retry logic
     */
    private void processMessage(MessageQueue message) {
        logger.info("Processing message ID: {} Type: {} Mobile: {}", 
                   message.getId(), message.getMessageType(), message.getMobileNumber());
        
        try {
            message.setStatus(MessageQueue.MessageStatus.PROCESSING);
            messageQueueRepository.save(message);
            
            boolean success = false;
            
            if (message.getMessageType() == MessageQueue.MessageType.SMS) {
                success = retrySmsMessage(message);
            } else if (message.getMessageType() == MessageQueue.MessageType.WHATSAPP) {
                success = retryWhatsAppMessage(message);
            }
            
            if (success) {
                message.setStatus(MessageQueue.MessageStatus.SENT);
                logger.info("Message sent successfully - ID: {}", message.getId());
            } else {
                handleRetryFailure(message);
            }
            
            messageQueueRepository.save(message);
            
        } catch (Exception e) {
            logger.error("Error processing message ID: {} - {}", message.getId(), e.getMessage());
            message.setLastError(e.getMessage());
            handleRetryFailure(message);
            messageQueueRepository.save(message);
        }
    }
    
    /**
     * Retries SMS message
     */
    private boolean retrySmsMessage(MessageQueue message) {
        try {
            return smsStrikerGateway.sendSms(
                message.getMobileNumber(), 
                message.getMessageContent(), 
                message.getSmsTemplate()
            );
        } catch (Exception e) {
            logger.warn("SMS retry failed for message ID: {} - {}", message.getId(), e.getMessage());
            message.setLastError(e.getMessage());
            return false;
        }
    }
    
    /**
     * Retries WhatsApp message
     */
    private boolean retryWhatsAppMessage(MessageQueue message) {
        try {
            return commonFunctionUtils.sendWhatsAppMsg(
                message.getTicketNumber(),
                message.getSubject(),
                message.getMobileNumber(),
                message.getLoginUser(),
                message.getUserAction()
            );
        } catch (Exception e) {
            logger.warn("WhatsApp retry failed for message ID: {} - {}", message.getId(), e.getMessage());
            message.setLastError(e.getMessage());
            return false;
        }
    }
    
    /**
     * Handles retry failure with exponential backoff
     */
    private void handleRetryFailure(MessageQueue message) {
        message.setRetryCount(message.getRetryCount() + 1);
        
        if (message.getRetryCount() >= message.getMaxRetryCount()) {
            message.setStatus(MessageQueue.MessageStatus.EXPIRED);
            logger.warn("Message expired after {} retries - ID: {}", message.getRetryCount(), message.getId());
        } else {
            message.setStatus(MessageQueue.MessageStatus.FAILED);
            
            // Exponential backoff: 2^retryCount minutes
            long delayMinutes = (long) Math.pow(2, message.getRetryCount());
            // Cap at 24 hours
            delayMinutes = Math.min(delayMinutes, 1440);
            
            message.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            
            logger.info("Message scheduled for retry {} in {} minutes - ID: {}", 
                       message.getRetryCount() + 1, delayMinutes, message.getId());
        }
    }
    
    /**
     * Cleanup old messages (daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldMessages() {
        logger.info("Starting cleanup of old messages");
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7); // Keep for 7 days
            
            List<MessageQueue> oldSentMessages = messageQueueRepository.findOldMessages(
                MessageQueue.MessageStatus.SENT, cutoffTime);
            
            List<MessageQueue> oldExpiredMessages = messageQueueRepository.findOldMessages(
                MessageQueue.MessageStatus.EXPIRED, cutoffTime);
            
            int deletedCount = oldSentMessages.size() + oldExpiredMessages.size();
            
            messageQueueRepository.deleteAll(oldSentMessages);
            messageQueueRepository.deleteAll(oldExpiredMessages);
            
            logger.info("Cleanup completed. Deleted {} old messages", deletedCount);
            
        } catch (Exception e) {
            logger.error("Error in cleanup process: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Log queue statistics (every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logQueueStatistics() {
        try {
            long pendingCount = messageQueueRepository.countByStatusAndMessageType(
                MessageQueue.MessageStatus.PENDING, MessageQueue.MessageType.SMS) +
                messageQueueRepository.countByStatusAndMessageType(
                MessageQueue.MessageStatus.PENDING, MessageQueue.MessageType.WHATSAPP);
            
            long failedCount = messageQueueRepository.countByStatusAndMessageType(
                MessageQueue.MessageStatus.FAILED, MessageQueue.MessageType.SMS) +
                messageQueueRepository.countByStatusAndMessageType(
                MessageQueue.MessageStatus.FAILED, MessageQueue.MessageType.WHATSAPP);
            
            long processingCount = messageQueueRepository.countByStatusAndMessageType(
                MessageQueue.MessageStatus.PROCESSING, MessageQueue.MessageType.SMS) +
                messageQueueRepository.countByStatusAndMessageType(
                MessageQueue.MessageStatus.PROCESSING, MessageQueue.MessageType.WHATSAPP);
            
            logger.info("Message Queue Statistics - Pending: {}, Failed: {}, Processing: {}", 
                       pendingCount, failedCount, processingCount);
            
        } catch (Exception e) {
            logger.error("Error getting queue statistics: {}", e.getMessage());
        }
    }
}