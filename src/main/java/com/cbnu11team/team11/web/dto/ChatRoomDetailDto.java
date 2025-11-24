package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ChatRoom;

/**
 * 채팅방 모달을 띄우기 위한 상세 정보를 담는 DTO (JSON 응답용)
 */
public record ChatRoomDetailDto(
        Long roomId,
        String roomName,
        Long roomOwnerId,
        int memberCount
) {
    public static ChatRoomDetailDto fromEntity(ChatRoom room) {
        return new ChatRoomDetailDto(
                room.getId(),
                room.getName(),
                room.getOwner() != null ? room.getOwner().getId() : null,
                room.getMembers().size()
        );
    }
}