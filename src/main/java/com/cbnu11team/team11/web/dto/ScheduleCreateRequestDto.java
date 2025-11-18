package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.ClubSchedule;
import com.cbnu11team.team11.domain.User;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class ScheduleCreateRequestDto {

    private String title;
    private String details;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fee;

    // 이 DTO의 정보를 바탕으로 실제 ClubSchedule Entity를 생성하는 헬퍼 메소드
    public ClubSchedule toEntity(Club club, User creator) {
        ClubSchedule schedule = new ClubSchedule();
        schedule.setTitle(title);
        schedule.setDetails(details);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        schedule.setFee(fee);
        schedule.setClub(club); // 이 일정이 속한 모임
        schedule.setCreator(creator); // 이 일정을 만든 사람
        return schedule;
    }
}