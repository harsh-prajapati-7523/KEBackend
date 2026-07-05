package com.ke.ticketsystemke.speech;

public record SpeechUsageSnapshot(
        String providerKey,
        String yearMonth,
        long usedSeconds,
        long monthlyLimitSeconds,
        boolean enabled
) {
    public long remainingSeconds() {
        return Math.max(0, monthlyLimitSeconds - usedSeconds);
    }
}
