package com.avidreader.services;

import com.avidreader.dtos.GoogleBookDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with the Google Books API.
 * Provides methods to search books and fetch book details.
 */
@Service
public class GoogleBooksService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final int maxResults;

    public GoogleBooksService(
            @Value("${google.books.api.key:AIzaSyAiwqjwq1glOJ-y9RvClhkmCZ1gaUhnthk}") String apiKey,
            @Value("${google.books.api.base-url:https://www.googleapis.com/books/v1}") String baseUrl,
            @Value("${google.books.api.max-results:10}") int maxResults) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.maxResults = maxResults;
    }

    /**
     * Search for books using the Google Books API.
     *
     * @param query the search query
     * @return list of GoogleBookDTO objects
     */
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

        return results;
    }

    /**
     * Get detailed information about a specific book by its Google Volume ID.
     *
     * @param volumeId the Google Volume ID
     * @return GoogleBookDTO with book details
     */
    @SuppressWarnings("unchecked")
    public GoogleBookDTO getBookDetails(String volumeId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/volumes/" + volumeId)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return mapToGoogleBookDTO(response);
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
