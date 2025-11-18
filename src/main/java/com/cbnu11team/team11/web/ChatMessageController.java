package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.ChatMessage;
import com.cbnu11team.team11.service.ChatService;
import com.cbnu11team.team11.web.dto.ChatMessageDto;
import com.cbnu11team.team11.web.dto.MessageSendRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 클라이언트가 "/app/chat/{roomId}/send"로 메시지를 보낼 때 처리
     */
    @MessageMapping("/chat/{roomId}/send")
    public void sendChatMessage(
            @DestinationVariable Long roomId,
            @Payload MessageSendRequest messageRequest,
            SimpMessageHeaderAccessor headerAccessor // 세션 속성에 접근하기 위해 추가
    ) {
        // 인터셉터에서 복사해준 WebSocket 세션 속성에서 사용자 ID 가져오기
        Long currentUserId = (Long) headerAccessor.getSessionAttributes().get("LOGIN_USER_ID");

        if (currentUserId == null) {
            return;
        }

        try {
            // 1. 메시지를 DB에 저장 (ChatService 재사용)
            ChatMessage savedMessage = chatService.sendMessage(
                    roomId,
                    currentUserId,
                    messageRequest.content()
            );

            // 2. DTO로 변환
            ChatMessageDto messageDto = ChatMessageDto.fromEntity(savedMessage);

            // 3. "/topic/chat/{roomId}"를 구독 중인 모든 클라이언트에게 메시지 브로드캐스팅
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, messageDto);

        } catch (Exception e) {
            // 예외 처리 (예: 특정 사용자에게 에러 메시지 전송)
            // messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/errors", e.getMessage());
            e.printStackTrace(); // 간단히 로그 출력
        }
    }
}