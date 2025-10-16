package com.avidreader.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    public RecommendationController(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    @GetMapping
    public List<Map<String, Object>> recommend(@RequestParam String query) {

    }
}
