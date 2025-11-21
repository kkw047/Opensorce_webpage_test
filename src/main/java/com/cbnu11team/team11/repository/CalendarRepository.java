package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {

    List<Calendar> findAllByClubId(Long clubId);

    // [기존 삭제] findAllByUserIdAndClubIsNull -> 이제 안 씁니다.

    // [신규 추가] 내가 만든 일정 OR 내가 참가 신청한 일정 모두 조회 (중복 제거 DISTINCT)
    @Query("SELECT DISTINCT c FROM Calendar c " +
            "LEFT JOIN c.participants p " +
            "WHERE c.user.id = :userId OR p.user.id = :userId")
    List<Calendar> findAllRelatedToUser(@Param("userId") Long userId);
}