package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final ClubService clubService;

    @GetMapping
    public List<Category> list() {
        return clubService.findAllCategories();
    }

    // 모임 만들기에서만 사용(프론트 제어)
    @PostMapping
    public Category create(@RequestBody Category req) {
        return clubService.createCategoryIfNotExists(req.getName());
    }
}
