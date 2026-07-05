package com.ke.ticketsystemke.speech;

public record SpeechUnavailableResponse(
        String message,
        boolean manualTypingAvailable
) {
}
