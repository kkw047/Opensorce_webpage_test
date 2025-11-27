package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final CategoryRepository categoryRepository;

    /**
     * 프로필 설정 화면 (GET /profile)
     */
    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return "redirect:/clubs";
        }

        Optional<User> optUser = userService.findById(userId);
        if (optUser.isEmpty()) {
            return "redirect:/clubs";
        }

        User user = optUser.get();

        model.addAttribute("user", user);
        model.addAttribute("activeSidebarMenu", "profile");

        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);

        List<Long> userCategoryIds = user.getCategories()
                .stream()
                .map(Category::getId)
                .collect(Collectors.toList());


        model.addAttribute("userCategoryIds", userCategoryIds);
        model.addAttribute("selectedCategoryIds", Collections.emptyList());
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("q", null);

        return "profile";
    }

    /**
     * 프로필 저장 (POST /profile)
     */
    @PostMapping("/profile")
    public String updateProfile(@RequestParam("nickname") String nickname,
                                @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) {
            return "redirect:/clubs";
        }

        userService.updateProfile(userId, nickname, categoryIds);
        session.setAttribute("LOGIN_USER_NICKNAME", nickname);

        redirectAttributes.addFlashAttribute("msg", "프로필이 저장되었습니다.");
        return "redirect:/profile";
    }
}
