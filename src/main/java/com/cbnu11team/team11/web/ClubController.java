package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs")
class ClubApiController {

    private final ClubService clubService;
    private final FileStorageService storage;

    @GetMapping
    @Transactional(readOnly = true) // LazyInitializationException 방지
    public Page<ClubDtos.ClubCard> search(
            @RequestParam(value = "rdo", required = false) String rdo,
            @RequestParam(value = "rsi", required = false) String rsi,
            @RequestParam(value = "kw", required = false) String kw,
            @RequestParam(value = "cats", required = false) List<Long> cats,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Club> p = clubService.search(
                rdo,
                rsi,
                StringUtils.hasText(kw) ? kw.trim() : null,
                cats,
                pageable
        );
        return p.map(ClubDtos::toCard);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> create(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam("regionDo") String regionDo,
            @RequestParam("regionSi") String regionSi,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            @RequestParam(value = "newCategoryNames", required = false) List<String> newCategoryNames,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        String imageUrl = (image != null && !image.isEmpty()) ? storage.store(image) : null;
        Club c = clubService.createClub(name, description, regionDo, regionSi, categoryIds, imageUrl, newCategoryNames);
        return ResponseEntity.ok(Map.of("id", c.getId()));
    }
}

@Controller
@RequiredArgsConstructor
class ClubPageController {

    private final ClubService clubService;

    // 메인 페이지
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        return "clubs/index";
    }

    // 우측 패널로 로드되는 모임 만들기 폼
    @GetMapping("/clubs/create")
    public String createForm(Model model) {
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        return "clubs/create";
    }
}
