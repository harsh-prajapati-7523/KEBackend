package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.repository.TicketChargeItemRepository;
import com.ke.ticketsystemke.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Service
public class SuggestionService {

    private static final int MAX_QUERY_LENGTH = 120;

    private final TicketRepository ticketRepository;
    private final TicketChargeItemRepository chargeItemRepository;

    public SuggestionService(
            TicketRepository ticketRepository,
            TicketChargeItemRepository chargeItemRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.chargeItemRepository = chargeItemRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getProductTypes(String query) {
        return getSuggestions(query, ticketRepository::findProductTypeSuggestions);
    }

    @Transactional(readOnly = true)
    public List<String> getVillages(String query) {
        return getSuggestions(query, ticketRepository::findVillageSuggestions);
    }

    @Transactional(readOnly = true)
    public List<String> getChargeDescriptions(String query) {
        return getSuggestions(query, chargeItemRepository::findDescriptionSuggestions);
    }

    private List<String> getSuggestions(
            String query,
            Function<String, List<String>> suggestionFinder
    ) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery == null) {
            return Collections.emptyList();
        }

        return suggestionFinder.apply(escapeLikeWildcards(normalizedQuery));
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }

        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty() || trimmedQuery.length() > MAX_QUERY_LENGTH) {
            return null;
        }

        return trimmedQuery;
    }

    private String escapeLikeWildcards(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
