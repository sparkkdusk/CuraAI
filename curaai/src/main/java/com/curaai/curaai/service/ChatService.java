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

    private final VectorStoreService vectorStoreService;

    public ChatService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    // Keep this to avoid breaking any other callers of the no-RAG version
    public String getChatResponse(String userMessage) {
        return getChatResponse(null, userMessage, Collections.emptyList(), null);
    }

    public String getChatResponse(Long userId, String userMessage, List<ChatMessage> history) {
        return getChatResponse(userId, userMessage, history, null);
    }

    /**
     * @param userId  used to look up this user's own nutrition/exercise/
     *                prescription history for RAG context. Pass null to
     *                skip retrieval entirely (e.g. for an anonymous or
     *                system-triggered chat call).
     */
    public String getChatResponse(Long userId, String userMessage, List<ChatMessage> history, String exerciseContext) {

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

            // Deterministic context: the user's last 7 days of logged exercise.
            if (exerciseContext != null && !exerciseContext.isBlank()) {
                Map<String, Object> exMsg = new HashMap<>();
                exMsg.put("role", "system");
                exMsg.put("content", "This is the user's logged exercise for the last 7 days (from their "
                        + "workout tracker). Use it whenever they ask about their workouts, activity or "
                        + "exercise:\n" + exerciseContext);
                messages.add(exMsg);
            }

            // RAG: pull in anything from the user's nutrition/exercise/
            // prescription history that's semantically relevant to what
            // they just asked, and hand it to the model as grounding
            // context. Retrieval failing for any reason (embeddings API
            // hiccup, no indexed history yet) never blocks the chat itself
            // - it just answers without the extra context.
            if (userId != null) {
                try {
                    List<String> relevant = vectorStoreService.searchRelevant(userId, userMessage, 8);
                    if (!relevant.isEmpty()) {
                        String contextBlock = "Here is relevant information from this user's own "
                                + "logged health history (nutrition, exercise, and prescriptions). "
                                + "Use it to answer their question accurately and specifically when "
                                + "it's relevant; ignore anything here that doesn't relate to what "
                                + "they asked:\n\n- " + String.join("\n- ", relevant);

                        Map<String, Object> contextMessage = new HashMap<>();
                        contextMessage.put("role", "system");
                        contextMessage.put("content", contextBlock);
                        messages.add(contextMessage);
                    }
                } catch (Exception ragError) {
                    ragError.printStackTrace();
                }
            }

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
            requestBody.put("model", "openai/gpt-oss-120b");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            System.out.println("[CHAT-DEBUG] messages sent to model:");
            for (Map<String, Object> m : messages) {
                System.out.println("  [" + m.get("role") + "] " + m.get("content"));
            }

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