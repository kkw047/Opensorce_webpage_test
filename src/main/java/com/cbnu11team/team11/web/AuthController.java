package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.UserService;
import com.cbnu11team.team11.service.EmailVerificationService;
import com.cbnu11team.team11.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClubService clubService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 로그인 페이지 (GET)
     * SecurityConfig에서 failureHandler가 에러 메시지를 이쪽으로 보냅니다.
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "exception", required = false) String exception,
            Model model) {

        // 로그인 실패 시 에러 메시지를 모델에 담아 HTML로 전달
        if (error != null) {
            model.addAttribute("errorMessage", exception);
        }
        return "login";
    }

    /**
     * [삭제됨] 로그인 처리 (POST)
     * 이유: Spring Security가 낚아채서 처리하므로 컨트롤러에는 없어야 합니다.
     */

    /**
     * [삭제됨] 로그아웃 (POST)
     * 이유: Spring Security가 /logout 요청을 자동으로 처리합니다.
     */

    // 회원가입 페이지 (GET)
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        return "register";
    }

    // 회원가입 처리 (POST)
    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest req, RedirectAttributes ra, HttpSession session) {
        try {
            boolean verified = emailVerificationService.isVerified(req.email(), session);
            if (!verified) {
                ra.addFlashAttribute("error", "이메일 인증을 먼저 완료해 주세요.");
                return "redirect:/register"; // 다시 가입 페이지로
            }

            userService.register(
                    req.loginId(), req.email(), req.password(),
                    req.nickname(), req.regionDo(), req.regionSi(), req.categoryIds()
            );

            // 성공 시 로그인 페이지로 이동
            ra.addFlashAttribute("msg", "가입이 완료되었습니다. 로그인해 주세요.");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            // 실패 시 다시 가입 페이지로 (에러 메시지 포함)
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
    }
}