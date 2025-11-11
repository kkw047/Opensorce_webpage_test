package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.service.ClubService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ClubService clubService; // 기존 컨트롤러에서 이 메서드 호출하던 호환 위해 유지

    public CategoryController(CategoryRepository categoryRepository, ClubService clubService) {
        this.categoryRepository = categoryRepository;
        this.clubService = clubService;
    }

    record NameReq(String name) {}
    record CatDto(Long id, String name) {
        static CatDto of(Category c){ return new CatDto(c.getId(), c.getName()); }
    }

    @GetMapping
    public List<CatDto> list() {
        return categoryRepository.findAll().stream().map(CatDto::of).toList();
    }

    @PostMapping
    @Transactional
    public CatDto create(@RequestBody NameReq req) {
        Category c = clubService.createCategoryIfNotExists(req.name());
        return CatDto.of(c);
    }
}
