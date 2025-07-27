package com.example.repository;

import com.example.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    
    boolean existsByTicketNumber(String ticketNumber);
}