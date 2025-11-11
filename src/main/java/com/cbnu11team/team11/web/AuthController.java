package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.service.UserService;
import com.cbnu11team.team11.web.dto.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @GetMapping("/check-id")
    public Map<String, Object> checkId(@RequestParam("loginId") String loginId) {
        return Map.of("duplicated", userService.isDuplicatedLoginId(loginId));
    }

    @GetMapping("/check-email")
    public Map<String, Object> checkEmail(@RequestParam("email") String email) {
        return Map.of("duplicated", userService.isDuplicatedEmail(email));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        User u = userService.register(req.getLoginId(), req.getPassword(), req.getEmail(), req.getNickname(), req.getCategoryIds());
        return ResponseEntity.ok(UserResponse.builder()
                .id(u.getId()).loginId(u.getLoginId()).email(u.getEmail()).nickname(u.getNickname()).build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpSession session) {
        return userService.authenticate(req.getLoginId(), req.getPassword())
                .<ResponseEntity<?>>map(u -> {
                    session.setAttribute("USER_ID", u.getId());
                    session.setAttribute("NICKNAME", u.getNickname());
                    return ResponseEntity.ok(UserResponse.builder()
                            .id(u.getId()).loginId(u.getLoginId()).email(u.getEmail()).nickname(u.getNickname()).build());
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다.")));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        Long uid = (Long) session.getAttribute("USER_ID");
        String nick = (String) session.getAttribute("NICKNAME");
        if (uid == null) return Map.of("authenticated", false);
        return Map.of("authenticated", true, "id", uid, "nickname", nick);
    }
}
