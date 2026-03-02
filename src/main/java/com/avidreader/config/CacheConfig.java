package com.avidreader.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Enables Spring Cache backed by Caffeine (in-memory) and Spring Retry.
 *
 * <p>Cache regions:
 * <ul>
 *   <li>{@code googleBooks}  – search results returned by the Google Books API (30 min TTL, max 500 entries)</li>
 *   <li>{@code bookDetails}  – single-volume detail responses (30 min TTL, max 500 entries)</li>
 * </ul>
 *
 * <p>{@link EnableRetry} activates {@code @Retryable} / {@code @Recover} processing defined in the
 * service layer via AOP proxies.
 */
@Configuration
@EnableCaching
@EnableRetry
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(Arrays.asList("googleBooks", "bookDetails"));
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(500)
        );
        return manager;
    }
}
