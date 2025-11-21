package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.ClubService; // [추가]
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final ClubService clubService; // [추가] 카테고리 정보를 가져오기 위해 필요

    @GetMapping("/my-calendar")
    public String calendarPage(HttpSession session, Model model, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (userId == null) {
            ra.addFlashAttribute("error", "로그인 후 이용해 주세요.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs";
        }

        // 1. 사이드바 활성화 표시
        model.addAttribute("activeSidebarMenu", "calendar");

        // 2. [핵심 수정] 사이드바 구성을 위한 데이터 추가 (메인 컨트롤러와 동일하게 맞춤)
        model.addAttribute("categories", clubService.findAllCategories()); // 카테고리 목록
        model.addAttribute("dos", clubService.getAllDos());              // 지역 목록 (필요 시)

        // 3. 빈 선택값 처리 (오류 방지용)
        model.addAttribute("selectedCategoryIds", List.of());
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("q", null);
        model.addAttribute("searchActionUrl", "/clubs"); // 검색 시 메인으로 이동하게 설정

        return "clubs/my_calendar";
    }
}