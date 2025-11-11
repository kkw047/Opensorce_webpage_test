package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.service.ClubService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;

    // 메인 목록 + 필터
    @GetMapping
    public String index(@RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "kw", required = false) String kw,
                        @RequestParam(value = "rdo", required = false) String rdo,
                        @RequestParam(value = "rsi", required = false) String rsi,
                        @RequestParam(value = "cats", required = false) List<Long> cats,
                        Model model) {

        PageRequest pageable = PageRequest.of(page, 12);

        Page<Club> clubs = (kw != null || rdo != null || rsi != null || (cats != null && !cats.isEmpty()))
                ? clubService.search(rdo, rsi, kw, cats, pageable)
                : clubService.list(pageable);

        List<Category> categories = clubService.findAllCategories();

        model.addAttribute("clubs", clubs);
        model.addAttribute("categories", categories);

        // 뷰에서 선택값 복원에 사용
        model.addAttribute("kw", kw);
        model.addAttribute("rdo", rdo);
        model.addAttribute("rsi", rsi);
        model.addAttribute("cats", cats); // ★ null일 수 있으니 뷰에서 null-safe 처리

        return "clubs/index";
    }

    // 생성 폼
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("form", new ClubForm());
        model.addAttribute("categories", clubService.findAllCategories());
        return "clubs/create";
    }

    // 생성 처리
    @PostMapping("/create")
    public String create(@ModelAttribute("form") ClubForm form,
                         @RequestParam(value = "image", required = false) MultipartFile image) {

        // (선택) 이미지 저장 구현 필요 시 여기에. 지금은 경로만 null로 둔다.
        String imagePath = null;

        clubService.createClub(
                form.getName(),
                form.getDescription(),
                form.getRegionDo(),
                form.getRegionSi(),
                form.getCategoryIds(),
                imagePath
        );
        return "redirect:/clubs?created=1";
    }

    // ====== 지역 API (하드코딩 X, DB region_kor 사용) ======
    @ResponseBody
    @GetMapping(value = "/api/regions/do", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> apiDos() {
        return clubService.getAllDos();
    }

    @ResponseBody
    @GetMapping(value = "/api/regions/si", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> apiSis(@RequestParam("do") String regionDo) {
        return StringUtils.hasText(regionDo) ? clubService.getSisByDo(regionDo) : Collections.emptyList();
    }

    // 폼 DTO (새 파일 추가 없이 내부 클래스로 둠)
    @Data
    public static class ClubForm {
        private String name;
        private String description;
        private String regionDo;
        private String regionSi;
        private List<Long> categoryIds;
    }
}
