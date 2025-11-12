package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.web.dto.ClubForm;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final UserRepository userRepository;

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "do", required = false) String regionDo,
                        @RequestParam(value = "si", required = false) String regionSi,
                        @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                        @RequestParam(value = "size", required = false, defaultValue = "12") int size,
                        Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = clubService.search(q, regionDo, regionSi, categoryIds, pageable);

        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("selectedDo", regionDo);
        model.addAttribute("selectedSi", regionSi);
        model.addAttribute("selectedCategoryIds", categoryIds == null ? List.of() : categoryIds);
        model.addAttribute("q", q);
        model.addAttribute("page", result);

        return "clubs/index";
    }

    @PostMapping
    public String create(@ModelAttribute ClubForm form, HttpSession session) {
        User owner = null;
        Object uid = session.getAttribute("LOGIN_USER_ID");
        if (uid instanceof Long id) {
            owner = userRepository.findById(id).orElse(null);
        }

        clubService.createClub(owner,
                form.name(),
                form.description(),
                form.regionDo(),
                form.regionSi(),
                form.categoryIds(),
                form.newCategoryName(),
                form.imageFile());

        // 생성 후, 현재 필터 유지 없이 메인으로 이동
        return "redirect:/clubs";
    }
}
