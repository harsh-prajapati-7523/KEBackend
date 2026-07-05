package com.ke.ticketsystemke.speech;

import com.ke.ticketsystemke.config.SpeechTranscriptionProperties;
import org.springframework.stereotype.Component;

@Component
public class AzureSpeechTranscriptionProvider implements SpeechTranscriptionProvider {

    public static final String KEY = "AZURE_STT";

    private final SpeechTranscriptionProperties properties;

    public AzureSpeechTranscriptionProvider(SpeechTranscriptionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerKey() {
        return KEY;
    }

    @Override
    public boolean isEnabled() {
        return properties.getAzure().isEnabled();
    }

    @Override
    public SpeechTranscriptionResult transcribe(SpeechTranscriptionRequest request) {
        throw new SpeechProviderException("Azure speech transcription provider is not implemented");
    }
}
