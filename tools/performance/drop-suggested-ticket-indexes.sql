-- Drop the targeted candidate indexes from suggested-ticket-indexes.sql.
-- PostgreSQL requires DROP INDEX CONCURRENTLY outside an explicit transaction.

DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_lower_ticket_number;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_created_at_desc;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_owner_created_at_desc;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_status_created_at_desc;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_legacy_category_created_at_desc;
DROP INDEX CONCURRENTLY IF EXISTS idx_ticket_suggestion_terms_type_prefix;
DROP INDEX CONCURRENTLY IF EXISTS idx_ticket_suggestion_terms_type_count;

-- Optional trigram indexes, if they were manually enabled.
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_mobile_number_trgm;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_customer_name_trgm;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_ticket_number_trgm;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_product_type_trgm;
DROP INDEX CONCURRENTLY IF EXISTS idx_tickets_village_or_area_trgm;
