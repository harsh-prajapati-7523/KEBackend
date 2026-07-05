package com.ke.ticketsystemke.speech;

import com.ke.ticketsystemke.config.SpeechTranscriptionProperties;
import org.springframework.stereotype.Component;

@Component
public class OpenAiSpeechTranscriptionProvider implements SpeechTranscriptionProvider {

    public static final String KEY = "OPENAI_STT";

    private final SpeechTranscriptionProperties properties;

    public OpenAiSpeechTranscriptionProvider(SpeechTranscriptionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerKey() {
        return KEY;
    }

    @Override
    public boolean isEnabled() {
        return properties.getOpenai().isEnabled();
    }

    @Override
    public SpeechTranscriptionResult transcribe(SpeechTranscriptionRequest request) {
        throw new SpeechProviderException("OpenAI speech transcription provider is not implemented");
    }
}
