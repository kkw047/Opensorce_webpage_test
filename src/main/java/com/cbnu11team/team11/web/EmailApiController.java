package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.EmailVerificationService;
import com.cbnu11team.team11.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class EmailApiController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    // 1) 아이디 중복 검사
    @GetMapping("/check-login-id")
    public ResponseEntity<?> checkLoginId(@RequestParam String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return ResponseEntity.badRequest().body("아이디를 입력해 주세요.");
        }
        boolean exists = userService.existsLoginId(loginId);
        return ResponseEntity.ok(Map.of(
                "loginId", loginId.trim(),
                "available", !exists
        ));
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("이메일을 입력해 주세요.");
        }
        boolean exists = userService.existsEmail(email);
        return ResponseEntity.ok(Map.of(
                "email", email.trim(),
                "available", !exists
        ));
    }

    public record EmailRequest(String email) {}

    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmailCode(@RequestBody EmailRequest req,
                                           HttpSession session) {
        try {
            String email = req.email();
            if (userService.existsEmail(email)) {
                return ResponseEntity.badRequest().body("이미 가입된 이메일입니다.");
            }
            emailVerificationService.sendCode(email, session);
            return ResponseEntity.ok("인증 코드를 전송했습니다.");
        } catch (IllegalStateException e) {
            // 쿨타임 위반 같은 것들
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("인증 메일 전송 중 오류가 발생했습니다.");
        }
    }

    public record EmailVerifyRequest(String email, String code) {}

    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyEmailCode(@RequestBody EmailVerifyRequest req,
                                             HttpSession session) {
        try {
            emailVerificationService.verifyCode(req.email(), req.code(), session);
            return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
