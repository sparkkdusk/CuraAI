package com.curaai.curaai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We key history off the authenticated user's email (the login username)
    // rather than a foreign key, so this drops in regardless of your User entity shape.
    @Column(nullable = false)
    private String userEmail;

    // "user" or "bot"
    @Column(nullable = false, length = 10)
    private String sender;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ChatMessage() {
    }

    public ChatMessage(String userEmail, String sender, String content) {
        this.userEmail = userEmail;
        this.sender = sender;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
