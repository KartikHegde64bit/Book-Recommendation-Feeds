package com.avidreader.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public RecommendationService(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Embeds the query text via the Python microservice, then runs a
     * pgvector nearest-neighbour search against the books table.
     *
     * @param query free-text search query
     * @return top-5 books ranked by vector distance
     */
    public List<Map<String, Object>> recommend(String query) {
        float[] embedding = embeddingService.getEmbedding(query);

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        String vectorStr = sb.toString();

        String sql = "SELECT id, title, authors, bookshelves, "
                + "(embedding <#> ?::vector) AS distance "
                + "FROM books ORDER BY distance LIMIT 5";

        return jdbcTemplate.queryForList(sql, vectorStr);
    }
}
