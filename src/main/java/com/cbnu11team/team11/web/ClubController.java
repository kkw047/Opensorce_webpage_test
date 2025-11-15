package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.web.dto.ClubDetailDto;
import com.cbnu11team.team11.web.dto.ClubForm;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

import com.cbnu11team.team11.domain.ChatRoom;
import com.cbnu11team.team11.domain.ChatMessage;
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

    /**
     * 채팅 탭 - 채팅방 목록 보여주기 (뷰 반환)
     */
    @GetMapping("/{clubId}/chat")
    public String getChatPage(@PathVariable Long clubId, Model model, RedirectAttributes ra, HttpSession session) {
        if (!addClubDetailAttributes(clubId, model, session, ra)) {
            return "redirect:/clubs";
        }

        ClubDetailDto clubDto = (ClubDetailDto) model.getAttribute("club");
        if (clubDto == null || !clubDto.isAlreadyMember()) {
            ra.addFlashAttribute("error", "모임 멤버만 채팅방을 볼 수 있습니다.");
            return "redirect:/clubs/" + clubId; // 멤버가 아니면 홈으로
        }

        // 현재 유저 ID를 서비스로 전달하여 멤버 여부(isMember) 계산
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
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

    /**
     * 메시지 전송 처리 (AJAX용 JSON 반환)
     */
    @PostMapping("/{clubId}/chat/{roomId}/send")
    @ResponseBody // HTML 뷰가 아닌 JSON/데이터를 반환
    public ResponseEntity<?> sendMessage(@PathVariable Long clubId,
                                         @PathVariable Long roomId,
                                         @RequestParam("content") String content,
                                         HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다."); // 수정: HttpStatus.UNAUTHORIZED
        }

        try {
            // 서비스가 저장된 ChatMessage 엔티티를 반환
            ChatMessage newMessage = chatService.sendMessage(roomId, currentUserId, content);
            // 엔티티를 DTO로 변환하여 반환
            return ResponseEntity.ok(ChatMessageDto.fromEntity(newMessage));
        } catch (Exception e) {
            // 실패 시 에러 메시지 반환
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}