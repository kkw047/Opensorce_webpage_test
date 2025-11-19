package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ParticipationStatus;
import com.cbnu11team.team11.domain.ScheduleParticipant;
import lombok.Getter;

@Getter
public class ParticipantDto {
    private Long participantId;
    private Long userId; // [추가] 프론트에서 '나'인지 확인용
    private String nickname;
    private ParticipationStatus status;
    private boolean isConfirmed; // [추가] 체크 여부

    public ParticipantDto(ScheduleParticipant sp) {
        this.participantId = sp.getId();
        this.userId = sp.getUser().getId(); // [추가]
        this.nickname = sp.getUser().getNickname();
        this.status = sp.getStatus();
        this.isConfirmed = sp.isConfirmed(); // [추가]
    }
}