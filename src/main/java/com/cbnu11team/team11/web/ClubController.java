package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.RegionKor;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 모임(클럽) 목록 화면
 * - 루트("/")는 HomeController가 /clubs로 리다이렉트
 * - 여기서는 /clubs GET만 처리
 */
@Controller
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;

    @GetMapping
    public String index(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "regionDo", required = false) String regionDo,
            @RequestParam(name = "regionSi", required = false) String regionSi,
            @RequestParam(name = "category", required = false) String category, // 쿼리스트링은 문자열이므로 String으로 받고 아래서 Long 변환
            @RequestParam(name = "keywords", required = false) String keywords,
            Model model
    ) {
        int p = Math.max(page, 0);
        int s = size > 0 ? size : 12;

        // 단순 목록 (정렬: id desc)
        Page<Club> clubsPage = clubRepository.findAll(
                PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"))
        );

        // 카테고리(이름 오름차순)
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        // 지역(도) 목록: 리포지토리 커스텀 메서드에 의존하지 않고 전체 로드 후 distinct/sort
        List<String> regionDoList = regionKorRepository.findAll().stream()
                .map(RegionKor::getRegionDo)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        // 선택된 카테고리 id(Long) 파싱 (템플릿에서 selected 비교용)
        Long categoryId = null;
        if (category != null && !category.isBlank()) {
            try {
                categoryId = Long.parseLong(category.trim());
            } catch (NumberFormatException ignore) {
                categoryId = null;
            }
        }

        model.addAttribute("clubsPage", clubsPage);
        model.addAttribute("categories", categories);
        model.addAttribute("regionDoList", regionDoList);

        // 네비 active 표시용
        model.addAttribute("activeMenu", "clubs");

        // 현재 파라미터(뷰 유지/표시용)
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("regionDo", regionDo == null ? "" : regionDo);
        model.addAttribute("regionSi", regionSi == null ? "" : regionSi);
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keywords", keywords == null ? "" : keywords);

        return "clubs/index";
    }
}
