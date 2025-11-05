package com.cbnu11team.opensorce11;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/ok", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
        ).formLogin(withDefaults -> {});
        return http.build();
    }
}
