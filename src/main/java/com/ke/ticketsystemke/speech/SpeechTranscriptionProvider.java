package com.ke.ticketsystemke.speech;

public interface SpeechTranscriptionProvider {

    String providerKey();

    boolean isEnabled();

    SpeechTranscriptionResult transcribe(SpeechTranscriptionRequest request);
}
