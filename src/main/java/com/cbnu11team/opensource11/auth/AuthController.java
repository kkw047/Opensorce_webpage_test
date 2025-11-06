package com.cbnu11team.opensource11.auth;

import com.cbnu11team.opensource11.user.User;
import com.cbnu11team.opensource11.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo; this.encoder = encoder;
    }

    @GetMapping("/auth/login")
    public String loginPage() { return "auth/login"; }

    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form,
                           BindingResult binding) {
        if (binding.hasErrors()) return "auth/register";
        if (repo.existsByUsername(form.getUsername())) {
            binding.rejectValue("username", "dup", "이미 가입된 이메일입니다.");
            return "auth/register";
        }
        User u = new User();
        u.setUsername(form.getUsername());
        u.setPassword(encoder.encode(form.getPassword()));
        u.setName(form.getName());
        u.setRole("USER");
        repo.save(u);
        return "redirect:/auth/login";
    }
}
