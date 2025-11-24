package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.web.dto.ClubActivityStatDto; // [추가]
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

import com.cbnu11team.team11.domain.ChatRoom;
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
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("memberCounts", LoadMemberCounts(result.getContent()));

        return "clubs/index";
    }

    // 내 모임 페이지
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

    // [수정됨] 모임 상세 페이지 (홈 탭) + 활동 지표 추가
    @GetMapping("/{clubId}")
    public String detail(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        // [추가] 활동 지표 데이터 (최근 일정 3개 출석률)
        List<ClubActivityStatDto> stats = clubService.getRecentActivityStats(clubId);
        model.addAttribute("activityStats", stats);

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

    // 모임 생성
    @PostMapping
    public String create(@ModelAttribute ClubForm form, HttpSession session, RedirectAttributes ra) {
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

    // 헬퍼 메소드
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
        if(clubs == null || clubs.isEmpty()) return Map.of();
        List<Long> ids = clubs.stream().map(Club::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) return Map.of();
        var rows = clubMemberRepository.countMembersByClubIds(ids);
        Map<Long, Long> out = new HashMap<>();
        for (var r : rows) { out.put(r.getClubId(), r.getCnt()); }
        for (Long id : ids) { out.putIfAbsent(id, 0L); }
        return out;
    }

    // 채팅 탭
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
        ClubDetailDto clubDto = (ClubDetailDto) model.getAttribute("club");
        if (clubDto == null || !clubDto.isAlreadyMember()) {
            ra.addFlashAttribute("error", "모임 멤버만 채팅방을 볼 수 있습니다.");
            return "redirect:/clubs/" + clubId;
        }
        model.addAttribute("chatRooms", chatService.getChatRoomsByClub(clubId, currentUserId));
        model.addAttribute("activeTab", "chat");
        return "clubs/chat";
    }

    // 채팅방 가입
    @PostMapping("/{clubId}/chat/{roomId}/join")
    public String joinChatRoom(@PathVariable Long clubId, @PathVariable Long roomId, HttpSession session, RedirectAttributes ra) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/clubs/" + clubId + "/chat";
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

    // 채팅방 생성 페이지
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
        model.addAttribute("activeTab", "chat");
        return "clubs/chat_create";
    }

    // 채팅방 생성
    @PostMapping("/{clubId}/chat/create")
    public String createChatRoom(@PathVariable Long clubId, @ModelAttribute CreateChatRoomRequest request, HttpSession session, RedirectAttributes ra) {
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

    // 채팅방 상세 (URL 접근용)
    @GetMapping("/{clubId}/chat/{roomId}")
    public String getChatRoomDetailPage(@PathVariable Long clubId, @PathVariable Long roomId, Model model, RedirectAttributes ra, HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }
        try {
            ChatRoom room = chatService.getChatRoomDetails(roomId);
            if (!room.getClub().getId().equals(clubId)) throw new IllegalStateException("모임 정보 불일치");
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
}