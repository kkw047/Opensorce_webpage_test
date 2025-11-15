package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.ChatRoom;
import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.service.ChatService;
import com.cbnu11team.team11.web.dto.ChatMessageDto;
import com.cbnu11team.team11.web.dto.ChatRoomDetailDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs")
public class ChatApiController {

    private final ChatService chatService;

    public record ManageableMemberDto(Long id, String nickname, String email) {}


    /**
     * 채팅방 상세 정보 (JSON) - 메시지 제외
     * 모달 팝업을 띄울 때 JavaScript(fetch)로 호출할 API
     */
    @GetMapping("/{clubId}/chat/{roomId}")
    public ResponseEntity<?> getChatRoomApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            // 서비스에서 보안 검사 및 DTO 변환 수행
            ChatRoomDetailDto dto = chatService.getChatRoomDetailsDto(roomId, currentUserId);

            // clubId 검증 (URL 조작 방지)
            if (!chatService.getChatRoomDetails(roomId).getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }

            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            // 멤버가 아닌 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // 방을 못찾은 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 추가: 채팅방 메시지 목록 (JSON)
     */
    @GetMapping("/{clubId}/chat/{roomId}/messages")
    public ResponseEntity<?> getChatMessagesApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            // 방 정보 및 보안 검증
            ChatRoom room = chatService.getChatRoomDetails(roomId); // messages 포함 Eager Fetch
            if (!room.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }
            boolean isMember = room.getMembers().stream().anyMatch(user -> user.getId().equals(currentUserId));
            if (!isMember) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("채팅방 멤버만 조회할 수 있습니다.");
            }

            // 메시지를 DTO로 변환
            List<ChatMessageDto> messageDtos = room.getMessages().stream()
                    .map(ChatMessageDto::fromEntity)
                    .sorted(Comparator.comparing(ChatMessageDto::id))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(messageDtos); // 메시지 목록 반환

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 추가: 채팅방 관리 정보 (JSON)
     */
    @GetMapping("/{clubId}/chat/{roomId}/manage")
    public ResponseEntity<?> getManageInfo(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            ChatRoom room = chatService.getChatRoomDetails(roomId); // owner, members 포함 Eager Fetch

            if (room.getOwner() == null || !room.getOwner().getId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("방장만 접근할 수 있습니다.");
            }
            if (!room.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }

            List<ManageableMemberDto> membersToManage = room.getMembers().stream()
                    .filter(m -> !m.getId().equals(currentUserId))
                    .sorted(Comparator.comparing(User::getNickname))
                    .map(user -> new ManageableMemberDto(user.getId(), user.getNickname(), user.getEmail()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(membersToManage);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 추가: 멤버 강퇴 (JSON)
     */
    @PostMapping("/{clubId}/chat/{roomId}/kick")
    public ResponseEntity<?> kickMemberApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            @RequestParam("memberId") Long memberToKickId,
            HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            ChatRoom room = chatService.getChatRoomDetails(roomId);
            if (!room.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }

            chatService.kickMember(roomId, currentUserId, memberToKickId);
            return ResponseEntity.ok().body("멤버를 강퇴했습니다.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 추가: 채팅방 삭제 (JSON)
     */
    @PostMapping("/{clubId}/chat/{roomId}/delete")
    public ResponseEntity<?> deleteChatRoomApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            ChatRoom room = chatService.getChatRoomDetails(roomId);
            if (!room.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }

            chatService.deleteChatRoom(roomId, currentUserId);
            return ResponseEntity.ok().body("채팅방이 삭제되었습니다.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}