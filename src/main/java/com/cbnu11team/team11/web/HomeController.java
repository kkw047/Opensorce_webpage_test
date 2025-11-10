package com.cbnu11team.team11.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * 루트("/") 진입 시 클럽 목록으로 일원화
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/clubs";
    }
}
