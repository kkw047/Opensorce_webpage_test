package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final ClubService clubService;

    /* 새 카테고리 생성(들) – name 에 "게임, 공부, 독서" 처럼 쉼표로 여러개 가능
       반환: [{id:1,name:"게임"}, {id:2,name:"공부"}, ...]  */
    @PostMapping("/api/categories")
    public ResponseEntity<List<CategoryDto>> create(@RequestBody NewCategoryReq req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // 쉼표 분리 + 트림 + 공백제거 + (대소문자 무시) 중복 제거(입력 내 중복 방지)
        LinkedHashSet<String> names = Arrays.stream(req.name().split(","))
                .map(s -> s.trim().replaceAll("\\s+", " "))
                .filter(s -> !s.isBlank())
                .map(s -> s.length() > 50 ? s.substring(0, 50) : s)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CategoryDto> out = new ArrayList<>();
        for (String n : names) {
            Category c = clubService.createCategoryIfNotExists(n);
            out.add(new CategoryDto(c.getId(), c.getName()));
        }
        return ResponseEntity.ok(out);
    }

    /* DTOs */
    public record NewCategoryReq(String name) {}
    public record CategoryDto(Long id, String name) {}
}
