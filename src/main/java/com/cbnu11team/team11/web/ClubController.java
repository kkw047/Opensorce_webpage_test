package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ClubController {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;

    // 메인 (좌측 메뉴 + 검색 + 우측 카드)
    @GetMapping({"/", "/clubs"})
    public String index(Model model) {
        // 초기 로딩 데이터
        List<Category> categories = categoryRepository.findAll();
        List<String> dos = regionKorRepository.findAllDistinctDo();
        List<Club> clubs = clubRepository.search(null, null, null, false, Collections.emptyList(), PageRequest.of(0, 20));

        model.addAttribute("categories", categories);
        model.addAttribute("dos", dos);
        model.addAttribute("clubs", clubs);
        return "clubs/index";
    }

    // 이미지 스트리밍 (Base64 금지 → Thymeleaf의 static access 에러 회피)
    @ResponseBody
    @GetMapping(value = "/clubs/{id}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] image(@PathVariable Long id) {
        return clubRepository.findById(id)
                .map(Club::getImageData)
                .orElse(new byte[0]);
    }

    // 우측 모임 카드 데이터(필터 결과)를 AJAX로 내려줌
    @ResponseBody
    @GetMapping("/api/clubs")
    public List<Map<String, Object>> listApi(
            @RequestParam(required = false) String regionDo,
            @RequestParam(required = false) String regionSi,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "cat") List<Long> categoryIds
    ) {
        boolean hasCats = !CollectionUtils.isEmpty(categoryIds);
        List<Club> result = clubRepository.search(
                blankToNull(regionDo),
                blankToNull(regionSi),
                blankToNull(keyword),
                hasCats,
                hasCats ? categoryIds : Collections.emptyList(),
                PageRequest.of(0, 50)
        );

        return result.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("description", c.getDescription());
            m.put("regionDo", c.getRegionDo());
            m.put("regionSi", c.getRegionSi());
            m.put("imageUrl", "/clubs/" + c.getId() + "/image");
            m.put("categories", c.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
    }

    private String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
