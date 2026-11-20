package com.curaai.curaai.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin pgvector-backed store: each row is one piece of text (one nutrition
 * log, one exercise log, or one saved prescription) plus its embedding,
 * scoped to a user. Used so ChatService can pull in semantically relevant
 * history from the other three features when answering a question.
 *
 * Deliberately bypasses JPA/Hibernate for the embedding column itself -
 * pgvector's `vector` type is passed as a plain bracketed literal string
 * cast with ::vector in raw SQL via JdbcTemplate, which avoids needing a
 * custom Hibernate UserType and keeps this service easy to reason about.
 */
@Service
public class VectorStoreService {

    public enum SourceType { NUTRITION, EXERCISE, PRESCRIPTION }

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public VectorStoreService(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Embeds and stores (or re-stores) one piece of content for a user.
     * Call this right after saving a NutritionLog, ExerciseLog, or
     * Prescription so it becomes searchable from chat. Safe to call
     * repeatedly for the same source record - the old chunk is replaced.
     */
    @Transactional
    public void upsertChunk(Long userId, SourceType sourceType, Long sourceId, String content) {
        List<Double> embedding = embeddingService.embed(content, "document");
        String vectorLiteral = toVectorLiteral(embedding);

        jdbcTemplate.update(
                "DELETE FROM document_chunks WHERE source_type = ? AND source_id = ?",
                sourceType.name(), sourceId
        );

        jdbcTemplate.update(
                "INSERT INTO document_chunks (user_id, source_type, source_id, content, embedding) " +
                        "VALUES (?, ?, ?, ?, ?::vector)",
                userId, sourceType.name(), sourceId, content, vectorLiteral
        );
    }

    /**
     * Returns up to {@code topK} pieces of this user's history most
     * semantically relevant to {@code query}, nearest first. Empty list
     * if the user has no indexed history yet.
     */
    public List<String> searchRelevant(Long userId, String query, int topK) {
        List<Double> queryEmbedding = embeddingService.embed(query, "query");
        String vectorLiteral = toVectorLiteral(queryEmbedding);

        return jdbcTemplate.query(
                "SELECT content FROM document_chunks " +
                        "WHERE user_id = ? " +
                        "ORDER BY embedding <=> ?::vector " +
                        "LIMIT ?",
                (rs, rowNum) -> rs.getString("content"),
                userId, vectorLiteral, topK
        );
    }

    private String toVectorLiteral(List<Double> embedding) {
        return "[" + embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }
}
