package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.Ticket;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository
        extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    Long getNextTicketNumberValue();

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Ticket> findByPickedByEmployeeIdOrderByCreatedAtDesc(String pickedByEmployeeId, Pageable pageable);

    Optional<Ticket> findFirstByTicketNumberIgnoreCase(String ticketNumber);

    Optional<Ticket> findFirstByMobileNumberOrderByCreatedAtDesc(String mobileNumber);

    List<Ticket> findTop10ByMobileNumberAndIdNotOrderByCreatedAtDesc(String mobileNumber, Long id);

    @Query(value = """
            SELECT *
            FROM tickets
            WHERE LOWER(ticket_number) LIKE '%' || LOWER(:query) || '%' ESCAPE '\\'
               OR LOWER(mobile_number) LIKE '%' || LOWER(:query) || '%' ESCAPE '\\'
               OR LOWER(customer_name) LIKE '%' || LOWER(:query) || '%' ESCAPE '\\'
               OR LOWER(product_type) LIKE '%' || LOWER(:query) || '%' ESCAPE '\\'
               OR LOWER(village_or_area) LIKE '%' || LOWER(:query) || '%' ESCAPE '\\'
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Ticket> searchTickets(
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT MIN(BTRIM(product_type))
            FROM tickets
            WHERE product_type IS NOT NULL
              AND BTRIM(product_type) <> ''
              AND LOWER(BTRIM(product_type)) LIKE LOWER(:query) || '%' ESCAPE '\\'
            GROUP BY LOWER(BTRIM(product_type))
            ORDER BY COUNT(*) DESC, LOWER(MIN(BTRIM(product_type))) ASC, MIN(BTRIM(product_type)) ASC
            LIMIT 5
            """, nativeQuery = true)
    List<String> findProductTypeSuggestions(@Param("query") String query);

    @Query(value = """
            SELECT MIN(BTRIM(village_or_area))
            FROM tickets
            WHERE village_or_area IS NOT NULL
              AND BTRIM(village_or_area) <> ''
              AND LOWER(BTRIM(village_or_area)) LIKE LOWER(:query) || '%' ESCAPE '\\'
            GROUP BY LOWER(BTRIM(village_or_area))
            ORDER BY COUNT(*) DESC, LOWER(MIN(BTRIM(village_or_area))) ASC, MIN(BTRIM(village_or_area)) ASC
            LIMIT 5
            """, nativeQuery = true)
    List<String> findVillageSuggestions(@Param("query") String query);

}
