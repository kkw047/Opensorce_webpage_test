package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.service.CommentService;
import com.cbnu11team.team11.service.PostService;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberRepository clubMemberRepository;
    private final PostService postService;
    private final CommentService commentService;

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
        model.addAttribute("memberCounts", LoadMemberCounts(result.getContent()));

        return "clubs/index";
    }

    @GetMapping("/myclubs")
    public String myClubsPage(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
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
        Page<Club> myClubsPage = clubService.findMyClubs(currentUserId, pageable);

        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("selectedCategoryIds", List.of());
        model.addAttribute("q", null);
        model.addAttribute("page", myClubsPage);
        model.addAttribute("activeSidebarMenu", "myclubs");
        model.addAttribute("memberCounts", LoadMemberCounts(myClubsPage.getContent()));

        return "clubs/index";
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

        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        model.addAttribute("postForm", new PostForm("", ""));
        model.addAttribute("clubId", clubId);
        return "post_new";
    }

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

        if (bindingResult.hasErrors()) {

            addClubDetailAttributes(clubId, model, session, ra);
            model.addAttribute("clubId", clubId);
            return "post_new";
        }

        postService.createPost(clubId, postForm, currentUserId);
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

    @GetMapping("/{clubId}/chat")
    public String getChatPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        model.addAttribute("activeTab", "chat");
        return "clubs/chat";
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
            ra.addFlashAttribute("openLogin", true);
            return "redirect:/clubs/" + clubId;
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
        Long ownerId = null;
        Object uid = session.getAttribute("LOGIN_USER_ID");
        if (uid instanceof Long id) {
            ownerId = id;
        }

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

        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("selectedCategoryIds", List.of());
        model.addAttribute("q", null);

        return true;
    }

    private Map<Long, Long> LoadMemberCounts(List<Club> clubs) {
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