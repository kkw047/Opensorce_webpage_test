package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.ChatRoomBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface ChatRoomBanRepository extends JpaRepository<ChatRoomBan, Long> {

    // 특정 채팅방에서 특정 유저가 밴 되었는지 확인
    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    // 밴 해제를 위해 조회
    Optional<ChatRoomBan> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    // 밴 목록 조회 (User 정보도 함께 로딩)
    @EntityGraph(attributePaths = {"user"})
    List<ChatRoomBan> findAllByChatRoomId(Long chatRoomId);
}