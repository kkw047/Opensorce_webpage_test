package com.cbnu11team.team11.web.dto;

/**
 * WebSocket (STOMP) 메시지 전송 시 클라이언트가 보내는 페이로드 DTO
 */
public record MessageSendRequest(
        String content
) {
}