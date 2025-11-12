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

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClubService clubService;

    // 팝업을 별도 페이지로 열고 싶을 때 사용 가능 (메인에 모달도 제공됨)
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest req, HttpSession session) {
        var opt = userService.findByEmailOrLoginId(req.loginIdOrEmail());
        if (opt.isPresent() && userService.checkPassword(opt.get(), req.password())) {
            User u = opt.get();
            session.setAttribute("LOGIN_USER_ID", u.getId());
            session.setAttribute("LOGIN_USER_NICKNAME", u.getNickname());
            return "redirect:/clubs";
        }
        return "redirect:/login?error";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest req) {
        userService.register(req.loginId(), req.email(), req.password(),
                req.nickname(), req.regionDo(), req.regionSi(), req.categoryIds());
        return "redirect:/login?registered";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/clubs";
    }
}
