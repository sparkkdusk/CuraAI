package com.curaai.curaai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wraps Voyage AI's embeddings endpoint (api.voyageai.com), used to turn
 * nutrition/exercise/prescription records and chat questions into vectors
 * for the pgvector-backed RAG store (see VectorStoreService).
 *
 * Voyage was chosen over OpenAI/Gemini embeddings because it offers 200M
 * free tokens/month with no credit card required, and is a completely
 * separate account/provider from Groq and Gemini - so an outage or block
 * on either of those never takes RAG down with it.
 *
 * Setup: create a free key at https://dash.voyageai.com and add it to
 * application.properties as: voyage.api.key=pa-...
 */
@Service
public class EmbeddingService {

    @Value("${voyage.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.voyageai.com/v1/embeddings";

    // voyage-4-lite: 1024 dims by default, cheapest tier, included in the
    // 200M free tokens/month. If you ever change models or request a
    // different output_dimension, update the vector(1024) column in the
    // V2 migration to match, or every search will fail with a dimension
    // mismatch error.
    private static final String MODEL = "voyage-4-lite";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param text      the text to embed
     * @param inputType "document" when storing content to be searched later,
     *                  "query" when embedding a user's question to search
     *                  against already-stored content. Voyage recommends
     *                  setting this for best retrieval accuracy; embeddings
     *                  produced with either setting remain comparable.
     */
    public List<Double> embed(String text, String inputType) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = Map.of(
                "input", List.of(text),
                "model", MODEL,
                "input_type", inputType
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Voyage's free tier has a very low requests-per-minute cap and answers 429 when exceeded.
        // Documents (indexing runs in the background) wait and retry; queries (user is waiting) retry once.
        int maxRetries = "document".equals(inputType) ? 3 : 1;
        long waitMs = "document".equals(inputType) ? 21_000 : 4_000;
        ResponseEntity<String> response = null;
        for (int attempt = 0; ; attempt++) {
            try {
                response = restTemplate.postForEntity(API_URL, request, String.class);
                break;
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt >= maxRetries) throw e;
                try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw e; }
            }
        }

        try {
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");

            List<Double> embedding = new ArrayList<>();
            for (JsonNode v : embeddingNode) {
                embedding.add(v.asDouble());
            }
            return embedding;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Voyage embedding response: " + e.getMessage(), e);
        }
    }
}