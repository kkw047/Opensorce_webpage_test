package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ChatMessage;
import java.time.format.DateTimeFormatter;

// 채팅방 내부 메시지 표시용
public record ChatMessageDto(
        Long id,
        String senderNickname,
        String content,
        String sentAt
) {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    public static ChatMessageDto fromEntity(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getSender() != null ? message.getSender().getNickname() : "(알 수 없음)", // sender가 null일 경우 대비
                message.getContent(),
                message.getSentAt().format(formatter)
        );
    }
}