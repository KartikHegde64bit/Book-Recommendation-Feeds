package com.avidreader.services;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.embedding.EmbeddingModel;

public class RecommendationService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public RecommendationService(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    public List<Map<String, Object>> recommend(String query) {
        float[] embedding = embeddingService.getEmbedding(query);

        String vectorStr = Arrays.stream(embedding)
                .mapToObj(Float::toString)
                .collect(Collectors.joining(",", "[", "]"));

        String sql = """
                SELECT id, title, authors, bookshelves, (embedding <#> ?::vector) AS distance
                FROM books ORDER BY distance LIMIT 5
                """;

        return jdbcTemplate.queryForList(sql, vectorStr);
    }
}
