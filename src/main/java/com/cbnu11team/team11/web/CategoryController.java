package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Controller
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @ResponseBody
    public List<Category> list() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestParam("name") String name) {
        String raw = name == null ? "" : name.trim();
        if (raw.isEmpty()) return ResponseEntity.badRequest().body("카테고리명이 비어있음");

        if (categoryRepository.existsByNameIgnoreCase(raw)) {
            return ResponseEntity.status(409).body("이미 존재하는 카테고리");
        }

        Category saved = categoryRepository.save(new Category(raw));
        return ResponseEntity.created(URI.create("/api/categories/" + saved.getId())).build();
    }
}
