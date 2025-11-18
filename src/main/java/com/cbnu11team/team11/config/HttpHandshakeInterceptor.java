package com.cbnu11team.team11.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 핸드셰이크 과정에서 HTTP 세션의 속성을 WebSocket 세션 속성으로 복사하는 인터셉터
 * (STOMP 컨트롤러에서 사용자 ID를 참조하기 위해 필요)
 */
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession httpSession = servletRequest.getServletRequest().getSession(false); // false: 세션이 없으면 새로 생성하지 않음

            if (httpSession != null) {
                // HTTP 세션에서 WebSocket 세션 속성으로 복사
                Object userId = httpSession.getAttribute("LOGIN_USER_ID");
                Object userNickname = httpSession.getAttribute("LOGIN_USER_NICKNAME");

                if (userId != null) {
                    attributes.put("LOGIN_USER_ID", userId);
                }
                if (userNickname != null) {
                    attributes.put("LOGIN_USER_NICKNAME", userNickname);
                }
            }
        }
        return true; // 핸드셰이크 계속 진행
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 완료 후
    }
}