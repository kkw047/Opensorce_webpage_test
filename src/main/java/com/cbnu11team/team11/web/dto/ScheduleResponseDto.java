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
    private Long clubId; // [추가]

    public ScheduleResponseDto(ClubSchedule schedule) {
        this.id = schedule.getId();
        this.title = schedule.getTitle();
        this.start = schedule.getStartDate();
        this.end = schedule.getEndDate().plusDays(1);
        this.clubId = schedule.getClub().getId(); // [추가]
    }
}