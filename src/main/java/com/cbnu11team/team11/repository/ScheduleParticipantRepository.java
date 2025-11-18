package com.cbnu11team.team11.repository;


import com.cbnu11team.team11.domain.ClubSchedule;
import com.cbnu11team.team11.domain.ScheduleParticipant;
import com.cbnu11team.team11.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {

    // 1. 특정 일정(ClubSchedule)에 참가한 모든 참가자 '목록' 조회
    List<ScheduleParticipant> findAllByClubSchedule(ClubSchedule clubSchedule);

    // 2. 특정 유저(User)가 특정 일정(ClubSchedule)에 '참가했는지 확인'용
    Optional<ScheduleParticipant> findByUserAndClubSchedule(User user, ClubSchedule clubSchedule);

    // 3. (위와 동일한 기능, boolean 값만 반환)
    boolean existsByUserAndClubSchedule(User user, ClubSchedule clubSchedule);
}