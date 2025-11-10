package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.club.Club;
import com.cbnu11team.team11.domain.club.ClubRepository;
import com.cbnu11team.team11.domain.category.Category;
import com.cbnu11team.team11.domain.category.CategoryRepository;
import com.cbnu11team.team11.domain.region.RegionKorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 클럽 목록/검색 화면 컨트롤러
 * 주의: 이 컨트롤러는 "/"를 매핑하지 않습니다. (HomeController가 루트를 전담)
 */
@Controller
@RequestMapping("/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository; // distinct do 리스트용

    /**
     * 목록 페이지
     * 기존 로그 패턴에 맞춰 단순 페이지네이션 + 정렬만 적용 (필터 로직은 필요 시 추가)
     */
    @GetMapping
    public String index(
            int page,
            int size,
            String regionDo,
            String regionSi,
            String category,
            List<String> keywords,
            Model model
    ) {
        // 기본값 보정
        int p = Math.max(page, 0);
        int s = (size <= 0) ? 10 : size;

        // 단순 정렬(id desc) + 페이징
        Page<Club> clubsPage = clubRepository.findAll(
                PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"))
        );

        // 사이드 필터용 데이터
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        List<String> regionDoList = regionKorRepository.findDistinctRegionDo();

        model.addAttribute("clubsPage", clubsPage);
        model.addAttribute("categories", categories);
        model.addAttribute("regionDoList", regionDoList);

        // 뷰에서 네비 active 처리용
        model.addAttribute("activeMenu", "clubs");

        // 현재 검색 파라미터(뷰에서 유지/표시용)
        model.addAttribute("page", p);
        model.addAttribute("size", s);
        model.addAttribute("regionDo", regionDo);
        model.addAttribute("regionSi", regionSi);
        model.addAttribute("category", category);
        model.addAttribute("keywords", keywords);

        return "clubs/index";
    }
}
