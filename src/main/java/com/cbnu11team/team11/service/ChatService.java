package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.*;
import com.cbnu11team.team11.web.dto.ChatRoomDetailDto;
import com.cbnu11team.team11.web.dto.ChatRoomListDto;
import com.cbnu11team.team11.web.ChatApiController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator; // [추가]
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;

    /**
     * 모임의 모든 채팅방 목록 조회 (방장 닉네임 포함)
     */
    @Transactional(readOnly = true)
    public List<ChatRoomListDto> getChatRoomsByClub(Long clubId, Long currentUserId) {
        List<ChatRoom> rooms = chatRoomRepository.findByClubId(clubId); // members와 owner를 Eager 로딩

        return rooms.stream().map(room -> {
            boolean isMember = room.getMembers().stream()
                    .anyMatch(user -> user.getId().equals(currentUserId));

            String ownerNickname = (room.getOwner() != null) ? room.getOwner().getNickname() : "(알 수 없음)";

            return new ChatRoomListDto(
                    room.getId(),
                    room.getName(),
                    ownerNickname,
                    room.getMembers().size(),
                    isMember
            );
        }).collect(Collectors.toList());
    }

    /**
     * 특정 채팅방 상세 정보 (엔티티 반환)
     */
    @Transactional(readOnly = true)
    public ChatRoom getChatRoomDetails(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
    }

    /**
     * 특정 채팅방 상세 정보 (DTO 반환, 보안 검사 포함)
     * API용
     */
    @Transactional(readOnly = true)
    public ChatRoomDetailDto getChatRoomDetailsDto(Long roomId, Long currentUserId) {
        ChatRoom room = this.getChatRoomDetails(roomId);

        // 이 채팅방의 멤버가 맞는지 확인
        boolean isMember = room.getMembers().stream().anyMatch(user -> user.getId().equals(currentUserId));
        if (!isMember) {
            throw new IllegalStateException("채팅방 멤버만 조회할 수 있습니다.");
        }

        return ChatRoomDetailDto.fromEntity(room);
    }

    /**
     * 채팅방 생성
     */
    @Transactional
    public ChatRoom createChatRoom(Long clubId, Long creatorUserId, String roomName, List<Long> memberUserIds) {
        if (memberUserIds == null || memberUserIds.isEmpty()) {
            throw new IllegalArgumentException("최소 1명 이상의 멤버를 초대해야 합니다.");
        }
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException("채팅방 이름은 필수입니다.");
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));
        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Set<Long> allMemberIds = memberUserIds.stream().collect(Collectors.toSet());
        allMemberIds.add(creatorUserId);
        for (Long userId : allMemberIds) {
            ClubMemberId memberId = new ClubMemberId(clubId, userId);
            if (!clubMemberRepository.existsById(memberId)) {
                throw new IllegalStateException("모임 멤버가 아닌 사용자를 초대할 수 없습니다.");
            }
        }
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setClub(club);
        chatRoom.setOwner(creator);
        chatRoom.setName(roomName.trim());
        List<User> members = userRepository.findAllById(allMemberIds);
        chatRoom.getMembers().addAll(members);
        return chatRoomRepository.save(chatRoom);
    }

    /**
     * 채팅방 가입
     */
    @Transactional
    public void joinChatRoom(Long roomId, Long currentUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ClubMemberId clubMemberId = new ClubMemberId(room.getClub().getId(), currentUserId);
        if (!clubMemberRepository.existsById(clubMemberId)) {
            throw new IllegalStateException("모임 멤버만 채팅방에 가입할 수 있습니다.");
        }
        boolean alreadyChatMember = room.getMembers().stream()
                .anyMatch(m -> m.getId().equals(currentUserId));
        if (alreadyChatMember) {
            throw new IllegalStateException("이미 가입한 채팅방입니다.");
        }
        room.getMembers().add(user);
    }

    /**
     * 채팅방에 초대 가능한 모임 멤버 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatApiController.ManageableMemberDto> getInvitableMembers(Long roomId, Long clubId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));

        // 현재 채팅방 멤버 ID 목록
        Set<Long> chatMemberIds = room.getMembers().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // 모임 멤버 중 채팅방에 없는 멤버 필터링
        return club.getMembers().stream()
                .map(ClubMember::getUser)
                .filter(user -> !chatMemberIds.contains(user.getId()))
                .sorted(Comparator.comparing(User::getNickname))
                .map(user -> new ChatApiController.ManageableMemberDto(user.getId(), user.getNickname(), user.getEmail()))
                .collect(Collectors.toList());
    }

    /**
     * 멤버들을 채팅방에 초대 (방장 권한)
     * @return 실제로 새로 추가된 멤버 수
     */
    @Transactional
    public int inviteMembers(Long roomId, Long ownerUserId, List<Long> memberIdsToInvite) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (room.getOwner() == null || !room.getOwner().getId().equals(ownerUserId)) {
            throw new IllegalStateException("방장만 멤버를 초대할 수 있습니다.");
        }

        if (memberIdsToInvite == null || memberIdsToInvite.isEmpty()) {
            throw new IllegalArgumentException("초대할 멤버를 선택해주세요.");
        }

        Set<Long> chatMemberIds = room.getMembers().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // 초대할 ID 목록 중 이미 멤버가 아닌 ID만 필터링
        List<Long> newMemberIds = memberIdsToInvite.stream()
                .filter(id -> !chatMemberIds.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if (newMemberIds.isEmpty()) {
            throw new IllegalArgumentException("이미 모두 채팅방 멤버이거나, 유효하지 않은 멤버입니다.");
        }

        // (보안) 이들이 모임 멤버인지 재확인
        Long clubId = room.getClub().getId();
        for (Long userId : newMemberIds) {
            ClubMemberId memberId = new ClubMemberId(clubId, userId);
            if (!clubMemberRepository.existsById(memberId)) {
                throw new IllegalStateException("모임 멤버가 아닌 사용자를 초대할 수 없습니다.");
            }
        }

        // User 엔티티 조회 및 추가
        List<User> newMembers = userRepository.findAllById(newMemberIds);
        room.getMembers().addAll(newMembers);
        // chatRoomRepository.save(room); // @Transactional이므로 save는 생략 가능

        return newMembers.size(); // 새로 추가된 멤버 수 반환
    }


    /**
     * 멤버 강퇴
     */
    @Transactional
    public void kickMember(Long roomId, Long ownerUserId, Long memberToKickId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        if (room.getOwner() == null || !room.getOwner().getId().equals(ownerUserId)) {
            throw new IllegalStateException("방장만 멤버를 강퇴할 수 있습니다.");
        }
        if (ownerUserId.equals(memberToKickId)) {
            throw new IllegalArgumentException("방장은 스스로를 강퇴할 수 없습니다.");
        }
        boolean removed = room.getMembers().removeIf(user -> user.getId().equals(memberToKickId));
        if (!removed) {
            throw new IllegalArgumentException("해당 멤버가 채팅방에 없거나 찾을 수 없습니다.");
        }
    }


    /**
     * 메시지 전송
     */
    @Transactional
    public ChatMessage sendMessage(Long roomId, Long senderUserId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        boolean isMember = room.getMembers().stream().anyMatch(user -> user.getId().equals(senderUserId));
        if (!isMember) {
            throw new IllegalStateException("채팅방 멤버만 메시지를 보낼 수 있습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }

        ChatMessage message = new ChatMessage();
        message.setChatRoom(room);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setSentAt(LocalDateTime.now());

        return chatMessageRepository.save(message); // 저장된 엔티티 반환
    }

    /**
     * 채팅방 삭제 (방장 전용)
     */
    @Transactional
    public void deleteChatRoom(Long roomId, Long currentUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        if (room.getOwner() == null || !room.getOwner().getId().equals(currentUserId)) {
            throw new IllegalStateException("채팅방 삭제 권한이 없습니다.");
        }
        chatRoomRepository.delete(room);
    }
}