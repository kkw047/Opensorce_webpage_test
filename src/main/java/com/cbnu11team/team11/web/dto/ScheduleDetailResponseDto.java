package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ClubSchedule;
import com.cbnu11team.team11.domain.ParticipationStatus;
import com.cbnu11team.team11.domain.ScheduleParticipant;
import com.cbnu11team.team11.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty; // [필수] 이거 import 하세요!
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

    private List<ParticipantDto> participants;

    // [핵심 수정] JSON으로 변환될 때 이름을 'isParticipating'으로 고정!
    @JsonProperty("isParticipating")
    private boolean isParticipating;

    private ParticipationStatus myStatus;

    // [핵심 수정] JSON으로 변환될 때 이름을 'isManager'로 고정!
    // 이게 없으면 자바스크립트가 값을 못 읽어서 버튼이 안 보입니다.
    @JsonProperty("isManager")
    private boolean isManager;

    public ScheduleDetailResponseDto(ClubSchedule schedule, List<ScheduleParticipant> participantEntities, ScheduleParticipant myParticipation, boolean isManager) {
        this.id = schedule.getId();
        this.title = schedule.getTitle();
        this.details = schedule.getDetails();
        this.startDate = schedule.getStartDate();
        this.endDate = schedule.getEndDate();
        this.fee = schedule.getFee();
        this.creatorName = schedule.getCreator().getNickname();

        this.participants = participantEntities.stream()
                .map(ParticipantDto::new)
                .collect(Collectors.toList());

        if (myParticipation != null) {
            this.isParticipating = true;
            this.myStatus = myParticipation.getStatus();
        } else {
            this.isParticipating = false;
            this.myStatus = null;
        }

        this.isManager = isManager;
    }
}