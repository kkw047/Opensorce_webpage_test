package com.cbnu11team.team11.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CalendarController {

    @GetMapping("/my-calendar")
    public String calendarPage(HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인 후 이용해 주세요.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs";
        }

        model.addAttribute("activeSidebarMenu", "calendar");

        // [수정된 부분] 경로 변경: "calendar/my_calendar" -> "clubs/my_calendar"
        return "clubs/my_calendar";
    }
}