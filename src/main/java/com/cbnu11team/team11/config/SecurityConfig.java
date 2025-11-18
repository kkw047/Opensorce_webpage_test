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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/login", "/register", "/error").permitAll()

                        // [기존] 모임 관련
                        .requestMatchers("/clubs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/club/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()

                        // [추가] 내 캘린더 페이지 허용!
                        .requestMatchers("/my-calendar").permitAll()

                        // [추가] 내 캘린더 데이터 API 허용!
                        .requestMatchers("/api/my/**").permitAll()

                        .anyRequest().authenticated()
                )
                // ... (formLogin, logout 설정 그대로) ...
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("loginIdOrEmail")
                        .successHandler(customLoginSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/clubs")
                        .invalidateHttpSession(true)
                        .permitAll()
                );

        return http.build();
    }
}