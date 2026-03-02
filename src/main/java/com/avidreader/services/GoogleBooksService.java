package com.avidreader.services;

import com.avidreader.dtos.GoogleBookDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for interacting with the Google Books API.
 * Provides methods to search books and fetch book details.
 *
 * <p>Resilience strategy:
 * <ul>
 *   <li>Connect / read timeouts are set on the shared {@link RestTemplate} bean
 *       ({@link com.avidreader.config.RestTemplateConfig}).</li>
 *   <li>Up to 3 attempts with exponential backoff (500 ms base, &times;2 per retry).</li>
 *   <li>Successful responses are stored in Caffeine caches ({@code googleBooks} /
 *       {@code bookDetails}, 30&nbsp;min TTL) via {@code @Cacheable}. Cache hits bypass
 *       the network entirely.</li>
 *   <li>In parallel, the last successful result is persisted in a local stale-value map so
 *       that {@code @Recover} can return meaningful data even on a cold Caffeine cache.</li>
 * </ul>
 */
@Service
public class GoogleBooksService {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final int maxResults;

    /** Stale-value fallback caches – populated on each successful API call. */
    private final ConcurrentHashMap<String, List<GoogleBookDTO>> searchFallbackCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GoogleBookDTO> detailFallbackCache = new ConcurrentHashMap<>();

    public GoogleBooksService(
            RestTemplate restTemplate,
            @Value("${google.books.api.key:AIzaSyAiwqjwq1glOJ-y9RvClhkmCZ1gaUhnthk}") String apiKey,
            @Value("${google.books.api.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${google.books.api.max-results:10}") int maxResults) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.maxResults = maxResults;
    }

    /**
     * Search for books using the Google Books API.
     * Results are served from the {@code googleBooks} Caffeine cache (30 min TTL) on
     * subsequent calls with the same query, avoiding a network round-trip entirely.
     *
     * @param query the search query
     * @return list of GoogleBookDTO objects
     */
    @Cacheable(cacheNames = "googleBooks", key = "#query")
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    @SuppressWarnings("unchecked")
    public List<GoogleBookDTO> searchBooks(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/volumes")
                .queryParam("q", query)
                .queryParam("maxResults", maxResults)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        List<GoogleBookDTO> results = new ArrayList<>();

        if (response == null || !response.containsKey("items")) {
            return results;
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        for (Map<String, Object> item : items) {
            GoogleBookDTO dto = mapToGoogleBookDTO(item);
            if (dto != null) {
                results.add(dto);
            }
        }

        // Persist for stale-value fallback.
        searchFallbackCache.put(query, results);
        return results;
    }

    /**
     * Recovery invoked after all {@link #searchBooks} retry attempts are exhausted.
     * Returns the last cached search results for the query if available, otherwise an empty list.
     */
    @Recover
    public List<GoogleBookDTO> searchBooksFallback(Exception e, String query) {
        log.warn("Google Books search failed after retries for query='{}'. Cause: {}", query, e.getMessage());
        List<GoogleBookDTO> stale = searchFallbackCache.get(query);
        if (stale != null) {
            log.info("Returning stale cached results for query='{}'", query);
            return stale;
        }
        log.warn("No stale data for query='{}'. Returning empty list.", query);
        return Collections.emptyList();
    }

    /**
     * Get detailed information about a specific book by its Google Volume ID.
     * Results are served from the {@code bookDetails} Caffeine cache (30 min TTL) on
     * subsequent calls with the same volumeId.
     *
     * @param volumeId the Google Volume ID
     * @return GoogleBookDTO with book details
     */
    @Cacheable(cacheNames = "bookDetails", key = "#volumeId")
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    @SuppressWarnings("unchecked")
    public GoogleBookDTO getBookDetails(String volumeId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/volumes/" + volumeId)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        GoogleBookDTO dto = mapToGoogleBookDTO(response);

        // Persist for stale-value fallback.
        if (dto != null) {
            detailFallbackCache.put(volumeId, dto);
        }
        return dto;
    }

    /**
     * Recovery invoked after all {@link #getBookDetails} retry attempts are exhausted.
     * Returns the last cached detail for the volumeId if available, otherwise {@code null}.
     */
    @Recover
    public GoogleBookDTO getBookDetailsFallback(Exception e, String volumeId) {
        log.warn("Google Books detail fetch failed after retries for volumeId='{}'. Cause: {}", volumeId, e.getMessage());
        GoogleBookDTO stale = detailFallbackCache.get(volumeId);
        if (stale != null) {
            log.info("Returning stale cached detail for volumeId='{}'", volumeId);
            return stale;
        }
        log.warn("No stale data for volumeId='{}'. Returning null.", volumeId);
        return null;
    }

    /**
     * Maps the Google Books API response to a GoogleBookDTO.
     */
    @SuppressWarnings("unchecked")
    private GoogleBookDTO mapToGoogleBookDTO(Map<String, Object> item) {
        if (item == null) {
            return null;
        }

        GoogleBookDTO dto = new GoogleBookDTO();
        dto.setGoogleId((String) item.get("id"));

        Map<String, Object> volumeInfo = (Map<String, Object>) item.get("volumeInfo");
        if (volumeInfo != null) {
            dto.setTitle((String) volumeInfo.get("title"));
            
            // Join authors array into a single string
            List<String> authors = (List<String>) volumeInfo.get("authors");
            if (authors != null && !authors.isEmpty()) {
                dto.setAuthors(String.join("; ", authors));
            }

            dto.setPublisher((String) volumeInfo.get("publisher"));
            dto.setPublishedDate((String) volumeInfo.get("publishedDate"));
            dto.setDescription((String) volumeInfo.get("description"));
            dto.setLanguage((String) volumeInfo.get("language"));
            
            if (volumeInfo.get("pageCount") != null) {
                dto.setPageCount(((Number) volumeInfo.get("pageCount")).intValue());
            }
            
            if (volumeInfo.get("averageRating") != null) {
                dto.setAverageRating(((Number) volumeInfo.get("averageRating")).doubleValue());
            }

            // Join categories array into a single string
            List<String> categories = (List<String>) volumeInfo.get("categories");
            if (categories != null && !categories.isEmpty()) {
                dto.setCategories(String.join("; ", categories));
            }

            // Extract image links
            Map<String, Object> imageLinks = (Map<String, Object>) volumeInfo.get("imageLinks");
            if (imageLinks != null) {
                String thumbnail = (String) imageLinks.get("thumbnail");
                if (thumbnail == null) {
                    thumbnail = (String) imageLinks.get("smallThumbnail");
                }
                dto.setThumbnail(thumbnail);
            }

            dto.setInfoLink((String) volumeInfo.get("infoLink"));
        }

        // Extract sale info for buy link
        Map<String, Object> saleInfo = (Map<String, Object>) item.get("saleInfo");
        if (saleInfo != null) {
            dto.setBuyLink((String) saleInfo.get("buyLink"));
        }

        return dto;
    }
}
