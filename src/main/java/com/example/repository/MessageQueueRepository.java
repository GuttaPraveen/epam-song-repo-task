package com.example.repository;

import com.example.entity.MessageQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueue, Long> {
    
    List<MessageQueue> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
            MessageQueue.MessageStatus status, Integer maxRetryCount);
    
    @Query("SELECT mq FROM MessageQueue mq WHERE mq.status = :status " +
           "AND mq.retryCount < mq.maxRetryCount " +
           "AND (mq.nextRetryAt IS NULL OR mq.nextRetryAt <= :currentTime)")
    List<MessageQueue> findMessagesReadyForRetry(
            @Param("status") MessageQueue.MessageStatus status,
            @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT mq FROM MessageQueue mq WHERE mq.status = :status " +
           "AND mq.createdAt < :cutoffTime")
    List<MessageQueue> findOldMessages(
            @Param("status") MessageQueue.MessageStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime);
    
    List<MessageQueue> findByTicketIdAndStatus(Long ticketId, MessageQueue.MessageStatus status);
    
    long countByStatusAndMessageType(MessageQueue.MessageStatus status, MessageQueue.MessageType messageType);
}