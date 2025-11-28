package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.CommentService;
import com.cbnu11team.team11.service.PostService;
import com.cbnu11team.team11.web.dto.*;
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
import java.util.stream.Collectors;

import com.cbnu11team.team11.domain.ChatRoom;
import com.cbnu11team.team11.service.ChatService;

import java.util.Comparator;


@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberRepository clubMemberRepository;
    private final PostService postService;
    private final CommentService commentService;
    private final ChatService chatService;

    // --- 메인 목록 ---
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
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("memberCounts", LoadMemberCounts(result.getContent()));

        return "clubs/index";
    }

    // --- 내 모임 ---
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
            return "redirect:/clubs";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Club> myClubsPage = clubService.searchMyClubs(currentUserId, q, regionDo, regionSi, categoryIds, pageable);

        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("selectedDo", regionDo);
        model.addAttribute("selectedSi", regionSi);
        model.addAttribute("selectedCategoryIds", categoryIds == null ? List.of() : categoryIds);
        model.addAttribute("q", q);
        model.addAttribute("page", myClubsPage);
        model.addAttribute("activeSidebarMenu", "myclubs");
        model.addAttribute("searchActionUrl", "/clubs/myclubs");
        model.addAttribute("memberCounts", LoadMemberCounts(myClubsPage.getContent()));

        return "clubs/index";
    }

    // --- 모임 상세 (홈) ---
    @GetMapping("/{clubId}")
    public String detail(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }


        // [병합 포인트] 두 번째 코드에 있던 활동 지표(activityStats) 로직 추가
        List<ClubActivityStatDto> stats = clubService.getRecentActivityStats(clubId);
        model.addAttribute("activityStats", stats);

        model.addAttribute("activeTab", "home");
        return "clubs/detail";
    }

    // --- 게시판 목록 ---
    @GetMapping("/{clubId}/board")
    public String getBoardPage(@PathVariable Long clubId,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "type", required = false) String type,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               Model model,
                               RedirectAttributes ra,
                               HttpSession session) {

        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        Page<Post> postPage = postService.getPostsByClubId(clubId, page, type, keyword);

        int nowPage = postPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 2, 1);
        int endPage = Math.min(nowPage + 2, postPage.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("posts", postPage);
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("searchType", type);
        model.addAttribute("searchKeyword", keyword);
        model.addAttribute("activeTab", "board");
        return "clubs/board";
    }

    // --- 게시글 작성 폼 ---
    @GetMapping("/{clubId}/board/new")
    public String getNewPostForm(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "글을 작성하려면 로그인이 필요합니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs/" + clubId + "/board";
        }

        boolean isMember = clubMemberRepository.existsById(new ClubMemberId(clubId, currentUserId));
        if (!isMember) {
            ra.addFlashAttribute("error", "클럽 멤버만 게시글을 작성할 수 있습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }

        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        model.addAttribute("postForm", new PostForm("", "", null, null));
        model.addAttribute("clubId", clubId);
        return "post_new";
    }

    // --- 게시글 작성 처리 ---
    @PostMapping("/{clubId}/board/new")
    public String createPost(@PathVariable Long clubId,
                             @Valid @ModelAttribute PostForm postForm,
                             BindingResult bindingResult,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (currentUserId == null) {
            return "redirect:/login";
        }

        boolean isMember = clubMemberRepository.existsById(new ClubMemberId(clubId, currentUserId));
        if (!isMember) {
            ra.addFlashAttribute("error", "클럽 멤버만 게시글을 작성할 수 있습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }

        if (bindingResult.hasErrors()) {
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            return "post_new";
        }

        postService.createPost(clubId, postForm, currentUserId);
        ra.addFlashAttribute("msg", "게시글이 등록되었습니다.");
        return "redirect:/clubs/" + clubId + "/board";
    }

    // --- 게시글 상세 ---
    @GetMapping("/{clubId}/board/{postId}")
    public String getPostDetail(@PathVariable Long clubId,
                                @PathVariable Long postId,
                                Model model,
                                RedirectAttributes ra,
                                HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "게시글을 보려면 로그인이 필요합니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs/" + clubId + "/board";
        }

        boolean isMember = clubMemberRepository.existsById(new ClubMemberId(clubId, currentUserId));
        if (!isMember) {
            ra.addFlashAttribute("error", "클럽 멤버만 게시글을 볼 수 있습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }

        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        Optional<Post> optPost = postService.findPostById(postId);
        if (optPost.isEmpty() || !optPost.get().getClub().getId().equals(clubId)) {
            ra.addFlashAttribute("error", "게시글을 찾을 수 없습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }

        model.addAttribute("post", optPost.get());
        model.addAttribute("activeTab", "board");

        List<Comment> comments = commentService.getCommentsByPostId(postId);
        model.addAttribute("comments", comments);
        model.addAttribute("commentForm", new CommentForm(""));

        return "clubs/post_detail";
    }

    // --- 게시글 수정 폼 ---
    @GetMapping("/{clubId}/board/{postId}/edit")
    public String getEditPostForm(@PathVariable Long clubId,
                                  @PathVariable Long postId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        Optional<Post> optPost = postService.findPostById(postId);
        if (optPost.isEmpty()) {
            ra.addFlashAttribute("error", "게시물을 찾을 수 없습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }
        Post post = optPost.get();

        if (!post.getAuthor().getId().equals(currentUserId)) {
            ra.addFlashAttribute("error", "수정 권한이 없습니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        addClubDetailAttributes(clubId, model, session, ra);
        model.addAttribute("postForm", new PostForm(post.getTitle(), post.getContent(), null, null));
        model.addAttribute("clubId", clubId);
        model.addAttribute("postId", postId);
        model.addAttribute("post", post);
        return "post_edit";
    }

    // --- 게시글 수정 처리 ---
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
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            model.addAttribute("postId", postId);
            return "post_edit";
        }

        try {
            postService.updatePost(postId, postForm, currentUserId);
            ra.addFlashAttribute("msg", "게시글이 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/board/" + postId + "/edit";
        }
        return "redirect:/clubs/" + clubId + "/board/" + postId;
    }

    // --- 게시글 삭제 ---
    @PostMapping("/{clubId}/board/{postId}/delete")
    public String deletePost(@PathVariable Long clubId,
                             @PathVariable Long postId,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        try {
            postService.deletePost(postId, currentUserId);
            ra.addFlashAttribute("msg", "게시글이 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }
        return "redirect:/clubs/" + clubId + "/board";
    }

    // --- 댓글 등록 ---
    @PostMapping("/{clubId}/board/{postId}/comment")
    public String createComment(@PathVariable Long clubId,
                                @PathVariable Long postId,
                                @Valid @ModelAttribute("commentForm") CommentForm commentForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                RedirectAttributes ra,
                                Model model) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        if (bindingResult.hasErrors()) {
            addClubDetailAttributes(clubId, model, session, ra);
            Post post = postService.findPostById(postId).orElse(null);
            model.addAttribute("post", post);
            model.addAttribute("activeTab", "board");
            List<Comment> comments = commentService.getCommentsByPostId(postId);
            model.addAttribute("comments", comments);
            ra.addFlashAttribute("error", "댓글 내용을 입력해주세요.");
            return "clubs/post_detail";
        }

        try {
            commentService.createComment(postId, currentUserId, commentForm);
            ra.addFlashAttribute("msg", "댓글이 등록되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clubs/" + clubId + "/board/" + postId;
    }

    // --- 댓글 수정 폼 ---
    @GetMapping("/{clubId}/board/{postId}/comment/{commentId}/edit")
    public String getEditCommentForm(@PathVariable Long clubId,
                                     @PathVariable Long postId,
                                     @PathVariable Long commentId,
                                     HttpSession session,
                                     Model model,
                                     RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        try {
            Comment comment = commentService.getCommentById(commentId);
            if (!comment.getAuthor().getId().equals(currentUserId)) {
                ra.addFlashAttribute("error", "수정 권한이 없습니다.");
                return "redirect:/clubs/" + clubId + "/board/" + postId;
            }

            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("commentForm", new CommentForm(comment.getContent()));
            model.addAttribute("clubId", clubId);
            model.addAttribute("postId", postId);
            model.addAttribute("commentId", commentId);
            return "comment_edit";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }
    }

    // --- 댓글 수정 처리 ---
    @PostMapping("/{clubId}/board/{postId}/comment/{commentId}/edit")
    public String updateComment(@PathVariable Long clubId,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                @Valid @ModelAttribute("commentForm") CommentForm commentForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model,
                                RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            model.addAttribute("postId", postId);
            model.addAttribute("commentId", commentId);
            return "comment_edit";
        }

        try {
            commentService.updateComment(commentId, commentForm, currentUserId);
            ra.addFlashAttribute("msg", "댓글이 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/board/" + postId + "/comment/" + commentId + "/edit";
        }
        return "redirect:/clubs/" + clubId + "/board/" + postId;
    }

    // --- 댓글 삭제 ---
    @PostMapping("/{clubId}/board/{postId}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long clubId,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                HttpSession session,
                                Model model,
                                RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        try {
            commentService.deleteComment(commentId, currentUserId);
            ra.addFlashAttribute("msg", "댓글이 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clubs/" + clubId + "/board/" + postId;
    }

    // --- 캘린더 페이지 ---
    @GetMapping("/{clubId}/calendar")
    public String getCalendarPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        model.addAttribute("activeTab", "calendar");
        return "clubs/calendar";
    }

    // --- 모임 가입 ---
    @PostMapping("/{clubId}/join")
    public String joinClub(@PathVariable Long clubId, HttpSession session, RedirectAttributes ra, Model model) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인 후 가입할 수 있습니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs/" + clubId;
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        try {
            ClubMemberStatus status = clubService.joinClub(clubId, currentUserId);
            // 상태에 따라 메시지 분기 (첫 번째 코드의 로직 사용)
            if (status == ClubMemberStatus.ACTIVE) {
                ra.addFlashAttribute("msg", "모임에 가입되었습니다! 환영합니다.");
            } else {
                ra.addFlashAttribute("msg", "가입 신청이 완료되었습니다. 모임장의 승인을 기다려주세요.");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clubs/" + clubId;
    }

    // --- 모임 생성 ---
    @PostMapping
    public String create(@ModelAttribute ClubForm form,
                         HttpSession session,
                         RedirectAttributes ra) {
        Long ownerId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (ownerId == null) {
            ra.addFlashAttribute("error", "로그인 후 모임을 만들 수 있습니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs";
        }

        clubService.createClub(ownerId, form);
        ra.addFlashAttribute("msg", "모임이 생성되었습니다.");
        return "redirect:/clubs";
    }

    // --- 공통: 모임 상세 속성 로딩 ---
    private boolean addClubDetailAttributes(Long clubId, Model model, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        Optional<ClubDetailDto> optDto = clubService.getClubDetail(clubId, currentUserId);

        if (optDto.isEmpty()) {
            ra.addFlashAttribute("error", "해당 모임을 찾을 수 없습니다.");
            return false;
        }

        ClubDetailDto dto = optDto.get();
        model.addAttribute("club", dto);
        model.addAttribute("memberCount", dto.members().size());
        model.addAttribute("isOwner", dto.isOwner());
        model.addAttribute("isAlreadyMember", dto.isAlreadyMember());
        model.addAttribute("members", dto.members());

        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("selectedCategoryIds", List.of());
        model.addAttribute("q", null);
        model.addAttribute("searchActionUrl", "/clubs");

        return true;
    }

    private Map<Long, Long> LoadMemberCounts(List<Club> clubs) {
        if (clubs == null || clubs.isEmpty()) return Map.of();
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

    // --- 채팅 목록 ---
    @GetMapping("/{clubId}/chat")
    public String getChatPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인 후 이용할 수 있습니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs/" + clubId;
        }

        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        ClubDetailDto clubDto = (ClubDetailDto) model.getAttribute("club");
        if (clubDto == null || !clubDto.isAlreadyMember()) {
            ra.addFlashAttribute("error", "모임 멤버만 채팅방을 볼 수 있습니다.");
            return "redirect:/clubs/" + clubId;
        }

        model.addAttribute("chatRooms", chatService.getChatRoomsByClub(clubId, currentUserId));
        model.addAttribute("activeTab", "chat");
        return "clubs/chat";
    }

    // --- 채팅방 가입 ---
    @PostMapping("/{clubId}/chat/{roomId}/join")
    public String joinChatRoom(@PathVariable Long clubId,
                               @PathVariable Long roomId,
                               HttpSession session,
                               Model model,
                               RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/chat";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }


        try {
            chatService.joinChatRoom(roomId, currentUserId);
            ra.addFlashAttribute("msg", "채팅방에 가입되었습니다.");
            return "redirect:/clubs/" + clubId + "/chat?openRoom=" + roomId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/chat";
        }
    }

    // --- 채팅방 생성 폼 ---
    @GetMapping("/{clubId}/chat/create")
    public String getChatCreatePage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }


        ClubDetailDto clubDto = (ClubDetailDto) model.getAttribute("club");
        if (clubDto == null || !clubDto.isAlreadyMember()) {
            ra.addFlashAttribute("error", "모임 멤버만 채팅방을 개설할 수 있습니다.");
            return "redirect:/clubs/" + clubId;
        }

        model.addAttribute("requestDto", new CreateChatRoomRequest("", List.of()));
        model.addAttribute("activeTab", "chat");
        return "clubs/chat_create";
    }

    // --- 채팅방 생성 처리 ---
    @PostMapping("/{clubId}/chat/create")
    public String createChatRoom(@PathVariable Long clubId,
                                 @ModelAttribute CreateChatRoomRequest request,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId;
        }

        try {
            ChatRoom newRoom = chatService.createChatRoom(clubId, currentUserId, request.roomName(), request.memberIds());
            ra.addFlashAttribute("msg", "채팅방이 개설되었습니다.");
            return "redirect:/clubs/" + clubId + "/chat?openRoom=" + newRoom.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/chat/create";
        }
    }

    // --- 채팅방 상세 (URL 직접 접근) ---
    @GetMapping("/{clubId}/chat/{roomId}")
    public String getChatRoomDetailPage(@PathVariable Long clubId,
                                        @PathVariable Long roomId,
                                        Model model, RedirectAttributes ra, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }


        try {
            ChatRoom room = chatService.getChatRoomDetails(roomId);
            if (!room.getClub().getId().equals(clubId)) throw new IllegalStateException("모임 정보가 일치하지 않습니다.");

            boolean isMember = room.getMembers().stream().anyMatch(user -> user.getId().equals(currentUserId));
            if (!isMember) {
                ra.addFlashAttribute("error", "채팅방 멤버만 입장할 수 있습니다.");
                return "redirect:/clubs/" + clubId + "/chat";
            }

            List<ChatMessageDto> messages = room.getMessages().stream()
                    .map(ChatMessageDto::fromEntity)
                    .sorted(Comparator.comparing(ChatMessageDto::id))
                    .collect(Collectors.toList());

            model.addAttribute("roomName", room.getName());
            model.addAttribute("roomId", room.getId());
            model.addAttribute("roomOwnerId", room.getOwner() != null ? room.getOwner().getId() : null);
            model.addAttribute("messages", messages);
            model.addAttribute("activeTab", "chat");
            return "clubs/chat_room";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/chat";
        }
    }

    // --- 관리자 페이지 ---
    @GetMapping("/{clubId}/manager")
    public String getManagerPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId;
        }

        if (!addClubDetailAttributes(clubId, model, session, ra)) return "redirect:/clubs";

        ClubDetailDto club = (ClubDetailDto) model.getAttribute("club");

        if (club != null && "BANNED".equals(club.myStatus())) {
            ra.addFlashAttribute("error", "이 모임에서 차단되어 접근할 수 없습니다.");
            return "redirect:/clubs";
        }

        // 권한 체크: DTO에 있는 isManager 값을 확인
        ClubDetailDto dto = (ClubDetailDto) model.getAttribute("club");
        if (dto == null || !dto.isManager()) {
            ra.addFlashAttribute("error", "모임장만 접근할 수 있습니다.");
            return "redirect:/clubs/" + clubId;
        }
        model.addAttribute("activeTab", "manager");
        return "clubs/manager";
    }

    // --- 관리자: 멤버 관리 ---
    @GetMapping("/{clubId}/manager/members")
    public String getMemberManagementPage(@PathVariable Long clubId, Model model, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        Optional<ClubDetailDto> optDto = clubService.getClubDetail(clubId, currentUserId);
        if (optDto.isEmpty() || !optDto.get().isManager()) {
            ra.addFlashAttribute("error", "모임장 권한이 없습니다.");
            return "redirect:/clubs/" + clubId;
        }

        List<ClubMember> waitingList = clubService.getMembersByStatus(clubId, ClubMemberStatus.WAITING);
        List<ClubMember> activeList = clubService.getMembersByStatus(clubId, ClubMemberStatus.ACTIVE);

        List<ClubMember> bannedList = clubService.getMembersByStatus(clubId, ClubMemberStatus.BANNED);

        List<ClubMember> sortedActiveList = activeList.stream()
                .sorted((m1, m2) -> {
                    boolean m1IsManager = m1.getRole() == ClubRole.MANAGER || m1.getRole() == ClubRole.ADMIN;
                    boolean m2IsManager = m2.getRole() == ClubRole.MANAGER || m2.getRole() == ClubRole.ADMIN;
                    if (m1IsManager && !m2IsManager) return -1;
                    if (!m1IsManager && m2IsManager) return 1;
                    return m1.getUser().getNickname().compareTo(m2.getUser().getNickname());
                })
                .toList();

        model.addAttribute("categories", clubService.getAllCategories());
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("selectedCategoryIds", new ArrayList<>());
        model.addAttribute("q", "");
        model.addAttribute("selectedDo", "");
        model.addAttribute("selectedSi", "");

        model.addAttribute("club", optDto.get());
        model.addAttribute("waitingList", waitingList);
        model.addAttribute("activeList", sortedActiveList);
        model.addAttribute("bannedList", bannedList);
        model.addAttribute("activeTab", "manager");
        return "clubs/manager_members";
    }

    // --- 관리자: 멤버 승인 ---
    @PostMapping("/{clubId}/manager/members/{memberId}/approve")
    public String approveMember(@PathVariable Long clubId, @PathVariable Long memberId, RedirectAttributes ra) {
        clubService.approveMember(clubId, memberId);
        ra.addFlashAttribute("msg", "멤버 가입이 승인되었습니다.");
        return "redirect:/clubs/" + clubId + "/manager/members";
    }

    // --- 관리자: 멤버 추방 ---
    @PostMapping("/{clubId}/manager/members/{memberId}/kick")
    public String kickMember(@PathVariable Long clubId, @PathVariable Long memberId, RedirectAttributes ra) {
        clubService.kickMember(clubId, memberId);
        ra.addFlashAttribute("msg", "멤버를 모임에서 추방했습니다.");
        return "redirect:/clubs/" + clubId + "/manager/members";
    }

    // --- 관리자: 멤버 거절 ---
    @PostMapping("/{clubId}/manager/members/{memberId}/reject")
    public String rejectMember(@PathVariable Long clubId,
                               @PathVariable Long memberId,
                               @RequestParam("reason") String reason,
                               RedirectAttributes ra) {
        clubService.rejectMember(clubId, memberId, reason);
        ra.addFlashAttribute("msg", "가입 요청을 거절했습니다.");

        return "redirect:/clubs/" + clubId + "/manager/members";
    }

    // 차단 해제 요청
    @PostMapping("/{clubId}/manager/members/{memberId}/unban")
    public String unbanMember(@PathVariable Long clubId,
                              @PathVariable Long memberId,
                              RedirectAttributes ra) {
        clubService.unbanMember(clubId, memberId);
        ra.addFlashAttribute("msg", "차단이 해제되었습니다.");
        return "redirect:/clubs/" + clubId + "/manager/members";
    }

    // --- 모임 삭제 ---
    @PostMapping("/{clubId}/delete")
    public String deleteClub(@PathVariable Long clubId, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        Optional<ClubDetailDto> optDto = clubService.getClubDetail(clubId, currentUserId);

        if (optDto.isEmpty() || !optDto.get().isManager()) {
            ra.addFlashAttribute("error", "모임 삭제 권한이 없습니다.");
            return "redirect:/clubs/" + clubId;
        }

        try {
            clubService.deleteClub(clubId);
            ra.addFlashAttribute("msg", "모임이 삭제되었습니다.");
            return "redirect:/clubs";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "모임 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/clubs/" + clubId + "/manager";
        }
    }

    // --- 관리자: 모임 정보 수정 폼 ---
    @GetMapping("/{clubId}/manager/edit")
    public String getClubEditForm(@PathVariable Long clubId, Model model, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        Optional<ClubDetailDto> optDto = clubService.getClubDetail(clubId, currentUserId);
        if (optDto.isEmpty() || !optDto.get().isManager()) {
            ra.addFlashAttribute("error", "수정 권한이 없습니다.");
            return "redirect:/clubs/" + clubId;
        }

        ClubDetailDto club = optDto.get();

        List<Long> currentCategoryIds = clubService.getClubCategoryIds(clubId);

        ClubForm form = new ClubForm(
                club.name(),
                club.description(),
                club.regionDo(),
                club.regionSi(),
                null,
                currentCategoryIds,
                null
        );

        model.addAttribute("categories", clubService.getAllCategories());
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("selectedCategoryIds", new ArrayList<Long>());
        model.addAttribute("q", "");
        model.addAttribute("selectedDo", "");
        model.addAttribute("selectedSi", "");

        model.addAttribute("club", club);
        model.addAttribute("clubForm", form);
        model.addAttribute("activeTab", "manager");
        return "clubs/club_edit";
    }

    // --- 관리자: 모임 정보 수정 처리 ---
    @PostMapping("/{clubId}/manager/edit")
    public String updateClub(@PathVariable Long clubId,
                             @ModelAttribute ClubForm clubForm,
                             HttpSession session,
                             RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        try {
            clubService.updateClub(clubId, clubForm, currentUserId);
            ra.addFlashAttribute("msg", "모임 정보가 수정되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "수정 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/clubs/" + clubId + "/manager/edit";
        }

        return "redirect:/clubs/" + clubId + "/manager";
    }

    // --- 관리자: 기능 설정 폼 ---
    @GetMapping("/{clubId}/manager/features")
    public String getClubFeaturesForm(@PathVariable Long clubId, Model model, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        Optional<ClubDetailDto> optDto = clubService.getClubDetail(clubId, currentUserId);
        if (optDto.isEmpty() || !optDto.get().isManager()) {
            ra.addFlashAttribute("error", "모임장 권한이 없습니다.");
            return "redirect:/clubs/" + clubId;
        }
        ClubDetailDto club = optDto.get();
        List<Long> currentCategoryIds = clubService.getClubCategoryIds(clubId);

        ClubForm form = new ClubForm(
                club.name(),
                club.description(),
                club.regionDo(),
                club.regionSi(),
                null,
                currentCategoryIds,
                null
        );

        model.addAttribute("categories", clubService.getAllCategories());
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("q", "");
        model.addAttribute("selectedDo", "");
        model.addAttribute("selectedSi", "");

        model.addAttribute("club", club);
        model.addAttribute("clubForm", form);
        model.addAttribute("activeTab", "manager");
        return "clubs/manager_features";
    }

    // --- 관리자: 기능 설정 처리 (가입 방식) ---
    @PostMapping("/{clubId}/manager/features")
    public String updateClubFeatures(@PathVariable Long clubId,
                                     @RequestParam("policy") ClubJoinPolicy policy,
                                     RedirectAttributes ra) {
        clubService.updateClubPolicy(clubId, policy);
        ra.addFlashAttribute("msg", "가입 방식이 변경되었습니다.");
        return "redirect:/clubs/" + clubId + "/manager";
    }

    // --- 관리자: 멤버 차단 ---
    @PostMapping("/{clubId}/manager/members/{memberId}/ban")
    public String banMember(@PathVariable Long clubId,
                            @PathVariable Long memberId,
                            RedirectAttributes ra) {
        clubService.banMember(clubId, memberId);
        ra.addFlashAttribute("msg", "해당 멤버를 영구 차단했습니다.");
        return "redirect:/clubs/" + clubId + "/manager/members";
    }

    // --- 모임 탈퇴 ---
    @PostMapping("/{clubId}/leave")
    public String leaveClub(@PathVariable Long clubId, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        try {
            clubService.leaveClub(clubId, currentUserId);
            ra.addFlashAttribute("msg", "모임에서 탈퇴했습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId;
        }
        return "redirect:/clubs";
    }
}