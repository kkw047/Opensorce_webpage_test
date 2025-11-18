package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.ClubSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClubScheduleRepository extends JpaRepository<ClubSchedule, Long> {

    // 1. 특정 모임(Club)의 캘린더용 일정 조회
    @Query("SELECT s FROM ClubSchedule s WHERE s.club = :club AND (" +
            "(s.startDate BETWEEN :monthStart AND :monthEnd) OR " +
            "(s.endDate BETWEEN :monthStart AND :monthEnd) OR " +
            "(s.startDate <= :monthStart AND s.endDate >= :monthEnd)" +
            ")")
    List<ClubSchedule> findSchedulesForCalendar(
            @Param("club") Club club,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd
    );

    /**
     * [수정] 2. 나만의 캘린더용 조회
     * 조건: (내가 참가자 명단에 있거나) OR (내가 생성자이거나)
     */
    @Query("SELECT DISTINCT s FROM ClubSchedule s " +
            "LEFT JOIN ScheduleParticipant sp ON s.id = sp.clubSchedule.id " +
            "WHERE (sp.user.id = :userId OR s.creator.id = :userId) " +
            "AND (s.startDate < :end AND s.endDate >= :start)")
    List<ClubSchedule> findJoinedSchedulesByUser(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}