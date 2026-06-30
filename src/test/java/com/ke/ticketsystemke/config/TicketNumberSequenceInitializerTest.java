package com.ke.ticketsystemke.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketNumberSequenceInitializerTest {

    @Test
    void createsAndSynchronizesTicketNumberSequence() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        TicketNumberSequenceInitializer initializer = new TicketNumberSequenceInitializer(jdbcTemplate);

        initializer.run(null);

        verify(jdbcTemplate).execute(startsWith("CREATE SEQUENCE IF NOT EXISTS ticket_number_seq"));
        verify(jdbcTemplate).queryForObject(startsWith("SELECT setval("), eq(Long.class));
    }
}
