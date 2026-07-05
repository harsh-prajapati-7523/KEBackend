-- Targeted candidate indexes for the 100k ticket performance test.
--
-- Scope:
--   1. Open Ticket
--   2. Smart Suggestions for Product Type and Village / Area
--   3. Find Tickets
--   4. My Tickets
--
-- Apply these in a disposable test database first, then compare API/browser timings.
-- PostgreSQL requires CREATE INDEX CONCURRENTLY outside an explicit transaction.

-- ---------------------------------------------------------------------------
-- Apply first: low-risk btree/expression indexes that match current queries.
-- ---------------------------------------------------------------------------

-- Open Ticket:
-- TicketService#getTicketByNumber uses findFirstByTicketNumberIgnoreCase,
-- which resolves to a lower(ticket_number) equality check. The existing unique
-- ticket_number index is case-sensitive, so it may not help this query.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_lower_ticket_number
    ON tickets (lower(ticket_number));

-- Find Tickets default list:
-- repository.findAllByOrderByCreatedAtDesc(pageable)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_created_at_desc
    ON tickets (created_at DESC);

-- My Tickets:
-- repository.findByPickedByEmployeeIdOrderByCreatedAtDesc(employeeId, pageable)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_owner_created_at_desc
    ON tickets (picked_by_employee_id, created_at DESC)
    WHERE picked_by_employee_id IS NOT NULL;

-- Find Tickets status filter:
-- TicketSpecifications filters on Ticket.status, then pageable sorts by createdAt DESC.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_status_created_at_desc
    ON tickets (status, created_at DESC);

-- Find Tickets legacy enum category filter:
-- The current TicketSpecifications code filters the legacy enum column
-- `category`, not `category_id`. Do not use category_id for this query unless
-- the backend filter is changed to categoryRecord/category_id.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_legacy_category_created_at_desc
    ON tickets (category, created_at DESC)
    WHERE category IS NOT NULL;

-- Smart Suggestions:
-- Product Type and Village / Area suggestions now read from the precomputed
-- ticket_suggestion_terms table, not from tickets. The backfill script also
-- creates these indexes, but they are repeated here for isolated performance
-- test databases.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_suggestion_terms_type_prefix
    ON ticket_suggestion_terms (suggestion_type, normalized_value text_pattern_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ticket_suggestion_terms_type_count
    ON ticket_suggestion_terms (suggestion_type, usage_count DESC, normalized_value);

-- ---------------------------------------------------------------------------
-- Optional: apply only if Find Tickets free-text search is proven slow.
-- ---------------------------------------------------------------------------
--
-- Current Find Tickets search uses contains matching:
--   LOWER(field) LIKE '%' || LOWER(:query) || '%'
-- across ticket_number, mobile_number, customer_name, product_type, village.
--
-- Trigram indexes can help contains searches, but they are write-heavy and can
-- produce many index rechecks for numeric/mobile substrings. Do not apply all
-- five by default. Start with the fields users actually search most.
--
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
--
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_mobile_number_trgm
--     ON tickets USING gin (lower(mobile_number) gin_trgm_ops);
--
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_customer_name_trgm
--     ON tickets USING gin (lower(customer_name) gin_trgm_ops);
--
-- Optional only if real user behavior shows these searches matter:
--
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_ticket_number_trgm
--     ON tickets USING gin (lower(ticket_number) gin_trgm_ops);
--
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_product_type_trgm
--     ON tickets USING gin (lower(product_type) gin_trgm_ops);
--
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tickets_village_or_area_trgm
--     ON tickets USING gin (lower(village_or_area) gin_trgm_ops);
