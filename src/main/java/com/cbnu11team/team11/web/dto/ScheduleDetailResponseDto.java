package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ClubSchedule;
import com.cbnu11team.team11.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty; // [1] import 추가
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ScheduleDetailResponseDto {

    private Long id;
    private String title;
    private String details;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fee;
    private String creatorName;
    private List<String> participantNames;

    // [2] 이름 강제 고정! (isParticipating -> isParticipating)
    @JsonProperty("isParticipating")
    private boolean isParticipating;

    // [3] 이름 강제 고정! (isManager -> isManager)
    @JsonProperty("isManager")
    private boolean isManager;

    public ScheduleDetailResponseDto(ClubSchedule schedule, List<User> participants, boolean isParticipating, boolean isManager) {
        this.id = schedule.getId();
        this.title = schedule.getTitle();
        this.details = schedule.getDetails();
        this.startDate = schedule.getStartDate();
        this.endDate = schedule.getEndDate();
        this.fee = schedule.getFee();
        this.creatorName = schedule.getCreator().getNickname();
        this.participantNames = participants.stream()
                .map(User::getNickname)
                .collect(Collectors.toList());

        this.isParticipating = isParticipating;
        this.isManager = isManager;
    }
}