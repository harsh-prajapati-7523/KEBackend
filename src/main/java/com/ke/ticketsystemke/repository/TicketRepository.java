package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository
        extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    Long getNextTicketNumberValue();

    List<Ticket> findAllByOrderByCreatedAtDesc();

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

    @Query(value = """
            SELECT MIN(BTRIM(manufacturer_or_brand_name))
            FROM tickets
            WHERE manufacturer_or_brand_name IS NOT NULL
              AND BTRIM(manufacturer_or_brand_name) <> ''
              AND LOWER(BTRIM(manufacturer_or_brand_name)) LIKE LOWER(:query) || '%' ESCAPE '\\'
            GROUP BY LOWER(BTRIM(manufacturer_or_brand_name))
            ORDER BY COUNT(*) DESC, LOWER(MIN(BTRIM(manufacturer_or_brand_name))) ASC, MIN(BTRIM(manufacturer_or_brand_name)) ASC
            LIMIT 5
            """, nativeQuery = true)
    List<String> findManufacturerSuggestions(@Param("query") String query);
}
