package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.TicketDynamicValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketDynamicValueRepository extends JpaRepository<TicketDynamicValue, Long> {

    List<TicketDynamicValue> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);
}
