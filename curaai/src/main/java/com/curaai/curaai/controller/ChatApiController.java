package com.curaai.curaai.controller;

import com.curaai.curaai.model.ChatMessage;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.ChatMessageRepository;
import com.curaai.curaai.repository.UserRepository;
import com.curaai.curaai.service.ChatService;
import com.curaai.curaai.service.ExerciseService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ExerciseService exerciseService;

    public ChatApiController(ChatService chatService,
                             ChatMessageRepository chatMessageRepository,
                             UserRepository userRepository,
                             ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/send")
    public Map<String, String> sendMessage(@RequestBody Map<String, String> body, Authentication authentication) {
        String userEmail = authentication.getName();
        String userMessage = body.get("message");

        List<ChatMessage> history = chatMessageRepository.findByUserEmailOrderByCreatedAtAsc(userEmail);

        chatMessageRepository.save(new ChatMessage(userEmail, "user", userMessage));

        // Resolve the user id so ChatService can pull in their nutrition/
        // exercise/prescription history for RAG context. Falls back to no
        // RAG (null) rather than failing the whole chat if the lookup
        // somehow misses - findByEmail should always succeed here since
        // the person is already authenticated as this email.
        User user = userRepository.findByEmail(userEmail).orElse(null);
        Long userId = user != null ? user.getId() : null;

        // Always give the model the user's last 7 days of exercise directly (no vector search needed)
        String exerciseContext = null;
        if (user != null) {
            try {
                exerciseContext = exerciseService.weeklyContextText(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("[CHAT-DEBUG] /api/chat/send hit. userId=" + userId + " message=" + userMessage);
        System.out.println("[CHAT-DEBUG] exerciseContext=\n" + exerciseContext);

        String reply = chatService.getChatResponse(userId, userMessage, history, exerciseContext);

        chatMessageRepository.save(new ChatMessage(userEmail, "bot", reply));

        return Map.of("reply", reply);
    }

    @DeleteMapping("/clear")
    public Map<String, String> clearHistory(Authentication authentication) {
        chatMessageRepository.deleteByUserEmail(authentication.getName());
        return Map.of("status", "cleared");
    }
}