package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ChatRoom;
import com.cbnu11team.team11.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record ChatRoomListDto(
        Long id,
        String name,
        String ownerNickname, // 방장 닉네임
        int memberCount,
        boolean isMember, // 현재 사용자가 멤버인지 여부
        LocalDateTime lastActivityAt // 마지막 활동 시간
) {
}