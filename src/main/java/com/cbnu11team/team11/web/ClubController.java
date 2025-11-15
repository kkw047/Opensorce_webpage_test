package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.ClubMemberId;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.web.dto.ClubDetailDto;
import com.cbnu11team.team11.web.dto.ClubForm;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberRepository clubMemberRepository; // 세션 체크 헬퍼용으로 유지

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
        model.addAttribute("activeSidebarMenu", "main");
        model.addAttribute("searchActionUrl", "/clubs"); // 검색창이 요청할 URL
        model.addAttribute("memberCounts", LoadMemberCounts(result.getContent()));

        return "clubs/index";
    }

    // 내 모임 페이지를 위한 핸들러
    @GetMapping("/myclubs")
    public String myClubsPage(@RequestParam(value = "q", required = false) String q,
                              @RequestParam(value = "do", required = false) String regionDo,
                              @RequestParam(value = "si", required = false) String regionSi,
                              @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
                              @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                              @RequestParam(value = "size", required = false, defaultValue = "12") int size,
                              HttpSession session,
                              Model model,
                              RedirectAttributes ra) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인 후 '내 모임'을 볼 수 있습니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs"; // 로그인 안했으면 메인 페이지로
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // 검색 파라미터를 포함하여 서비스 호출
        Page<Club> myClubsPage = clubService.searchMyClubs(currentUserId, q, regionDo, regionSi, categoryIds, pageable);

        // clubs/index.html 템플릿이 요구하는 모든 속성
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());

        // 검색 파라미터를 모델에 다시 추가 (검색창 값 유지를 위해)
        model.addAttribute("selectedDo", regionDo);
        model.addAttribute("selectedSi", regionSi);
        model.addAttribute("selectedCategoryIds", categoryIds == null ? List.of() : categoryIds);
        model.addAttribute("q", q);

        // 페이지 데이터
        model.addAttribute("page", myClubsPage);

        // 사이드바 활성화를 위한 속성
        model.addAttribute("activeSidebarMenu", "myclubs");
        model.addAttribute("searchActionUrl", "/clubs/myclubs"); // 검색창이 요청할 URL
        model.addAttribute("memberCounts", LoadMemberCounts(myClubsPage.getContent()));

        return "clubs/index"; // 메인 템플릿 재사용
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

    // 모임 가입
    @PostMapping("/{clubId}/join")
    public String joinClub(@PathVariable Long clubId, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        // 로그인 여부 확인
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인 후 가입할 수 있습니다.");
            ra.addFlashAttribute("openLogin", true); // 로그인 모달 바로 열기
            return "redirect:/clubs/" + clubId; // 현재 상세 페이지로 리다이렉트
        }

        // 서비스 로직 호출
        try {
            clubService.joinClub(clubId, currentUserId);
            ra.addFlashAttribute("msg", "모임에 가입되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 이미 가입했거나, 유저/모임 ID가 잘못된 경우
            ra.addFlashAttribute("error", e.getMessage());
        }

        // 결과 페이지로 리다이렉트
        return "redirect:/clubs/" + clubId;
    }

    // 모임 생성
    @PostMapping
    public String create(@ModelAttribute ClubForm form,
                         HttpSession session,
                         RedirectAttributes ra) {

        Long ownerId = (Long) session.getAttribute("LOGIN_USER_ID");

        // 로그인 안 되어 있으면 경고 + 로그인 모달 열기
        if (ownerId == null) {
            ra.addFlashAttribute("error", "로그인 후 모임을 만들 수 있습니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs";
        }

        // 서비스 호출 시 DTO 전달
        clubService.createClub(ownerId, form);

        // 성공 토스트 메시지
        ra.addFlashAttribute("msg", "모임이 생성되었습니다.");
        return "redirect:/clubs";
    }

    /**
     * 상세/탭 페이지 공통 속성 추가 헬퍼 메소드
     * @return 클럽을 찾지 못하면 false 반환
     */
    private boolean addClubDetailAttributes(Long clubId, Model model, HttpSession session, RedirectAttributes ra) {
        // 현재 사용자 ID 가져오기
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        // 서비스 계층에서 DTO를 조회
        Optional<ClubDetailDto> optDto = clubService.getClubDetail(clubId, currentUserId);

        // DTO가 없는 경우 (모임이 없는 경우)
        if (optDto.isEmpty()) {
            ra.addFlashAttribute("error", "해당 모임을 찾을 수 없습니다.");
            return false;
        }

        // DTO를 모델에 추가
        ClubDetailDto dto = optDto.get();
        model.addAttribute("club", dto); // 엔티티(Club) 대신 DTO(ClubDetailDto)를 전달

        // DTO에 이미 포함된 정보는 뷰에서 ${club.isOwner}, ${club.isAlreadyMember} 등으로 접근
        model.addAttribute("memberCount", dto.members().size());
        model.addAttribute("isOwner", dto.isOwner());
        model.addAttribute("isAlreadyMember", dto.isAlreadyMember());

        // --- 프래그먼트(사이드바, 검색바)용 공통 데이터 ---
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("selectedCategoryIds", List.of());
        model.addAttribute("q", null);
        model.addAttribute("searchActionUrl", "/clubs");

        return true;
    }

    private Map<Long, Long> LoadMemberCounts(List<Club> clubs)
    {
        if(clubs == null || clubs.isEmpty()) return Map.of();

        List<Long> ids = clubs.stream().map(Club::getId).filter(Objects::nonNull).collect(Collectors.toList());

        if (ids.isEmpty()) return Map.of();

        var rows = clubMemberRepository.countMembersByClubIds(ids);
        Map<Long, Long> out = new HashMap<>();
        for (var r : rows) {
            out.put(r.getClubId(), r.getCnt());
        }

        for (Long id : ids) {
            out.putIfAbsent(id, 0L);
        }
        return out;
    }

}