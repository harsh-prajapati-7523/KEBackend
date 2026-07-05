package com.ke.ticketsystemke.speech;

public record SpeechTranscriptionResult(
        String text,
        String languageCode,
        String providerKey
) {
}
