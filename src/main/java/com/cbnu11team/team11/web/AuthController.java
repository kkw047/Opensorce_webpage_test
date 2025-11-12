package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.UserService;
import com.cbnu11team.team11.web.dto.LoginRequest;
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

    // 단독 페이지(모달 대신 별도 접근용)
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest req,
                        HttpSession session,
                        RedirectAttributes ra) {
        var opt = userService.findByEmailOrLoginId(req.loginIdOrEmail());
        if (opt.isPresent() && userService.checkPassword(opt.get(), req.password())) {
            User u = opt.get();
            session.setAttribute("LOGIN_USER_ID", u.getId());
            session.setAttribute("LOGIN_USER_NICKNAME", u.getNickname());
            return "redirect:/clubs";
        }
        // 실패 시 메인으로 되돌리고 로그인 모달 자동 오픈 + 토스트
        ra.addFlashAttribute("error", "아이디/이메일 또는 비밀번호가 올바르지 않습니다.");
        ra.addFlashAttribute("openLogin", true);
        return "redirect:/clubs";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest req, RedirectAttributes ra) {
        try {
            userService.register(
                    req.loginId(), req.email(), req.password(),
                    req.nickname(), req.regionDo(), req.regionSi(), req.categoryIds()
            );
            // 성공: 메인으로 이동 + 성공 토스트
            ra.addFlashAttribute("msg", "가입이 완료되었습니다. 로그인해 주세요.");
            ra.addFlashAttribute("openLogin", true); // 바로 로그인 모달 열기
            return "redirect:/clubs";
        } catch (IllegalArgumentException ex) {
            // 실패: 메인으로 이동 + 회원가입 모달 자동 오픈 + 에러 토스트
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("openRegister", true);
            return "redirect:/clubs";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("msg", "로그아웃 되었습니다.");
        return "redirect:/clubs";
    }
}
