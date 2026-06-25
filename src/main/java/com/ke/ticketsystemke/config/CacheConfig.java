package com.ke.ticketsystemke.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CacheNames.ACCESS_KEY_METADATA,
                CacheNames.WORKFLOW_ACTIONS,
                CacheNames.WORKFLOW_STATUSES,
                CacheNames.WORKFLOW_TRANSITIONS,
                CacheNames.WORKFLOW_TRANSITION_OPTIONS,
                CacheNames.TICKET_CATEGORIES,
                CacheNames.DROPDOWN_SOURCES,
                CacheNames.DROPDOWN_OPTIONS
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(15)));
        return cacheManager;
    }
}
