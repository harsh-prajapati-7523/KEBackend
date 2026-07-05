package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.TicketSuggestionTerm;
import com.ke.ticketsystemke.entity.TicketSuggestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TicketSuggestionTermRepository extends JpaRepository<TicketSuggestionTerm, Long> {

    @Query(value = """
            SELECT display_value
            FROM ticket_suggestion_terms
            WHERE suggestion_type = :suggestionType
              AND normalized_value LIKE :normalizedPrefix || '%' ESCAPE '\\'
            ORDER BY usage_count DESC, normalized_value ASC
            LIMIT 5
            """, nativeQuery = true)
    List<String> findTopSuggestions(
            @Param("suggestionType") String suggestionType,
            @Param("normalizedPrefix") String normalizedPrefix
    );

    @Modifying
    @Query(value = """
            INSERT INTO ticket_suggestion_terms (
                suggestion_type,
                normalized_value,
                display_value,
                usage_count,
                last_used_at,
                created_at,
                updated_at
            )
            VALUES (
                :suggestionType,
                :normalizedValue,
                :displayValue,
                1,
                :usedAt,
                :usedAt,
                :usedAt
            )
            ON CONFLICT (suggestion_type, normalized_value)
            DO UPDATE SET
                display_value = EXCLUDED.display_value,
                usage_count = ticket_suggestion_terms.usage_count + 1,
                last_used_at = GREATEST(ticket_suggestion_terms.last_used_at, EXCLUDED.last_used_at),
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsertSuggestion(
            @Param("suggestionType") String suggestionType,
            @Param("normalizedValue") String normalizedValue,
            @Param("displayValue") String displayValue,
            @Param("usedAt") Instant usedAt
    );

    default List<String> findTopSuggestions(TicketSuggestionType suggestionType, String normalizedPrefix) {
        return findTopSuggestions(suggestionType.name(), normalizedPrefix);
    }
}
