package com.curaai.curaai.controller;

import com.curaai.curaai.model.ChatMessage;
import com.curaai.curaai.repository.ChatMessageRepository;
import com.curaai.curaai.service.ChatService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;

    public ChatApiController(ChatService chatService, ChatMessageRepository chatMessageRepository) {
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @PostMapping("/send")
    public Map<String, String> sendMessage(@RequestBody Map<String, String> body, Authentication authentication) {
        String userEmail = authentication.getName();
        String userMessage = body.get("message");

        List<ChatMessage> history = chatMessageRepository.findByUserEmailOrderByCreatedAtAsc(userEmail);

        chatMessageRepository.save(new ChatMessage(userEmail, "user", userMessage));

        String reply = chatService.getChatResponse(userMessage, history);

        chatMessageRepository.save(new ChatMessage(userEmail, "bot", reply));

        return Map.of("reply", reply);
    }

    @DeleteMapping("/clear")
    public Map<String, String> clearHistory(Authentication authentication) {
        chatMessageRepository.deleteByUserEmail(authentication.getName());
        return Map.of("status", "cleared");
    }
}
