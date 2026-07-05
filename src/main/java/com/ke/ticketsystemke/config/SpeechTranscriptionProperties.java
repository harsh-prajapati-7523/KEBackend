package com.ke.ticketsystemke.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "speech.transcription")
public class SpeechTranscriptionProperties {

    private boolean enabled = false;
    private List<String> providerPriority = new ArrayList<>(List.of("GOOGLE_STT", "OPENAI_STT", "AZURE_STT"));
    private String defaultLanguageCode = "hi-IN";
    private int maxAudioSeconds = 30;
    private long maxAudioBytes = 5 * 1024 * 1024;
    private Google google = new Google();
    private OpenAi openai = new OpenAi();
    private Azure azure = new Azure();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getProviderPriority() {
        return providerPriority;
    }

    public void setProviderPriority(List<String> providerPriority) {
        this.providerPriority = providerPriority;
    }

    public String getDefaultLanguageCode() {
        return defaultLanguageCode;
    }

    public void setDefaultLanguageCode(String defaultLanguageCode) {
        this.defaultLanguageCode = defaultLanguageCode;
    }

    public int getMaxAudioSeconds() {
        return maxAudioSeconds;
    }

    public void setMaxAudioSeconds(int maxAudioSeconds) {
        this.maxAudioSeconds = maxAudioSeconds;
    }

    public long getMaxAudioBytes() {
        return maxAudioBytes;
    }

    public void setMaxAudioBytes(long maxAudioBytes) {
        this.maxAudioBytes = maxAudioBytes;
    }

    public Google getGoogle() {
        return google;
    }

    public void setGoogle(Google google) {
        this.google = google;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public Azure getAzure() {
        return azure;
    }

    public void setAzure(Azure azure) {
        this.azure = azure;
    }

    public Provider provider(String providerKey) {
        return switch (providerKey) {
            case "GOOGLE_STT" -> google;
            case "OPENAI_STT" -> openai;
            case "AZURE_STT" -> azure;
            default -> Provider.disabled(defaultLanguageCode);
        };
    }

    public static class Provider {
        private boolean enabled = false;
        private int monthlyLimitSeconds = 3600;
        private String languageCode;

        static Provider disabled(String languageCode) {
            Provider provider = new Provider();
            provider.setEnabled(false);
            provider.setLanguageCode(languageCode);
            return provider;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMonthlyLimitSeconds() {
            return monthlyLimitSeconds;
        }

        public void setMonthlyLimitSeconds(int monthlyLimitSeconds) {
            this.monthlyLimitSeconds = monthlyLimitSeconds;
        }

        public String getLanguageCode() {
            return languageCode;
        }

        public void setLanguageCode(String languageCode) {
            this.languageCode = languageCode;
        }
    }

    public static class Google extends Provider {
        private String applicationCredentials = "";

        public Google() {
            setLanguageCode("hi-IN");
        }

        public String getApplicationCredentials() {
            return applicationCredentials;
        }

        public void setApplicationCredentials(String applicationCredentials) {
            this.applicationCredentials = applicationCredentials;
        }
    }

    public static class OpenAi extends Provider {
        private String apiKey = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Azure extends Provider {
        private String key = "";
        private String region = "";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
