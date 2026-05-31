package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository
        extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    Long getNextTicketNumberValue();

    List<Ticket> findAllByOrderByCreatedAtDesc();
}
