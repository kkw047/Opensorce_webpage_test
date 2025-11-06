package com.cbnu11team.opensource11;

import com.cbnu11team.opensource11.club.ClubService;
import com.cbnu11team.opensource11.club.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final ClubService clubService;
    private final CategoryService categoryService;

    public HomeController(ClubService clubService, CategoryService categoryService) {
        this.clubService = clubService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String categoryId,
                        @RequestParam(required = false) String q,
                        Model model) {

        model.addAttribute("categories", categoryService.getCategorySummary());
        model.addAttribute("activeCategoryId", categoryId);
        model.addAttribute("q", q);

        model.addAttribute("clubs", clubService.findClubs(categoryId, q));
        return "index";
    }
}
