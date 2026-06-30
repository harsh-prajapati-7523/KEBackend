package com.ke.ticketsystemke.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStatusMetadataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStatusMetadataInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public WorkflowStatusMetadataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.update("""
                INSERT INTO workflow_statuses (
                    status_key,
                    display_name,
                    active,
                    system_status,
                    protected_status,
                    terminal,
                    behavior_bucket,
                    sort_order,
                    created_at,
                    updated_at
                )
                VALUES
                    ('NEW', 'New', TRUE, TRUE, TRUE, FALSE, 'NEW', 10, now(), now()),
                    ('PICKED', 'Picked', TRUE, TRUE, TRUE, FALSE, 'PICKED', 20, now(), now()),
                    ('IN_PROGRESS', 'In Progress', TRUE, TRUE, TRUE, FALSE, 'IN_PROGRESS', 30, now(), now()),
                    ('COMPLETED', 'Completed', TRUE, TRUE, TRUE, TRUE, 'COMPLETED', 40, now(), now()),
                    ('CANCELLED', 'Cancelled', TRUE, TRUE, TRUE, TRUE, 'CANCELLED', 50, now(), now())
                ON CONFLICT (status_key) DO UPDATE
                SET
                    display_name = EXCLUDED.display_name,
                    active = TRUE,
                    system_status = TRUE,
                    protected_status = TRUE,
                    terminal = EXCLUDED.terminal,
                    behavior_bucket = EXCLUDED.behavior_bucket,
                    sort_order = EXCLUDED.sort_order,
                    updated_at = now()
                """);
        log.info("event=workflow_status_metadata_initialized");
    }
}
