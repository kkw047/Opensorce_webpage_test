package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.ClubMemberId;
import com.cbnu11team.team11.domain.Post;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.PostService;
import com.cbnu11team.team11.web.dto.ClubForm;
import com.cbnu11team.team11.web.dto.PostForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberRepository clubMemberRepository;
    private final PostService postService;

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
        List<Post> postList = postService.getPostsByClubId(clubId);
        model.addAttribute("postList", postList);

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

    //새 게시물 작성 폼 조회
    @GetMapping("/{clubId}/board/new")
    public String getNewPostForm(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {

        //세션에서 사용자 ID 가져오기
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        //로그인이 안 되어있는 경우
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "글을 작성하려면 로그인이 필요합니다.");
            ra.addFlashAttribute("openLogin", true);

            //글쓰기 폼 대신 게시판 목록 페이지로 리다이렉트
            return "redirect:/clubs/" + clubId + "/board";
        }
        // 클럽 정보/사이드바 정보 등을 로드하기 위해 헬퍼 메서드 호출
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        // 폼 바인딩을 위한 DTO 및 clubId 추가
        model.addAttribute("postForm", new PostForm("", ""));
        model.addAttribute("clubId", clubId);

        return "post_new"; // 템플릿: /templates/post_new.html
    }

    //새 게시물 등록 처리
    @PostMapping("/{clubId}/board/new")
    public String createPost(@PathVariable Long clubId,
                             @Valid @ModelAttribute("postForm") PostForm postForm,
                             BindingResult bindingResult,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return "redirect:/login"; // 로그인 안했으면 로그인 페이지로
        }

        // 폼 유효성 검사 실패 시
        if (bindingResult.hasErrors()) {
            // 폼 페이지를 다시 보여주기 위해 클럽 정보 로드
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            // postForm은 @ModelAttribute가 자동으로 다시 model에 넣어줌
            return "post_new";
        }

        try {
            // 서비스 호출하여 게시물 저장
            postService.createPost(clubId, postForm, currentUserId);
        } catch (Exception e) {
            // 저장 중 예외 발생 시
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            model.addAttribute("errorMessage", "게시물 등록 중 오류가 발생했습니다: " + e.getMessage());
            return "post_new";
        }

        // 성공 시, 게시판 목록으로 리다이렉트
        return "redirect:/clubs/" + clubId + "/board";
    }

    @GetMapping("/{clubId}/board/{postId}")
    public String getPostDetail(
            @PathVariable Long clubId,
            @PathVariable Long postId, // postId 파라미터 받기
            Model model,
            RedirectAttributes ra,
            HttpSession session) {

        // 헬퍼 메서드로 클럽/사이드바/탭 정보 로드
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        // PostService로 게시물 1건 조회
        Optional<Post> optPost = postService.findPostById(postId);

        // 게시물이 없거나, 해당 클럽의 게시물이 아닐 경우
        if (optPost.isEmpty() || !optPost.get().getClub().getId().equals(clubId)) {
            ra.addFlashAttribute("error", "게시물을 찾을 수 없거나 접근 권한이 없습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }

        // 모델에 게시물 데이터 추가
        model.addAttribute("post", optPost.get());
        model.addAttribute("activeTab", "board"); // 게시판 탭 활성화 유지

        // 템플릿 반환: /templates/clubs/post_detail.html
        return "clubs/post_detail";
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

    @PostMapping("/{clubId}/board/{postId}/delete")
    public String deletePost(@PathVariable Long clubId,
                             @PathVariable Long postId,
                             HttpSession session,
                             RedirectAttributes ra) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        try {
            postService.deletePost(postId, currentUserId);
            // [토스트 알림] 삭제 성공 시 메시지 전달
            ra.addFlashAttribute("msg", "게시글이 삭제되었습니다.");

        } catch (IllegalArgumentException | SecurityException e) {
            // [토스트 알림] 삭제 실패 시 에러 메시지 전달
            ra.addFlashAttribute("error", e.getMessage());
            // 실패 시 상세 페이지로 리다이렉트
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        // 성공 시 게시판 목록 페이지로 리다이렉트
        return "redirect:/clubs/" + clubId + "/board";
    }

    //게시물 수정 폼 메서드
    @GetMapping("/{clubId}/board/{postId}/edit")
    public String getEditPostForm(@PathVariable Long clubId,
                                  @PathVariable Long postId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes ra) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        //기존 게시물 데이터를 불러옴
        Optional<Post> optPost = postService.findPostById(postId);
        if (optPost.isEmpty()) {
            ra.addFlashAttribute("error", "게시물을 찾을 수 없습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }
        Post post = optPost.get();

        //작성자가 맞는지 확인
        if (!post.getAuthor().getId().equals(currentUserId)) {
            ra.addFlashAttribute("error", "수정 권한이 없습니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        //템플릿(post_edit.html)에 필요한 모든 데이터를 전달합니다.
        // (사이드바, 탭 등을 위한 공통 데이터)
        addClubDetailAttributes(clubId, model, session, ra);
        // (폼을 미리 채우기 위한 PostForm DTO)
        model.addAttribute("postForm", new PostForm(post.getTitle(), post.getContent()));
        model.addAttribute("clubId", clubId);
        model.addAttribute("postId", postId); // 폼 action URL과 '취소' 버튼에 사용

        return "post_edit";
    }

    //게시물 수정 처리
    @PostMapping("/{clubId}/board/{postId}/edit")
    public String updatePost(@PathVariable Long clubId,
                             @PathVariable Long postId,
                             @Valid @ModelAttribute("postForm") PostForm postForm,
                             BindingResult bindingResult,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return "redirect:/login"; // (보안)
        }

        //폼 유효성 검사 실패 (제목이나 내용이 비었을 때)
        if (bindingResult.hasErrors()) {
            //수정 폼 페이지를 다시 보여줘야 함
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            model.addAttribute("postId", postId);
            //postForm은 @ModelAttribute가 자동으로 다시 model에 넣어줌
            return "post_edit";
        }

        try {
            postService.updatePost(postId, postForm, currentUserId);

            // 수정 성공 시
            ra.addFlashAttribute("msg", "게시글이 수정되었습니다.");

        } catch (Exception e) {
            // (권한 오류, 게시물 없음 오류 등)
            ra.addFlashAttribute("error", e.getMessage());
            // 실패 시 수정 폼으로 리다이렉트
            return "redirect:/clubs/" + clubId + "/board/" + postId + "/edit";
        }

        // 성공 시 게시글 상세 페이지로 리다이렉트
        return "redirect:/clubs/" + clubId + "/board/" + postId;
    }

    //상세/탭 페이지 공통 속성 추가 헬퍼 메소드, @return 클럽을 찾지 못하면 false 반환
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
