package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.TicketChargeItem;
import com.ke.ticketsystemke.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketChargeItemRepository extends JpaRepository<TicketChargeItem, Long> {

    List<TicketChargeItem> findAllByTicketAndDeletedAtIsNullOrderByCreatedAtAsc(Ticket ticket);

    Optional<TicketChargeItem> findByIdAndTicketAndDeletedAtIsNull(Long id, Ticket ticket);

    @Query("SELECT COALESCE(SUM(item.amount), 0) FROM TicketChargeItem item WHERE item.ticket = :ticket AND item.deletedAt IS NULL")
    BigDecimal sumAmountByTicket(@Param("ticket") Ticket ticket);

    @Query("""
            SELECT item.ticket.id AS ticketId, COALESCE(SUM(item.amount), 0) AS totalAmount
            FROM TicketChargeItem item
            WHERE item.ticket.id IN :ticketIds
              AND item.deletedAt IS NULL
            GROUP BY item.ticket.id
            """)
    List<TicketChargeTotalProjection> sumAmountByTicketIds(@Param("ticketIds") Collection<Long> ticketIds);

    @Query(value = """
            SELECT MIN(BTRIM(description))
            FROM ticket_charge_items
            WHERE deleted_at IS NULL
              AND description IS NOT NULL
              AND BTRIM(description) <> ''
              AND LOWER(BTRIM(description)) LIKE LOWER(:query) || '%' ESCAPE '\\'
            GROUP BY LOWER(BTRIM(description))
            ORDER BY COUNT(*) DESC, LOWER(MIN(BTRIM(description))) ASC, MIN(BTRIM(description)) ASC
            LIMIT 5
            """, nativeQuery = true)
    List<String> findDescriptionSuggestions(@Param("query") String query);

    interface TicketChargeTotalProjection {

        Long getTicketId();

        BigDecimal getTotalAmount();
    }
}
