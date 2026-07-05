package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.TicketSuggestionType;
import com.ke.ticketsystemke.repository.TicketSuggestionTermRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TicketSuggestionService {

    private static final int MAX_VALUE_LENGTH = 255;
    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("ups", "UPS"),
            Map.entry("led", "LED"),
            Map.entry("tv", "TV"),
            Map.entry("ac", "AC"),
            Map.entry("dc", "DC"),
            Map.entry("mcb", "MCB"),
            Map.entry("pcb", "PCB"),
            Map.entry("usb", "USB"),
            Map.entry("cctv", "CCTV"),
            Map.entry("ro", "RO")
    );

    private final TicketSuggestionTermRepository repository;

    public TicketSuggestionService(TicketSuggestionTermRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<String> getSuggestions(TicketSuggestionType suggestionType, String query) {
        String normalizedQuery = normalizeSuggestionQuery(query);
        if (normalizedQuery == null) {
            return List.of();
        }

        return repository.findTopSuggestions(
                suggestionType,
                escapeLikeWildcards(normalizedQuery)
        );
    }

    @Transactional
    public void recordTicketSuggestions(String productType, String villageOrArea, Instant usedAt) {
        upsertSuggestion(TicketSuggestionType.PRODUCT_TYPE, productType, usedAt);
        upsertSuggestion(TicketSuggestionType.VILLAGE_OR_AREA, villageOrArea, usedAt);
    }

    private void upsertSuggestion(TicketSuggestionType suggestionType, String rawValue, Instant usedAt) {
        NormalizedSuggestion normalizedSuggestion = normalize(rawValue);
        if (normalizedSuggestion == null) {
            return;
        }

        repository.upsertSuggestion(
                suggestionType.name(),
                normalizedSuggestion.normalizedValue(),
                normalizedSuggestion.displayValue(),
                usedAt == null ? Instant.now() : usedAt
        );
    }

    static NormalizedSuggestion normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String collapsed = rawValue.trim().replaceAll("\\s+", " ");
        if (collapsed.isBlank()) {
            return null;
        }

        String normalizedValue = collapsed.toLowerCase(Locale.ROOT);
        String displayValue = toDisplayValue(normalizedValue);
        if (normalizedValue.length() > MAX_VALUE_LENGTH || displayValue.length() > MAX_VALUE_LENGTH) {
            return null;
        }
        return new NormalizedSuggestion(normalizedValue, displayValue);
    }

    private String normalizeSuggestionQuery(String query) {
        if (query == null) {
            return "";
        }

        String collapsed = query.trim().replaceAll("\\s+", " ");
        if (collapsed.length() > MAX_VALUE_LENGTH) {
            return null;
        }
        return collapsed.toLowerCase(Locale.ROOT);
    }

    private static String toDisplayValue(String normalizedValue) {
        String[] words = normalizedValue.split(" ");
        for (int index = 0; index < words.length; index += 1) {
            String abbreviation = ABBREVIATIONS.get(words[index]);
            if (abbreviation != null) {
                words[index] = abbreviation;
            } else {
                words[index] = words[index].substring(0, 1).toUpperCase(Locale.ROOT) + words[index].substring(1);
            }
        }
        return String.join(" ", words);
    }

    private String escapeLikeWildcards(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    record NormalizedSuggestion(String normalizedValue, String displayValue) {
    }
}
