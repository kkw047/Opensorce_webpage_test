package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import com.cbnu11team.team11.web.dto.ClubForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ClubController {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;

    // ✅ 루트("/")는 HomeController가 처리하므로 여기서는 "/clubs"만 매핑
    @GetMapping("/clubs")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Club> clubs = clubRepository.findAllByOrderByIdDesc(PageRequest.of(page, 12));
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        List<String> regionDos = regionKorRepository.findDistinctDo();

        model.addAttribute("clubs", clubs);
        model.addAttribute("categories", categories);
        model.addAttribute("regionDos", regionDos);
        return "clubs/index";
    }

    // 모임 만들기 화면
    @GetMapping("/clubs/create")
    public String createForm(Model model) {
        model.addAttribute("form", new ClubForm());
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("regionDos", regionKorRepository.findDistinctDo());
        return "clubs/create";
    }

    // 모임 생성
    @PostMapping("/clubs")
    public String create(@ModelAttribute ClubForm form,
                         @RequestParam(name = "image", required = false) MultipartFile image,
                         @RequestParam(name = "categoryIds", required = false) List<Long> categoryIds) throws IOException {

        Club club = new Club();
        club.setName(form.getName());
        club.setDescription(form.getDescription());

        if (StringUtils.hasText(form.getRegionDo())) club.setRegionDo(form.getRegionDo());
        if (StringUtils.hasText(form.getRegionSi())) club.setRegionSi(form.getRegionSi());

        if (image != null && !image.isEmpty()) {
            club.setImageData(image.getBytes());
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Category> set = new HashSet<>(categoryRepository.findAllById(categoryIds));
            club.setCategories(set);
        } else {
            club.setCategories(new HashSet<>());
        }

        clubRepository.save(club);
        return "redirect:/clubs";
    }

    // 이미지 Base64 (템플릿에서 data URL로 쓰려면 사용)
    @GetMapping("/clubs/{id}/image")
    @ResponseBody
    public String clubImageBase64(@PathVariable Long id) {
        Club club = clubRepository.findById(id).orElseThrow();
        byte[] bytes = club.getImageData();
        if (bytes == null || bytes.length == 0) return "";
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
