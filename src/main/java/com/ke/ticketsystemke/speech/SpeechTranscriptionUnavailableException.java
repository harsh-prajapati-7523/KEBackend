package com.ke.ticketsystemke.speech;

public class SpeechTranscriptionUnavailableException extends RuntimeException {

    private final SpeechTranscriptionFailureReason reason;

    public SpeechTranscriptionUnavailableException(SpeechTranscriptionFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public SpeechTranscriptionFailureReason getReason() {
        return reason;
    }
}
