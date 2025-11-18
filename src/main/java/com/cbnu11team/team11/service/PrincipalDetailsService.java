package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginIdOrEmail) throws UsernameNotFoundException {

        // 1. 입력받은 값이 '아이디'라고 가정하고 찾아봄
        User user = userRepository.findByLoginId(loginIdOrEmail)
                .orElseGet(() ->
                        // 2. 없으면 '이메일'이라고 가정하고 찾아봄
                        userRepository.findByEmailIgnoreCase(loginIdOrEmail)
                                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + loginIdOrEmail))
                );

        // 3. [핵심 해결책] 찾은 유저가 누구든, Spring Security에게는 무조건 'Login ID'를 이름으로 알려줌!
        // 이렇게 해야 핸들러가 헷갈리지 않고 DB에서 다시 찾을 수 있음.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getLoginId()) // 여기가 중요! (user.getEmail() 아님)
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}