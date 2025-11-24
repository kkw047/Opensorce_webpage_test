package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방 ID로 메시지 조회 (작성자 정보 함께 로딩, 오래된 순 정렬)
    // Fetch Join을 사용하여 N+1 문제를 방지하고, 강퇴된 유저의 메시지도 정상 조회
    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.sender WHERE m.chatRoom.id = :roomId ORDER BY m.sentAt ASC")
    List<ChatMessage> findByChatRoomIdWithSender(@Param("roomId") Long roomId);
}