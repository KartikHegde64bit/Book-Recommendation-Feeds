package com.avidreader.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for the Python Flask embedding microservice.
 * Calls POST http://127.0.0.1:5000/embed with { "text": "..." }
 * and returns the 384-dimensional float[] embedding vector.
 */
@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final String embeddingServiceUrl;

    public EmbeddingService() {
        this.restTemplate = new RestTemplate();
        this.embeddingServiceUrl = "http://127.0.0.1:5000/embed";
    }

    /**
     * Sends text to the Python embedding microservice and returns the vector.
     *
     * @param text the input text to embed
     * @return 384-dimensional float array
     */
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
        return result;
    }
}
