package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}