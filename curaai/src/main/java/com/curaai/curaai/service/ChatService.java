package com.curaai.curaai.service;

import com.curaai.curaai.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChatService {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String SYSTEM_PROMPT =
            "You are CuraAI, a friendly, knowledgeable health assistant. " +
                    "Give clear, safe, general guidance and always recommend seeing a doctor for serious or urgent concerns.Messages should not be too long";

    // Keep this to avoid breaking any other callers of the single-arg version
    public String getChatResponse(String userMessage) {
        return getChatResponse(userMessage, Collections.emptyList());
    }

    public String getChatResponse(String userMessage, List<ChatMessage> history) {

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            List<Map<String, Object>> messages = new ArrayList<>();

            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);

            // Optionally cap history length to avoid huge/expensive prompts
            int maxHistory = 20;
            int start = Math.max(0, history.size() - maxHistory);

            for (ChatMessage msg : history.subList(start, history.size())) {
                Map<String, Object> m = new HashMap<>();
                // "bot" sender in your DB -> "assistant" role for the API
                m.put("role", "user".equalsIgnoreCase(msg.getSender()) ? "user" : "assistant");
                m.put("content", msg.getContent());
                messages.add(m);
            }

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    API_URL,
                    request,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            return root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }
}