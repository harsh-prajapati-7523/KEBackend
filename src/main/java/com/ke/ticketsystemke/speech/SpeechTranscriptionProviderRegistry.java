package com.ke.ticketsystemke.speech;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SpeechTranscriptionProviderRegistry {

    private final Map<String, SpeechTranscriptionProvider> providersByKey;

    public SpeechTranscriptionProviderRegistry(List<SpeechTranscriptionProvider> providers) {
        this.providersByKey = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> normalize(provider.providerKey()),
                        Function.identity()
                ));
    }

    public Optional<SpeechTranscriptionProvider> find(String providerKey) {
        return Optional.ofNullable(providersByKey.get(normalize(providerKey)));
    }

    private String normalize(String providerKey) {
        return providerKey == null ? "" : providerKey.trim().toUpperCase(Locale.ROOT);
    }
}
