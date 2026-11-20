package com.curaai.curaai.service;

import com.curaai.curaai.model.Medication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Reads a prescription image and returns both the raw text and structured
 * medication entries in a single call, using Groq's vision-capable chat
 * completions endpoint (OpenAI-compatible).
 *
 * Switched from Google's Gemini API after Gemini started returning
 * 403 PERMISSION_DENIED ("Your project has been denied access") - a known,
 * widely reported Google-account-level block (see discuss.ai.google.dev
 * threads from 2026) that persists even across brand-new projects/keys and
 * is unrelated to anything in this codebase.
 *
 * qwen/qwen3.6-27b is one of Groq's current documented vision-capable
 * models (console.groq.com/docs/vision) and reuses the same groq.api.key
 * already configured for ChatService/NutritionService - no new credentials
 * needed. gemini.api.key in application.properties is no longer used by
 * this class and can be removed if nothing else references it.
 */
@Service
public class PrescriptionAnalysisService {

    private static final String MODEL = "qwen/qwen3.8-27b";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String PROMPT = """
            You are reading a photo of a medical prescription, which may be handwritten \
            and hard to read. Extract the text and the individual medications from it.

            Respond with ONLY a raw JSON object (no markdown fences, no commentary) \
            in exactly this shape:

            {
              "rawText": "<all legible text from the image, line by line>",
              "medications": [
                { "drugName": "<name>", "dosage": "<e.g. 500mg, or 'Not detected'>", "frequency": "<e.g. twice daily, 1-0-1, or 'Not detected'>" }
              ]
            }

            Handwriting may be messy. If a word is genuinely ambiguous, give your best \
            reading rather than leaving it blank, but do not invent a drug name that \
            isn't plausibly supported by the strokes on the page.

            If the image is not a legible prescription, or no medications can be \
            confidently identified, return an empty "medications" array but still \
            fill in "rawText" with whatever is legible.
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${groq.api.key}")
    private String apiKey;

    public record Result(String rawText, List<Medication> medications) {}

    public Result analyze(byte[] imageBytes, String contentType) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = normalizeMediaType(contentType);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", PROMPT);

            List<Object> contentParts = new ArrayList<>();
            contentParts.add(imagePart);
            contentParts.add(textPart);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", contentParts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("max_tokens", 2048);
            requestBody.put("messages", List.of(userMessage));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

            return parseResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            // Surface Groq's actual error body (e.g. invalid key, rate limit,
            // model access issue) instead of a bare status code.
            throw new RuntimeException("Failed to analyze prescription image: Groq API returned "
                    + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze prescription image: " + rootMessage(e), e);
        }
    }

    private Result parseResponse(String responseJson) throws Exception {
        JsonNode root = mapper.readTree(responseJson);

        // Groq (OpenAI-compatible) response shape: choices[0].message.content
        String text = root.path("choices").path(0)
                .path("message").path("content").asText("");

        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }

        if (cleaned.isEmpty()) {
            throw new RuntimeException("Groq returned no content for this image");
        }

        JsonNode parsed = mapper.readTree(cleaned);
        String rawText = parsed.path("rawText").asText("");

        List<Medication> medications = new ArrayList<>();
        for (JsonNode med : parsed.path("medications")) {
            medications.add(new Medication(
                    med.path("drugName").asText("Unknown"),
                    med.path("dosage").asText("Not detected"),
                    med.path("frequency").asText("Not detected")
            ));
        }

        return new Result(rawText, medications);
    }

    /** Groq's vision models accept image/jpeg, image/png, image/webp. */
    private String normalizeMediaType(String contentType) {
        if (contentType == null) return "image/jpeg";
        return switch (contentType.toLowerCase()) {
            case "image/png", "image/webp" -> contentType.toLowerCase();
            default -> "image/jpeg";
        };
    }

    private String rootMessage(Throwable t) {
        Throwable current = t;
        String lastMessage = t.getMessage();
        while (current.getCause() != null) {
            current = current.getCause();
            if (current.getMessage() != null) {
                lastMessage = current.getMessage();
            }
        }
        return lastMessage;
    }
}