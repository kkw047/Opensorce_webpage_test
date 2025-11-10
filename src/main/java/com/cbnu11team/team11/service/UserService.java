package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.web.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isDuplicateLoginId(String loginId) {
        return userRepository.existsByLoginIdIgnoreCase(loginId);
    }

    public User register(RegisterRequest req) {
        if (userRepository.existsByLoginIdIgnoreCase(req.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User u = new User();
        u.setLoginId(req.getLoginId().trim());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        // 필요시 닉네임/이메일/카테고리 등 추가 설정
        return userRepository.save(u);
    }
}
