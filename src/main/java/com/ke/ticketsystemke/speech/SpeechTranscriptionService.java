package com.ke.ticketsystemke.speech;

import com.ke.ticketsystemke.config.SpeechTranscriptionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class SpeechTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SpeechTranscriptionService.class);
    private static final String UNAVAILABLE_MESSAGE = "Voice typing is temporarily unavailable or monthly limit is reached. Please type manually.";
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "audio/webm",
            "audio/wav",
            "audio/wave",
            "audio/x-wav",
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/ogg"
    );

    private final SpeechTranscriptionProperties properties;
    private final SpeechTranscriptionProviderRegistry providerRegistry;
    private final SpeechUsageMonthlyService usageMonthlyService;

    public SpeechTranscriptionService(
            SpeechTranscriptionProperties properties,
            SpeechTranscriptionProviderRegistry providerRegistry,
            SpeechUsageMonthlyService usageMonthlyService
    ) {
        this.properties = properties;
        this.providerRegistry = providerRegistry;
        this.usageMonthlyService = usageMonthlyService;
    }

    public synchronized SpeechTranscriptionResponse transcribe(MultipartFile audio, Integer durationSeconds) {
        if (!properties.isEnabled()) {
            throw unavailable(SpeechTranscriptionFailureReason.UNAVAILABLE);
        }

        ValidatedAudio validatedAudio = validate(audio, durationSeconds);
        boolean foundUsableProvider = false;
        boolean blockedByLimit = false;

        for (String providerKey : properties.getProviderPriority()) {
            String normalizedProviderKey = normalizeProviderKey(providerKey);
            SpeechTranscriptionProvider provider = providerRegistry.find(normalizedProviderKey).orElse(null);
            if (provider == null) {
                log.info("event=speech_provider_skipped reason=not_registered providerKey={}", normalizedProviderKey);
                continue;
            }
            if (!provider.isEnabled()) {
                log.info("event=speech_provider_skipped reason=disabled providerKey={}", normalizedProviderKey);
                continue;
            }

            foundUsableProvider = true;
            SpeechTranscriptionProperties.Provider providerConfig = properties.provider(normalizedProviderKey);
            long monthlyLimitSeconds = providerConfig.getMonthlyLimitSeconds();
            SpeechUsageSnapshot usage = usageMonthlyService.getCurrentUsage(
                    normalizedProviderKey,
                    monthlyLimitSeconds,
                    providerConfig.isEnabled()
            );

            if (monthlyLimitSeconds <= 0 || usage.usedSeconds() + validatedAudio.durationSeconds() > monthlyLimitSeconds) {
                blockedByLimit = true;
                log.warn("event=speech_provider_skipped reason=monthly_limit providerKey={} usedSeconds={} requestedSeconds={} limitSeconds={}",
                        normalizedProviderKey, usage.usedSeconds(), validatedAudio.durationSeconds(), monthlyLimitSeconds);
                continue;
            }

            try {
                String languageCode = languageCode(providerConfig);
                SpeechTranscriptionResult result = provider.transcribe(new SpeechTranscriptionRequest(
                        validatedAudio.audioBytes(),
                        validatedAudio.contentType(),
                        languageCode,
                        validatedAudio.durationSeconds()
                ));
                SpeechUsageSnapshot updatedUsage = usageMonthlyService.recordUsage(
                        normalizedProviderKey,
                        validatedAudio.durationSeconds(),
                        monthlyLimitSeconds,
                        providerConfig.isEnabled()
                );
                log.info("event=speech_transcription_success providerKey={} durationSeconds={}",
                        normalizedProviderKey, validatedAudio.durationSeconds());
                return new SpeechTranscriptionResponse(
                        result.text(),
                        result.languageCode(),
                        result.providerKey(),
                        updatedUsage.usedSeconds(),
                        updatedUsage.monthlyLimitSeconds(),
                        updatedUsage.remainingSeconds()
                );
            } catch (SpeechProviderException ex) {
                log.warn("event=speech_provider_failed providerKey={} message={}", normalizedProviderKey, ex.getMessage());
            }
        }

        throw unavailable(blockedByLimit && foundUsableProvider
                ? SpeechTranscriptionFailureReason.LIMIT_REACHED
                : SpeechTranscriptionFailureReason.UNAVAILABLE);
    }

    private ValidatedAudio validate(MultipartFile audio, Integer durationSeconds) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Audio file is required");
        }
        if (audio.getSize() > properties.getMaxAudioBytes()) {
            throw new ResponseStatusException(BAD_REQUEST, "Audio file is too large");
        }
        int safeDurationSeconds = durationSeconds == null ? 0 : durationSeconds;
        if (safeDurationSeconds <= 0 || safeDurationSeconds > properties.getMaxAudioSeconds()) {
            throw new ResponseStatusException(BAD_REQUEST, "Audio duration is invalid");
        }

        String contentType = normalizeContentType(audio.getContentType());
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "Audio type is not supported");
        }

        try {
            return new ValidatedAudio(audio.getBytes(), contentType, safeDurationSeconds);
        } catch (IOException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Audio file could not be read");
        }
    }

    private String normalizeProviderKey(String providerKey) {
        return providerKey == null ? "" : providerKey.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private String languageCode(SpeechTranscriptionProperties.Provider providerConfig) {
        String providerLanguageCode = providerConfig.getLanguageCode();
        if (providerLanguageCode != null && !providerLanguageCode.isBlank()) {
            return providerLanguageCode.trim();
        }
        return properties.getDefaultLanguageCode();
    }

    private SpeechTranscriptionUnavailableException unavailable(SpeechTranscriptionFailureReason reason) {
        return new SpeechTranscriptionUnavailableException(reason, UNAVAILABLE_MESSAGE);
    }

    private record ValidatedAudio(byte[] audioBytes, String contentType, int durationSeconds) {
    }
}
