package com.cbnu11team.team11.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // [Server -> Client] 메시지 브로커 설정
        // 클라이언트가 구독(subscribe)할 토픽의 접두사(prefix)
        registry.enableSimpleBroker("/topic");

        // [Client -> Server] 메시지 핸들러(컨트롤러) 설정
        // 클라이언트가 메시지를 보낼 때(send) 사용할 접두사
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket (또는 SockJS) 클라이언트가 서버에 연결할 엔드포인트
        registry.addEndpoint("/ws")
                .addInterceptors(new HttpHandshakeInterceptor()) // HTTP 세션 속성을 복사할 인터셉터
                .withSockJS(); // SockJS 사용 설정 (폴백 지원)
    }
}