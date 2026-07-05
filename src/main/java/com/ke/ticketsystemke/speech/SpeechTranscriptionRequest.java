package com.ke.ticketsystemke.speech;

public record SpeechTranscriptionRequest(
        byte[] audioBytes,
        String contentType,
        String languageCode,
        int durationSeconds
) {
}
