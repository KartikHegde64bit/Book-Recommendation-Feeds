package com.avidreader.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP client for the Python Flask embedding microservice.
 * Calls POST http://127.0.0.1:5000/embed with { "text": "..." }
 * and returns the 384-dimensional float[] embedding vector.
 *
 * <p>Resilience strategy:
 * <ul>
 *   <li>Connect / read timeouts are set on the shared {@link RestTemplate} bean
 *       ({@link com.avidreader.config.RestTemplateConfig}).</li>
 *   <li>Up to 3 attempts with exponential backoff (500 ms base, &times;2 per retry).</li>
 *   <li>On total failure the last successfully computed embedding for the same text is
 *       returned from a local stale-value map; a zero vector is used as last resort.</li>
 * </ul>
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int EMBEDDING_DIMENSION = 384;

    private final RestTemplate restTemplate;
    private final String embeddingServiceUrl;

    /** Stale-value fallback: last successful embedding keyed by input text. */
    private final ConcurrentHashMap<String, float[]> embeddingFallbackCache = new ConcurrentHashMap<>();

    public EmbeddingService(
            RestTemplate restTemplate,
            @Value("${embedding.service.url:http://127.0.0.1:5000/embed}") String embeddingServiceUrl) {
        this.restTemplate = restTemplate;
        this.embeddingServiceUrl = embeddingServiceUrl;
    }

    /**
     * Sends text to the Python embedding microservice and returns the vector.
     *
     * <p>Retried up to 3 times (including the initial attempt) with exponential back-off
     * starting at 500&nbsp;ms and doubling on each subsequent attempt.
     *
     * @param text the input text to embed
     * @return 384-dimensional float array
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    @SuppressWarnings("unchecked")
    public float[] getEmbedding(String text) {
        Map<String, String> request = Map.of("text", text);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                embeddingServiceUrl, request, Map.class);

        List<Number> embedding = (List<Number>) response.getBody().get("embedding");
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }

        // Persist successful result for stale-value fallback on future failures.
        embeddingFallbackCache.put(text, result);
        return result;
    }

    /**
     * Recovery method invoked after all {@link #getEmbedding} retry attempts are exhausted.
     * Returns the last cached embedding for the given text if one exists; otherwise a zero vector.
     */
    @Recover
    public float[] getEmbeddingFallback(Exception e, String text) {
        log.warn("Embedding service unavailable after retries for text='{}'. Cause: {}", text, e.getMessage());
        float[] stale = embeddingFallbackCache.get(text);
        if (stale != null) {
            log.info("Returning stale cached embedding for text='{}'", text);
            return stale;
        }
        log.warn("No stale embedding cached for text='{}'. Returning zero vector.", text);
        return new float[EMBEDDING_DIMENSION];
    }
}
