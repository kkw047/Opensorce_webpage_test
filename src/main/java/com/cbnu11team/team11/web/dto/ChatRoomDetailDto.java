package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ChatRoom;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 채팅방 모달을 띄우기 위한 상세 정보를 담는 DTO (JSON 응답용)
 */
public record ChatRoomDetailDto(
        Long roomId,
        String roomName,
        Long roomOwnerId
) {
    public static ChatRoomDetailDto fromEntity(ChatRoom room) {
        return new ChatRoomDetailDto(
                room.getId(),
                room.getName(),
                room.getOwner() != null ? room.getOwner().getId() : null
        );
    }
}