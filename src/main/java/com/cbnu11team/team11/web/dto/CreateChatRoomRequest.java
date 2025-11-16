package com.cbnu11team.team11.web.dto;

import java.util.List;

// 채팅방 생성 폼에서 받을 데이터
public record CreateChatRoomRequest(
        String roomName,
        List<Long> memberIds // 초대할 멤버들의 User ID 리스트
) {}