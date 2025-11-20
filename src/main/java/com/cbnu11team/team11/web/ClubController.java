package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.CommentService;
import com.cbnu11team.team11.service.PostService;
import com.cbnu11team.team11.web.dto.ClubDetailDto;
import com.cbnu11team.team11.web.dto.ClubForm;
import com.cbnu11team.team11.web.dto.CommentForm;
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
import java.util.stream.Collectors;

import com.cbnu11team.team11.domain.ChatRoom;
import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.service.ChatService;
import com.cbnu11team.team11.web.dto.ChatMessageDto;
import com.cbnu11team.team11.web.dto.CreateChatRoomRequest;
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

        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());

        // 검색 파라미터를 모델에 다시 추가 (검색창 값 유지를 위해)
        model.addAttribute("selectedDo", regionDo);
        model.addAttribute("selectedSi", regionSi);
        model.addAttribute("selectedCategoryIds", categoryIds == null ? List.of() : categoryIds);
        model.addAttribute("q", q);

        // 페이지 데이터
        model.addAttribute("page", myClubsPage);
        model.addAttribute("activeSidebarMenu", "myclubs");
        model.addAttribute("searchActionUrl", "/clubs/myclubs"); // 검색창이 요청할 URL
        model.addAttribute("memberCounts", LoadMemberCounts(myClubsPage.getContent()));

        return "clubs/index"; // 메인 템플릿 재사용
    }

    @GetMapping("/{clubId}")
    public String detail(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        model.addAttribute("activeTab", "home");
        return "clubs/detail";
    }

    @GetMapping("/{clubId}/board")
    public String getBoardPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        List<Post> posts = postService.getPostsByClubId(clubId);
        model.addAttribute("posts", posts);

        model.addAttribute("activeTab", "board");
        return "clubs/board";
    }

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

        model.addAttribute("postForm", new PostForm("", ""));
        model.addAttribute("clubId", clubId);
        return "post_new";
    }

//    @PostMapping("/{clubId}/board/new")
//    public String createPost(@PathVariable Long clubId,
//                             @Valid @ModelAttribute PostForm postForm,
//                             BindingResult bindingResult,
//                             HttpSession session,
//                             Model model,
//                             RedirectAttributes ra) {
//        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
//
//        if (currentUserId == null) {
//            return "redirect:/login";
//        }
//
//        boolean isMember = clubMemberRepository.existsById(new ClubMemberId(clubId, currentUserId));
//        if (!isMember) {
//            ra.addFlashAttribute("error", "클럽 멤버만 게시글을 작성할 수 있습니다.");
//            return "redirect:/clubs/" + clubId + "/board";
//        }
//
//        if (bindingResult.hasErrors()) {
//
//            addClubDetailAttributes(clubId, model, session, ra);
//            model.addAttribute("clubId", clubId);
//            return "post_new";
//        }
//
//        postService.createPost(clubId, postForm, currentUserId);
//        return "redirect:/clubs/" + clubId + "/board";
//    }

    @PostMapping("/{clubId}/board/new")
    public String createPost(@PathVariable Long clubId,
                             @Valid @ModelAttribute PostForm postForm,
                             BindingResult bindingResult,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {

        // 🚨 [CCTV] 요청이 들어왔는지 확인
        System.out.println("========== [DEBUG] 게시글 작성 요청 도착! ==========");
        System.out.println(">>> clubId: " + clubId);
        System.out.println(">>> data: " + postForm);

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (currentUserId == null) {
            System.out.println(">>> [DEBUG] 로그인 안 됨");
            return "redirect:/login";
        }

        // 멤버 체크 로직
        boolean isMember = clubMemberRepository.existsById(new ClubMemberId(clubId, currentUserId));
        if (!isMember) {
            System.out.println(">>> [DEBUG] 멤버 아님");
            ra.addFlashAttribute("error", "클럽 멤버만 게시글을 작성할 수 있습니다.");
            return "redirect:/clubs/" + clubId + "/board";
        }

        // 유효성 검사 실패 확인
        if (bindingResult.hasErrors()) {
            System.out.println(">>> [DEBUG] 유효성 검사 실패: " + bindingResult.getAllErrors()); // 👈 여기가 범인일 수도 있음

            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            return "post_new";
        }

        try {
            postService.createPost(clubId, postForm, currentUserId);
            System.out.println(">>> [DEBUG] 게시글 저장 성공!");
        } catch (Exception e) {
            System.out.println(">>> [ERROR] 저장 중 에러 발생!");
            e.printStackTrace(); // 👈 에러 내용 출력
        }

        return "redirect:/clubs/" + clubId + "/board";
    }

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
        model.addAttribute("postForm", new PostForm(post.getTitle(), post.getContent()));
        model.addAttribute("clubId", clubId);
        model.addAttribute("postId", postId);

        return "post_edit";
    }

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
            ra.addFlashAttribute("msg", "게시글이 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }
        return "redirect:/clubs/" + clubId + "/board";
    }

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

    @PostMapping("/{clubId}/board/{postId}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long clubId,
                                @PathVariable Long postId,
                                @PathVariable Long commentId,
                                HttpSession session,
                                RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/board/" + postId;
        }

        try {
            commentService.deleteComment(commentId, currentUserId);
            ra.addFlashAttribute("msg", "댓글이 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clubs/" + clubId + "/board/" + postId;
    }

    @GetMapping("/{clubId}/calendar")
    public String getCalendarPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        model.addAttribute("activeTab", "calendar");
        return "clubs/calendar";
    }

    @PostMapping("/{clubId}/join")
    public String joinClub(@PathVariable Long clubId, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인 후 가입할 수 있습니다.");
            ra.addFlashAttribute("openLogin", true); // 로그인 모달 바로 열기
            return "redirect:/clubs/" + clubId; // 현재 상세 페이지로 리다이렉트
        }

        try {
            clubService.joinClub(clubId, currentUserId);
            ra.addFlashAttribute("msg", "모임에 가입되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/clubs/" + clubId;
    }

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

        model.addAttribute("members", dto.members());

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

    /**
     * 채팅 탭 - 채팅방 목록 보여주기 (뷰 반환)
     */
    @GetMapping("/{clubId}/chat")
    public String getChatPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인 후 이용할 수 있습니다.");
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs/" + clubId; // 로그인이 안됐으면 홈으로
        }

        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto clubDto = (ClubDetailDto) model.getAttribute("club");

        if (clubDto == null || !clubDto.isAlreadyMember()) {
            ra.addFlashAttribute("error", "모임 멤버만 채팅방을 볼 수 있습니다.");
            return "redirect:/clubs/" + clubId; // 멤버가 아니면 홈으로
        }

        // 현재 유저 ID를 서비스로 전달하여 멤버 여부(isMember) 계산
        model.addAttribute("chatRooms", chatService.getChatRoomsByClub(clubId, currentUserId));
        model.addAttribute("activeTab", "chat");
        return "clubs/chat"; // chat.html 뷰 반환
    }

    /**
     * 채팅방 가입 처리
     */
    @PostMapping("/{clubId}/chat/{roomId}/join")
    public String joinChatRoom(@PathVariable Long clubId,
                               @PathVariable Long roomId,
                               HttpSession session,
                               RedirectAttributes ra) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/chat";
        }

        try {
            chatService.joinChatRoom(roomId, currentUserId);
            ra.addFlashAttribute("msg", "채팅방에 가입되었습니다.");

            // 가입 성공 시, 채팅 목록으로 리다이렉트하며 팝업을 띄우도록 파라미터 추가
            return "redirect:/clubs/" + clubId + "/chat?openRoom=" + roomId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            // 실패 시 채팅방 목록으로
            return "redirect:/clubs/" + clubId + "/chat";
        }
    }

    /**
     * 채팅방 생성 폼 페이지 (뷰 반환)
     */
    @GetMapping("/{clubId}/chat/create")
    public String getChatCreatePage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto clubDto = (ClubDetailDto) model.getAttribute("club");
        if (clubDto == null || !clubDto.isAlreadyMember()) {
            ra.addFlashAttribute("error", "모임 멤버만 채팅방을 개설할 수 있습니다.");
            return "redirect:/clubs/" + clubId;
        }

        model.addAttribute("requestDto", new CreateChatRoomRequest("", List.of()));
        model.addAttribute("activeTab", "chat"); // 상단 탭 활성화
        return "clubs/chat_create";
    }

    /**
     * 채팅방 생성 처리
     */
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

            // 생성 직후에도 팝업을 띄우도록 파라미터 추가
            return "redirect:/clubs/" + clubId + "/chat?openRoom=" + newRoom.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/chat/create"; // 생성 폼으로 다시 이동
        }
    }

    /**
     * 개별 채팅방 상세 페이지 (뷰 반환)
     * (이 엔드포인트는 모달 팝업 대신, URL로 직접 접근 시 사용됩니다.)
     */
    @GetMapping("/{clubId}/chat/{roomId}")
    public String getChatRoomDetailPage(@PathVariable Long clubId,
                                        @PathVariable Long roomId,
                                        Model model, RedirectAttributes ra, HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        try {
            ChatRoom room = chatService.getChatRoomDetails(roomId);

            if (!room.getClub().getId().equals(clubId)) {
                throw new IllegalStateException("모임 정보가 일치하지 않습니다.");
            }

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

            return "clubs/chat_room"; // 개별 채팅방 뷰

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId + "/chat";
        }
    }
}