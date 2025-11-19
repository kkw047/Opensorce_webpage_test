package com.cbnu11team.team11.config;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final CustomLoginFailureHandler customLoginFailureHandler; // 로그인 실패 핸들러

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화

                .authorizeHttpRequests(auth -> auth
                        // 1. 내부 포워딩 및 정적 리소스 허용
                        .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // 2. 페이지 접근 권한 (메인, 로그인, 회원가입, 에러)
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()
                        .requestMatchers("/clubs/**").permitAll()     // 모임 관련 페이지
                        .requestMatchers("/my-calendar").permitAll()  // 내 캘린더 페이지

                        // 3. API 접근 권한
                        // [중요] 회원가입/인증 관련 API (POST 포함) 허용
                        .requestMatchers("/api/auth/**").permitAll()

                        // [중요] 캘린더 조회 및 공통 데이터 조회 (GET만 허용)
                        .requestMatchers(HttpMethod.GET, "/api/club/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/my/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                        // 4. 그 외 모든 요청(글쓰기, 일정 생성/삭제/수정, 채팅 등)은 로그인 필요
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("loginIdOrEmail") // HTML name 속성과 일치
                        .successHandler(customLoginSuccessHandler) // 로그인 성공 시
                        .failureHandler(customLoginFailureHandler) // 로그인 실패 시 (에러 메시지 처리)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/clubs") // 로그아웃 후 메인으로
                        .invalidateHttpSession(true) // 세션 삭제
                        .permitAll()
                );

        return http.build();
    }
}