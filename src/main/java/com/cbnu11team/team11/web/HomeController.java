package com.cbnu11team.team11.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 루트는 항상 /clubs 로 보냄
    @GetMapping("/")
    public String home() {
        return "redirect:/clubs";
    }
}
