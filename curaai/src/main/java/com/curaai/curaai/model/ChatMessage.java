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

    // NOTE: intentionally NOT @Lob. On Postgres, @Lob on a String maps to a
    // CLOB backed by large objects (OID), which pgjdbc can only stream inside
    // an explicit transaction - hence "Large Objects may not be used in
    // auto-commit mode" on plain reads like ChatController.chatPage().
    // columnDefinition = "TEXT" alone gives an unlimited-length Postgres text
    // column without any of that large-object machinery.
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