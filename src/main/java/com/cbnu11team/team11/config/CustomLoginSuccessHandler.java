package com.cbnu11team.team11.config;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 1. PrincipalDetailsService가 넘겨준 'Login ID'를 꺼냄
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String loginId = userDetails.getUsername();

        // 2. Login ID로 DB 조회 (이제 무조건 성공함)
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다. (ID: " + loginId + ")"));

        // 3. 세션 채우기
        HttpSession session = request.getSession();
        session.setAttribute("LOGIN_USER_ID", user.getId());
        session.setAttribute("LOGIN_USER_NICKNAME", user.getNickname()); // 닉네임 (상단바용)

        // 4. 메인으로 이동
        response.sendRedirect("/clubs");
    }
}