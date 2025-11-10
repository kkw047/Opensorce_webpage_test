package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isDuplicatedLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) return false; // 빈 값은 중복 아님 처리
        return userRepository.existsByLoginIdIgnoreCase(loginId.trim());
    }

    @Transactional
    public User register(String loginId, String rawPassword) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("아이디를 입력하세요.");
        }
        String id = loginId.trim();
        if (userRepository.existsByLoginIdIgnoreCase(id)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력하세요.");
        }

        User u = new User();
        u.setLoginId(id);
        u.setPassword(passwordEncoder.encode(rawPassword));
        // 이메일 저장 호출 제거 (User 엔티티에 email 없음)

        return userRepository.save(u);
    }

    public Optional<User> authenticate(String loginId, String rawPassword) {
        if (loginId == null || rawPassword == null) return Optional.empty();
        return userRepository.findByLoginId(loginId.trim())
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()));
    }
}
