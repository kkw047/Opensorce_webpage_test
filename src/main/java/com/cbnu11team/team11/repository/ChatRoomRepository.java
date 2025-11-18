package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.ChatRoom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 클럽 ID로 채팅방 목록 조회 (멤버 정보도 함께)
    @EntityGraph(attributePaths = {"members", "owner"}) // owner도 Eager 로딩
    List<ChatRoom> findByClubId(Long clubId);

    // 채팅방 ID로 조회 (멤버와 메시지, 메시지 작성자까지 EAGER 조회)
    @EntityGraph(attributePaths = {"owner", "members", "messages", "messages.sender"})
    Optional<ChatRoom> findById(Long id);
}