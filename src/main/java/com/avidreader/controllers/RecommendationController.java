package com.avidreader.controllers;

import com.avidreader.services.RecommendationService;
import com.avidreader.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserService userService;

    public RecommendationController(RecommendationService recommendationService,
                                    UserService userService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    /** Search books by free-text query. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> recommend(@RequestParam String query) {
        List<Map<String, Object>> results = recommendationService.recommend(query);
        return ResponseEntity.ok(results);
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
