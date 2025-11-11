package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.service.ClubService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IdName create(@RequestBody IdName req) {
        Category c = clubService.createCategoryIfNotExists(req.getName());
        return new IdName(c.getId(), c.getName());
    }

    @Data @AllArgsConstructor
    public static class IdName {
        public IdName() {}
        private Long id;
        private String name;
    }
}
