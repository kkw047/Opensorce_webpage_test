package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final ClubService clubService;

    public record CreateCategoryRequest(String name) {}

    @PostMapping
    public String create(@ModelAttribute CreateCategoryRequest req) {
        Category c = clubService.createCategoryIfNotExists(req.name());
        return "redirect:/clubs";
    }
}
