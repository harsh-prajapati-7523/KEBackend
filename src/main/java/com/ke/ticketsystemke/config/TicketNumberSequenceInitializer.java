package com.ke.ticketsystemke.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TicketNumberSequenceInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketNumberSequenceInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketNumberSequenceInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE SEQUENCE IF NOT EXISTS ticket_number_seq
                    START WITH 1
                    INCREMENT BY 1
                """);
        jdbcTemplate.queryForObject("""
                SELECT setval(
                    'ticket_number_seq',
                    GREATEST(
                        (
                            SELECT COALESCE(MAX(SUBSTRING(ticket_number FROM '^KE-([0-9]+)$')::bigint), 0)
                            FROM tickets
                            WHERE ticket_number ~ '^KE-[0-9]+$'
                        ),
                        1
                    ),
                    (
                        SELECT COALESCE(MAX(SUBSTRING(ticket_number FROM '^KE-([0-9]+)$')::bigint), 0) > 0
                        FROM tickets
                        WHERE ticket_number ~ '^KE-[0-9]+$'
                    )
                )
                """, Long.class);
        log.info("event=ticket_number_sequence_initialized sequence=ticket_number_seq");
    }
}
