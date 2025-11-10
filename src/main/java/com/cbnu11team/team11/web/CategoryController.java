package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    // 좌측 & 모임 만들기 폼 채우기
    @GetMapping
    public List<Category> list() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    // 새로운 카테고리 추가 (+카테고리)
    @PostMapping
    public Category create(@RequestParam String name) {
        String n = name.trim();
        if (n.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름이 비었습니다.");
        if (categoryRepository.existsByNameIgnoreCase(n))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 카테고리");

        return categoryRepository.save(new Category(n)); // Category(String name) 생성자 사용
    }
}
