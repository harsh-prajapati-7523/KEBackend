package com.ke.ticketsystemke.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkflowStatusMetadataInitializerTest {

    @Test
    void upsertsProtectedSystemWorkflowStatuses() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WorkflowStatusMetadataInitializer initializer = new WorkflowStatusMetadataInitializer(jdbcTemplate);

        initializer.run(null);

        verify(jdbcTemplate).update(contains("INSERT INTO workflow_statuses"));
        verify(jdbcTemplate).update(contains("'NEW', 'New', TRUE, TRUE, TRUE, FALSE, 'NEW'"));
        verify(jdbcTemplate).update(contains("'COMPLETED', 'Completed', TRUE, TRUE, TRUE, TRUE, 'COMPLETED'"));
        verify(jdbcTemplate).update(contains("ON CONFLICT (status_key) DO UPDATE"));
    }
}
