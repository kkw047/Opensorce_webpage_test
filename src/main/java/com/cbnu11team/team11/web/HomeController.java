package com.cbnu11team.team11.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping({"/", "/home"})
    public String home() {
        // 메인은 /clubs 로 통일
        return "redirect:/clubs";
    }
}
