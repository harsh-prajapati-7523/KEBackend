package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.TicketSuggestionType;
import com.ke.ticketsystemke.repository.TicketSuggestionTermRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketSuggestionServiceTest {

    @Mock
    private TicketSuggestionTermRepository repository;

    @Test
    void normalizeTitleCasesWordsAndPreservesKnownAbbreviations() {
        TicketSuggestionService.NormalizedSuggestion suggestion = TicketSuggestionService.normalize("  led   tv ups ac dc mcb pcb usb cctv ro pump ");

        assertThat(suggestion.normalizedValue()).isEqualTo("led tv ups ac dc mcb pcb usb cctv ro pump");
        assertThat(suggestion.displayValue()).isEqualTo("LED TV UPS AC DC MCB PCB USB CCTV RO Pump");
    }

    @Test
    void normalizeReturnsNullForBlankValue() {
        assertThat(TicketSuggestionService.normalize("   ")).isNull();
    }

    @Test
    void recordTicketSuggestionsUpsertsProductAndVillage() {
        TicketSuggestionService service = new TicketSuggestionService(repository);
        Instant usedAt = Instant.parse("2026-07-05T10:00:00Z");

        service.recordTicketSuggestions("led tv", "new market", usedAt);

        verify(repository).upsertSuggestion(TicketSuggestionType.PRODUCT_TYPE.name(), "led tv", "LED TV", usedAt);
        verify(repository).upsertSuggestion(TicketSuggestionType.VILLAGE_OR_AREA.name(), "new market", "New Market", usedAt);
    }

    @Test
    void getSuggestionsUsesEmptyPrefixForBlankQuery() {
        TicketSuggestionService service = new TicketSuggestionService(repository);
        when(repository.findTopSuggestions(TicketSuggestionType.PRODUCT_TYPE, "")).thenReturn(List.of("Battery", "UPS"));

        assertThat(service.getSuggestions(TicketSuggestionType.PRODUCT_TYPE, "   ")).containsExactly("Battery", "UPS");
    }
}
