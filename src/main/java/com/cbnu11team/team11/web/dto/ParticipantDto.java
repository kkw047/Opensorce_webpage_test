package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ParticipationStatus;
import com.cbnu11team.team11.domain.ScheduleParticipant;
import lombok.Getter;

@Getter
public class ParticipantDto {
    private Long participantId; // 참가 내역 ID (관리할 때 필요)
    private String nickname;    // 유저 닉네임
    private ParticipationStatus status; // 상태 (PENDING, ACCEPTED...)

    public ParticipantDto(ScheduleParticipant sp) {
        this.participantId = sp.getId();
        this.nickname = sp.getUser().getNickname();
        this.status = sp.getStatus();
    }
}