package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.UserService;
import com.cbnu11team.team11.web.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 아이디 중복검사: /auth/check-id?loginId=xxx
    @GetMapping("/check-id")
    @ResponseBody
    public ResponseEntity<Boolean> checkDuplicate(@RequestParam("loginId") String loginId) {
        boolean exists = userService.isDuplicateLoginId(loginId.trim());
        return ResponseEntity.ok(exists);
    }

    // 회원가입 POST
    @PostMapping("/register")
    public ResponseEntity<?> register(@ModelAttribute RegisterRequest req) {
        userService.register(req);
        return ResponseEntity.ok().build();
    }
}
