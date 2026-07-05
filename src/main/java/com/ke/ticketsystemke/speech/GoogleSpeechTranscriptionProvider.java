package com.ke.ticketsystemke.speech;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.protobuf.ByteString;
import com.ke.ticketsystemke.config.SpeechTranscriptionProperties;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Locale;

@Component
public class GoogleSpeechTranscriptionProvider implements SpeechTranscriptionProvider {

    public static final String KEY = "GOOGLE_STT";

    private final SpeechTranscriptionProperties properties;

    public GoogleSpeechTranscriptionProvider(SpeechTranscriptionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String providerKey() {
        return KEY;
    }

    @Override
    public boolean isEnabled() {
        return properties.getGoogle().isEnabled();
    }

    @Override
    public SpeechTranscriptionResult transcribe(SpeechTranscriptionRequest request) {
        try (SpeechClient speechClient = SpeechClient.create()) {
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(toGoogleEncoding(request.contentType()))
                    .setLanguageCode(request.languageCode())
                    .setEnableAutomaticPunctuation(true)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(ByteString.copyFrom(request.audioBytes()))
                    .build();
            RecognizeResponse response = speechClient.recognize(config, audio);

            String text = response.getResultsList().stream()
                    .flatMap(result -> result.getAlternativesList().stream())
                    .max(Comparator.comparing(SpeechRecognitionAlternative::getConfidence))
                    .map(SpeechRecognitionAlternative::getTranscript)
                    .map(String::trim)
                    .orElse("");

            if (text.isBlank()) {
                throw new SpeechProviderException("Google returned empty transcript");
            }
            return new SpeechTranscriptionResult(text, request.languageCode(), providerKey());
        } catch (SpeechProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SpeechProviderException("Google speech transcription failed", ex);
        }
    }

    private RecognitionConfig.AudioEncoding toGoogleEncoding(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("webm")) {
            return RecognitionConfig.AudioEncoding.WEBM_OPUS;
        }
        if (normalized.contains("ogg")) {
            return RecognitionConfig.AudioEncoding.OGG_OPUS;
        }
        if (normalized.contains("mpeg") || normalized.contains("mp3")) {
            return RecognitionConfig.AudioEncoding.MP3;
        }
        if (normalized.contains("wav") || normalized.contains("wave")) {
            return RecognitionConfig.AudioEncoding.LINEAR16;
        }
        return RecognitionConfig.AudioEncoding.ENCODING_UNSPECIFIED;
    }
}
