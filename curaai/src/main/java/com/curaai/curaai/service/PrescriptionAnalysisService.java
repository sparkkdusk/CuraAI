package com.curaai.curaai.service;

import com.curaai.curaai.model.Medication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Reads a prescription image and returns both the raw text and structured
 * medication entries in a single call, using Google's Gemini API.
 *
 * Gemini 2.5 Flash is used because it's on Google's free tier (no credit
 * card required, rate-limited to roughly 10 requests/min and 1,500/day as
 * of mid 2026) and handles OCR / handwriting well - a solid free alternative
 * to both the old Hugging Face pipeline and paid Claude/GPT vision calls.
 *
 * Setup:
 * 1. Go to https://aistudio.google.com -> "Get API key" -> create a key
 *    (no billing needed for the free tier).
 * 2. Put it in application.properties as: gemini.api.key=AIza...
 */
@Service
public class PrescriptionAnalysisService {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

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

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public PrescriptionAnalysisService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public record Result(String rawText, List<Medication> medications) {}

    public Result analyze(byte[] imageBytes, String contentType) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(
                                    Map.of("inline_data", Map.of(
                                            "mime_type", normalizeMediaType(contentType),
                                            "data", base64Image
                                    )),
                                    Map.of("text", PROMPT)
                            )
                    )),
                    // Ask Gemini to return strict JSON so we don't have to
                    // defensively strip markdown fences from the response.
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json"
                    )
            );

            String responseBody = webClient.post()
                    .uri(API_URL + "?key=" + geminiApiKey)
                    .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).map(errBody ->
                                    new RuntimeException("Gemini API returned " + clientResponse.statusCode()
                                            + " - " + errBody)
                            )
                    )
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze prescription image: " + rootMessage(e), e);
        }
    }

    private Result parseResponse(String responseJson) throws Exception {
        JsonNode root = mapper.readTree(responseJson);

        // Gemini response shape: candidates[0].content.parts[0].text
        String text = root.path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText("");

        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }

        if (cleaned.isEmpty()) {
            // Gemini can return an empty candidate if its safety filters trip
            // on a low-quality image; surface something usable rather than
            // throwing a bare parse error.
            JsonNode finishReason = root.path("candidates").path(0).path("finishReason");
            throw new RuntimeException("Gemini returned no content (finishReason: "
                    + finishReason.asText("unknown") + ")");
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

    /** Gemini's vision input accepts image/jpeg, image/png, image/heic, image/webp. */
    private String normalizeMediaType(String contentType) {
        if (contentType == null) return "image/jpeg";
        return switch (contentType.toLowerCase()) {
            case "image/png", "image/heic", "image/webp" -> contentType.toLowerCase();
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
