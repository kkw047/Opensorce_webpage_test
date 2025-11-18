package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existsLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) return false;
        return userRepository.existsByLoginId(loginId.trim());
    }

    public boolean existsEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return userRepository.existsByEmailIgnoreCase(email.trim());
    }

    public User register(String loginId,
                         String email,
                         String rawPassword,
                         String nickname,
                         String regionDo,
                         String regionSi,
                         List<Long> categoryIds) {

        if (loginId != null && !loginId.isBlank() && userRepository.existsByLoginId(loginId.trim())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User u = User.builder()
                .loginId((loginId == null || loginId.isBlank()) ? null : loginId.trim())
                .email(email.trim())
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname.trim())
                .regionDo(regionDo)
                .regionSi(regionSi)
                .build();

        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> cats = categoryRepository.findAllByIdIn(categoryIds);
            u.getCategories().addAll(cats);
        }

        return userRepository.save(u);
    }

    public Optional<User> findByEmailOrLoginId(String loginIdOrEmail) {
        if (loginIdOrEmail == null) return Optional.empty();
        String v = loginIdOrEmail.trim();
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(v);
        return byEmail.isPresent() ? byEmail : userRepository.findByLoginId(v);
    }

    public boolean checkPassword(User user, String raw) {
        return passwordEncoder.matches(raw, user.getPassword());
    }
}
