package com.curaai.curaai.controller;

import com.curaai.curaai.repository.ChatMessageRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;

    public ChatController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/chat")
    public String chatPage(Authentication authentication, Model model) {
        model.addAttribute("history",
                chatMessageRepository.findByUserEmailOrderByCreatedAtAsc(authentication.getName()));
        return "chat";
    }
}
