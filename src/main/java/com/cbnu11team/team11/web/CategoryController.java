package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final ClubService clubService;
    private final CategoryRepository categoryRepository;

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

    @DeleteMapping("/api/categories/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("잘못된 카테고리 ID 입니다.");
        }

        try {
            if (!categoryRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            categoryRepository.deleteById(id);
            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("이미 사용 중인 카테고리는 삭제할 수 없습니다.");
        }
    }

    /* DTOs */
    public record NewCategoryReq(String name) {}
    public record CategoryDto(Long id, String name) {}
}
