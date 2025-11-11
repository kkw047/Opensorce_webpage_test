package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isDuplicatedLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) return false;
        return userRepository.existsByLoginIdIgnoreCase(loginId.trim());
    }

    public boolean isDuplicatedEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return userRepository.existsByEmailIgnoreCase(email.trim());
    }

    @Transactional
    public User register(String loginId, String rawPassword, String email, String nickname, List<Long> categoryIds) {
        if (loginId == null || loginId.isBlank()) throw new IllegalArgumentException("아이디를 입력하세요.");
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("비밀번호를 입력하세요.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일을 입력하세요.");
        if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("닉네임을 입력하세요.");

        String id = loginId.trim();
        String mail = email.trim();
        String nick = nickname.trim();

        if (userRepository.existsByLoginIdIgnoreCase(id)) throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        if (userRepository.existsByEmailIgnoreCase(mail)) throw new IllegalArgumentException("이미 사용중인 이메일입니다.");

        Set<Category> cats = new LinkedHashSet<>();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            cats.addAll(categoryRepository.findAllById(new LinkedHashSet<>(categoryIds)));
        }

        User u = User.builder()
                .loginId(id)
                .email(mail)
                .nickname(nick)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        u.getCategories().addAll(cats);

        return userRepository.save(u);
    }

    public Optional<User> authenticate(String loginId, String rawPassword) {
        if (loginId == null || rawPassword == null) return Optional.empty();
        return userRepository.findByLoginId(loginId.trim())
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()));
    }
}
