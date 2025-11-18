package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ClubSchedule;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class ScheduleResponseDto {

    private Long id;
    private String title;
    private LocalDate start;
    private LocalDate end;

    // [핵심 추가] 이 일정이 어느 모임 것인지 알려줘야 함!
    private Long clubId;

    public ScheduleResponseDto(ClubSchedule schedule) {
        this.id = schedule.getId();
        this.title = schedule.getTitle();
        this.start = schedule.getStartDate();
        this.end = schedule.getEndDate().plusDays(1);

        // [핵심 추가]
        this.clubId = schedule.getClub().getId();
    }
}