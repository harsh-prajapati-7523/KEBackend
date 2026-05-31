package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.TicketChargeItem;
import com.ke.ticketsystemke.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TicketChargeItemRepository extends JpaRepository<TicketChargeItem, Long> {

    List<TicketChargeItem> findAllByTicketAndDeletedAtIsNullOrderByCreatedAtAsc(Ticket ticket);

    Optional<TicketChargeItem> findByIdAndTicketAndDeletedAtIsNull(Long id, Ticket ticket);

    @Query("SELECT COALESCE(SUM(item.amount), 0) FROM TicketChargeItem item WHERE item.ticket = :ticket AND item.deletedAt IS NULL")
    BigDecimal sumAmountByTicket(@Param("ticket") Ticket ticket);
}
