package com.cbnu11team.opensource11.club;

import com.cbnu11team.opensource11.club.dto.CategoryView;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final ClubRepository clubRepository;

    public CategoryService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    /** 좌측 사이드바에 뿌릴 카테고리 요약 (카테고리별 개수) */
    public List<CategoryView> getCategorySummary() {
        // 전체 로드 후 category별 count 집계 (간단 구현)
        Map<String, Long> map = clubRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        c -> Optional.ofNullable(c.getCategory()).orElse("기타"),
                        Collectors.counting()
                ));

        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new CategoryView(e.getKey(), e.getKey(), e.getValue()))
                .toList();
    }
}
