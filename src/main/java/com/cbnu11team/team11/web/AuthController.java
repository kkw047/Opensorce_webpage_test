package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.service.UserService;
import com.cbnu11team.team11.web.dto.LoginRequest;
import com.cbnu11team.team11.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    // 아이디 중복 체크 (AJAX)
    @GetMapping("/check-id")
    @ResponseBody
    public Map<String, Object> checkId(@RequestParam String loginId) {
        boolean exists = userService.isDuplicatedLoginId(loginId);
        return Map.of("exists", exists);
    }

    // 로그인
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("form", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("form") LoginRequest form,
                        BindingResult bindingResult,
                        HttpSession session,
                        RedirectAttributes ra) {
        if (bindingResult.hasErrors()) return "login";

        Optional<User> userOpt = userService.authenticate(form.getLoginId(), form.getPassword());
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "redirect:/auth/login";
        }
        session.setAttribute("LOGIN_USER_ID", userOpt.get().getId());
        return "redirect:/";
    }

    // 회원가입
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form,
                           BindingResult bindingResult,
                           RedirectAttributes ra) {
        if (bindingResult.hasErrors()) return "register";

        userService.register(form.getLoginId(), form.getPassword());
        ra.addFlashAttribute("msg", "회원가입 완료. 로그인해주세요.");
        return "redirect:/auth/login";
    }

    // 로그아웃
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
