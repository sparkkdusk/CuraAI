package com.curaai.curaai.repository;

import com.curaai.curaai.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserEmailOrderByCreatedAtAsc(String userEmail);

    void deleteByUserEmail(String userEmail);
}
