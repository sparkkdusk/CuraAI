package com.curaai.curaai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NutritionService {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String analyzeFood(MultipartFile image) {

        try {
            // Convert image to base64
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String mimeType = image.getContentType(); // e.g. "image/jpeg"

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // IMAGE content part
            Map<String, Object> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);

            // TEXT content part
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", """
    You are a professional nutritionist and food analyst.
    
    Carefully analyze this food image and respond ONLY in this exact format, no extra text:
    
    FOOD_NAME: [specific food name]
    CALORIES: [number only, kcal]
    PROTEIN: [number only, grams]
    CARBS: [number only, grams]
    FAT: [number only, grams]
    FIBER: [number only, grams]
    SUGAR: [number only, grams]
    HEALTH_SCORE: [1-10 score]
    MEAL_TYPE: [Breakfast/Lunch/Dinner/Snack]
    INSIGHT: [2-3 sentence expert nutritionist insight about this food, health benefits, warnings, and tips]
    """);

            // Combine parts into message content
            List<Object> contentParts = new ArrayList<>();
            contentParts.add(imagePart);
            contentParts.add(textPart);

            // User message
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", contentParts);

            // Request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "meta-llama/llama-4-scout-17b-16e-instruct"); // free vision model
            requestBody.put("max_tokens", 1024);
            requestBody.put("messages", List.of(userMessage));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    API_URL,
                    request,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            // Parse response: choices[0].message.content
            String result = root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }
}