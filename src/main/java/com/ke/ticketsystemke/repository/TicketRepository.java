package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository
        extends JpaRepository<Ticket, Long> {
}