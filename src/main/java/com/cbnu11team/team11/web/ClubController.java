package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.ClubMemberId;
import com.cbnu11team.team11.domain.Post; // 추가함
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.PostService; // 추가함
import com.cbnu11team.team11.web.dto.ClubForm;
import com.cbnu11team.team11.web.dto.PostForm; // 추가함
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid; // 추가함
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // 추가함
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberRepository clubMemberRepository;
    private final PostService postService; // 의존성 추가

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
        // A. 기존 헬퍼 메서드 호출 (클럽 정보, 멤버 정보 등 로드)
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        // B. (BoardController의 로직) 게시물 목록 조회
        List<Post> postList = postService.getPostsByClubId(clubId);
        model.addAttribute("postList", postList);

        // C. 기존 로직 (탭 활성화)
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

    // 글쓰기 폼 메서드 추가 (추가됨)
    @GetMapping("/{clubId}/board/new")
    public String getNewPostForm(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        // A. 클럽 정보 등이 필요하므로 헬퍼 메서드 우선 호출
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        // B. 글쓰기 폼에 필요한 데이터 추가
        model.addAttribute("postForm", new PostForm("", ""));
        model.addAttribute("clubId", clubId); // 폼 action URL에 사용됨

        return "post_new";
    }

    // 글쓰기 처리 메서드 추가 (추가됨)
    @PostMapping("/{clubId}/board/new")
    public String createPost(@PathVariable Long clubId,
                             @Valid @ModelAttribute("postForm") PostForm postForm,
                             BindingResult bindingResult,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) { // 헬퍼 메서드용 ra 추가

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            // 폼에 에러가 있으면, 폼 페이지를 다시 렌더링해야 함
            // 폼 페이지 렌더링에 필요한 클럽 정보 등을 다시 로드
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            // postForm은 @ModelAttribute에 의해 자동으로 모델에 추가됨
            return "post_new";
        }

        try {
            postService.createPost(clubId, postForm, currentUserId);
        } catch (Exception e) {
            // 예외 발생 시, 다시 폼으로 돌려보냄
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            model.addAttribute("errorMessage", "게시물 등록 중 오류가 발생했습니다: " + e.getMessage());
            return "post_new";
        }

        // 성공 시, 게시판 목록으로 리다이렉트
        return "redirect:/clubs/" + clubId + "/board";
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

    @GetMapping("/{clubId}/board/{postId}")
    public String getPostDetail(
            @PathVariable Long clubId,
            @PathVariable Long postId, // 1. postId 파라미터 받기
            Model model,
            RedirectAttributes ra,
            HttpSession session) {

        // 2. 헬퍼 메서드로 클럽/사이드바/탭 정보 로드
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        // 3. PostService로 게시물 1건 조회
        Optional<Post> optPost = postService.findPostById(postId);

        // 4. 게시물이 없거나, 해당 클럽의 게시물이 아닐 경우
        if (optPost.isEmpty() || !optPost.get().getClub().getId().equals(clubId)) {
            ra.addFlashAttribute("error", "게시물을 찾을 수 없거나 접근 권한이 없습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }

        // 5. 모델에 게시물 데이터 추가
        model.addAttribute("post", optPost.get());
        model.addAttribute("activeTab", "board"); // 게시판 탭 활성화 유지

        // 6. 1단계에서 만든 템플릿 반환
        return "clubs/post_detail";
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
