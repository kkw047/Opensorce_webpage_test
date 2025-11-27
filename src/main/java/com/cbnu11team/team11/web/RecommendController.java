package com.cbnu11team.team11.web;

import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import com.cbnu11team.team11.service.RecommendService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;

@Controller
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;

    @GetMapping("/recommend")
    public String recommend(HttpSession session, Model model) {

        Long loginUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        model.addAttribute("recommendedClubs",
                recommendService.getRecommendedClubs(loginUserId));
        model.addAttribute("popularClubs",
                recommendService.getPopularClubs(loginUserId));
        model.addAttribute("activeClubs",
                recommendService.getActiveClubs(loginUserId));

        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("dos", regionKorRepository.findDistinctDos());
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("q", null);
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("selectedCategoryIds", Collections.emptyList());

        model.addAttribute("activeSidebarMenu", "recommend");

        return "clubs/recommend";
    }
}
