package com.avidreader.controllers;

import com.avidreader.dtos.GoogleBookDTO;
import com.avidreader.services.GoogleBooksService;
import com.avidreader.services.RecommendationService;
import com.avidreader.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserService userService;
    private final GoogleBooksService googleBooksService;

    public RecommendationController(RecommendationService recommendationService,
                                    UserService userService,
                                    GoogleBooksService googleBooksService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
        this.googleBooksService = googleBooksService;
    }

    /**
     * Search books by free-text query.
     * Returns combined results from local DB and Google Books API.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> recommend(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        
        // Get local results from pgvector search
        List<Map<String, Object>> localResults = recommendationService.recommend(query);
        // Mark local results with source
        localResults.forEach(book -> book.put("source", "local"));
        response.put("local", localResults);
        
        // Get Google Books results
        try {
            List<GoogleBookDTO> googleResults = googleBooksService.searchBooks(query);
            response.put("google", googleResults);
        } catch (Exception e) {
            // If Google API fails, return empty list for google results
            response.put("google", List.of());
        }
        
        return ResponseEntity.ok(response);
    }

    /** Get personalised feed based on user's saved preference tags. */
    @GetMapping("/feed")
    public ResponseEntity<List<Map<String, Object>>> feed(Authentication auth) {
        String username = auth.getName();
        Set<String> prefs = userService.getPreferences(username);
        if (prefs.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        String query = String.join(" ", prefs);
        List<Map<String, Object>> results = recommendationService.recommend(query);
        return ResponseEntity.ok(results);
    }
}
