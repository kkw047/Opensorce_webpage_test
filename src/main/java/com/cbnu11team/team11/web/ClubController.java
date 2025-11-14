package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.ClubMemberId;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.web.dto.ClubForm;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberRepository clubMemberRepository;

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

    // 모임 상세 페이지 (홈 탭)
    @GetMapping("/{clubId}")
    public String detail(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        model.addAttribute("activeTab", "home");
        return "clubs/detail";
    }

    // 게시판 탭
    @GetMapping("/{clubId}/board")
    public String getBoardPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        model.addAttribute("activeTab", "board");
        return "clubs/board";
    }

    // 채팅 탭
    @GetMapping("/{clubId}/chat")
    public String getChatPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        model.addAttribute("activeTab", "chat");
        return "clubs/chat";
    }

    // 캘린더 탭
    @GetMapping("/{clubId}/calendar")
    public String getCalendarPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        model.addAttribute("activeTab", "calendar");
        return "clubs/calendar";
    }

    // 모임 생성
    @PostMapping
    public String create(@ModelAttribute ClubForm form,
                         HttpSession session,
                         RedirectAttributes ra) {

        Long ownerId = null;
        Object uid = session.getAttribute("LOGIN_USER_ID");
        if (uid instanceof Long id) {
            ownerId = id;
        }

        // 로그인 안 되어 있으면 경고 + 로그인 모달 열기
        if (ownerId == null) {
            ra.addFlashAttribute("error", "로그인 후 모임을 만들 수 있습니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs";
        }

        clubService.createClub(
                ownerId,
                form.name(),
                form.description(),
                form.regionDo(),
                form.regionSi(),
                form.categoryIds(),
                form.newCategoryName(),
                form.imageFile()
        );

        // 성공 토스트 메시지
        ra.addFlashAttribute("msg", "모임이 생성되었습니다.");
        return "redirect:/clubs";
    }

    /**
     * 상세/탭 페이지 공통 속성 추가 헬퍼 메소드
     * @return 클럽을 찾지 못하면 false 반환
     */
    private boolean addClubDetailAttributes(Long clubId, Model model, HttpSession session, RedirectAttributes ra) {
        Optional<Club> optClub = clubService.findById(clubId);
        if (optClub.isEmpty()) {
            ra.addFlashAttribute("error", "해당 모임을 찾을 수 없습니다.");
            return false;
        }

        Club club = optClub.get();
        model.addAttribute("club", club);

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        boolean isOwner = club.getOwner() != null && club.getOwner().getId().equals(currentUserId);
        model.addAttribute("isOwner", isOwner);

        boolean isAlreadyMember = false;
        if (currentUserId != null) {
            ClubMemberId memberId = new ClubMemberId(clubId, currentUserId);
            isAlreadyMember = clubMemberRepository.existsById(memberId);
        }
        model.addAttribute("isAlreadyMember", isAlreadyMember);

        model.addAttribute("members", club.getMembers());
        model.addAttribute("memberCount", club.getMembers().size());

        // --- 프래그먼트(사이드바, 검색바)용 공통 데이터 ---
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("selectedCategoryIds", List.of());
        model.addAttribute("q", null);

        return true;
    }
}
