package com.cbnu11team.team11.service;

import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailVerificationService {

    public static final String SESSION_KEY = "EMAIL_VERIFICATION_MAP";

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.verification.expire-minutes:5}")
    private long expireMinutes;

    @Value("${app.mail.verification.cooltime-seconds:60}")
    private long coolTimeSeconds;

    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @SuppressWarnings("unchecked")
    private Map<String, VerificationInfo> getMap(HttpSession session) {
        Object obj = session.getAttribute(SESSION_KEY);
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, VerificationInfo>) m;
        }
        Map<String, VerificationInfo> map = new HashMap<>();
        session.setAttribute(SESSION_KEY, map);
        return map;
    }

    public void sendCode(String email, HttpSession session) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }
        email = email.trim();

        Map<String, VerificationInfo> map = getMap(session);
        VerificationInfo info = map.get(email);
        LocalDateTime now = LocalDateTime.now();

        if (info != null && info.getLastSentAt() != null &&
                info.getLastSentAt().plusSeconds(coolTimeSeconds).isAfter(now)) {
            long remain = java.time.Duration.between(now, info.getLastSentAt().plusSeconds(coolTimeSeconds)).getSeconds();
            throw new IllegalStateException("인증 메일은 1분에 한 번만 보낼 수 있습니다. "
                    + remain + "초 후에 다시 시도해 주세요.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        VerificationInfo newInfo = new VerificationInfo();
        newInfo.setCode(code);
        newInfo.setLastSentAt(now);
        newInfo.setExpiresAt(now.plusMinutes(expireMinutes));
        newInfo.setVerified(false);
        map.put(email, newInfo);
        session.setAttribute(SESSION_KEY, map);

        // 메일 발송
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setFrom(from);
        msg.setSubject("[TEAM11] 이메일 인증 코드");
        msg.setText("""
                TEAM11 이메일 인증 코드입니다.

                인증 코드: %s

                %d분 이내에 입력해 주세요.
                """.formatted(code, expireMinutes));

        mailSender.send(msg);
    }

    public void verifyCode(String email, String code, HttpSession session) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            throw new IllegalArgumentException("이메일과 인증코드를 모두 입력해 주세요.");
        }
        email = email.trim();
        code = code.trim();

        Map<String, VerificationInfo> map = getMap(session);
        VerificationInfo info = map.get(email);
        if (info == null) {
            throw new IllegalStateException("먼저 인증 메일을 전송해 주세요.");
        }
        if (info.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("인증 코드가 만료되었습니다. 다시 요청해 주세요.");
        }
        if (!info.getCode().equals(code)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }
        info.setVerified(true);
    }

    public boolean isVerified(String email, HttpSession session) {
        if (email == null || email.isBlank()) return false;
        email = email.trim();
        Map<String, VerificationInfo> map = getMap(session);
        VerificationInfo info = map.get(email);
        if (info == null) return false;
        if (info.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        return info.isVerified();
    }

    @Getter
    @Setter
    public static class VerificationInfo {
        private String code;
        private LocalDateTime lastSentAt;
        private LocalDateTime expiresAt;
        private boolean verified;
    }
}
