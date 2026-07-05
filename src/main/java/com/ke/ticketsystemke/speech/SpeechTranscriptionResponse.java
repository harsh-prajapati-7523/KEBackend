package com.ke.ticketsystemke.speech;

public record SpeechTranscriptionResponse(
        String text,
        String languageCode,
        String providerKey,
        long usedSecondsThisMonth,
        long monthlyLimitSeconds,
        long remainingSecondsThisMonth
) {
}
