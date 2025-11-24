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
            ChatRoomDetailDto dto = chatService.getChatRoomDetailsDto(roomId, currentUserId);
            if (!chatService.getChatRoomDetails(roomId).getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 채팅방 메시지 목록 (JSON)
     * ChatService의 getChatMessages를 사용하여 안전하게 조회
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
            // 권한 체크 (방 멤버인지 확인)
            ChatRoom room = chatService.getChatRoomDetails(roomId);
            if (!room.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }
            boolean isMember = room.getMembers().stream().anyMatch(user -> user.getId().equals(currentUserId));
            if (!isMember) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("채팅방 멤버만 조회할 수 있습니다.");
            }

            // Service에서 메시지 DTO 리스트를 직접 받아옴
            List<ChatMessageDto> messageDtos = chatService.getChatMessages(roomId);

            return ResponseEntity.ok(messageDtos);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("메시지 로딩 중 오류: " + e.getMessage());
        }
    }

    /**
     * 채팅방 관리 정보 (강퇴할 멤버 목록) (JSON)
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
            ChatRoom room = chatService.getChatRoomDetails(roomId);

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
     * 채팅방에 초대할 멤버 목록 (JSON)
     */
    @GetMapping("/{clubId}/chat/{roomId}/invitable-members")
    public ResponseEntity<?> getInvitableMembersApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            ChatRoom room = chatService.getChatRoomDetails(roomId);
            if (room.getOwner() == null || !room.getOwner().getId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("방장만 접근할 수 있습니다.");
            }
            if (!room.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("모임 정보가 일치하지 않습니다.");
            }

            List<ManageableMemberDto> invitableMembers = chatService.getInvitableMembers(roomId, clubId);
            return ResponseEntity.ok(invitableMembers);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 멤버 초대 (JSON)
     * @return 새로 초대된 멤버 수
     */
    @PostMapping("/{clubId}/chat/{roomId}/invite")
    public ResponseEntity<?> inviteMembersApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            @RequestParam("memberIds") List<Long> memberIds,
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

            int addedCount = chatService.inviteMembers(roomId, currentUserId, memberIds);
            return ResponseEntity.ok().body(addedCount); // 새로 추가된 인원 수 반환

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 멤버 강퇴 (JSON)
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
     * 밴 목록 조회 (JSON)
     */
    @GetMapping("/{clubId}/chat/{roomId}/banned-members")
    public ResponseEntity<?> getBannedMembersApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");

        try {
            List<ManageableMemberDto> banned = chatService.getBannedMembers(roomId, currentUserId);
            return ResponseEntity.ok(banned);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 밴 해제 (JSON)
     */
    @PostMapping("/{clubId}/chat/{roomId}/unban")
    public ResponseEntity<?> unbanMemberApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            @RequestParam("memberId") Long memberToUnbanId,
            HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");

        try {
            chatService.unbanMember(roomId, currentUserId, memberToUnbanId);
            return ResponseEntity.ok("밴을 해제했습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    /**
     * 채팅방 나가기 (스스로) (JSON)
     */
    @PostMapping("/{clubId}/chat/{roomId}/leave")
    public ResponseEntity<?> leaveChatRoomApi(
            @PathVariable Long clubId,
            @PathVariable Long roomId,
            HttpSession session) {
        Long currentUserId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");

        try {
            chatService.leaveChatRoom(roomId, currentUserId);
            return ResponseEntity.ok("채팅방을 나갔습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 채팅방 삭제 (JSON)
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