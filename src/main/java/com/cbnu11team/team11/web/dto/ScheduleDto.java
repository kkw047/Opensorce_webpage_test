package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.Calendar;
// [중요] 이 import 문이 꼭 있어야 합니다!
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class ScheduleDto {

    private Long id;
    private String title;
    private String start;
    private String end;
    private String fee;
    private String details;

    // [핵심 수정] JSON으로 나갈 때 이름을 "isManager"로 고정!
    @JsonProperty("isManager")
    private boolean isManager;

    // [핵심 수정] 마찬가지로 고정
    @JsonProperty("isParticipating")
    private boolean isParticipating;

    private String myStatus;
    private List<ParticipantDto> participants;

    // 생성자
    public ScheduleDto(Calendar entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.start = entity.getStartDate().toLocalDate().toString();
        this.end = entity.getEndDate().toLocalDate().toString();
        this.fee = entity.getFee();
        this.details = entity.getDescription();
    }

    @Data
    @NoArgsConstructor
    public static class ParticipantDto {
        private Long participantId;
        private Long userId;
        private String nickname;
        private String status;

        @JsonProperty("isConfirmed") // 혹시 모르니 여기도 추가
        private boolean isConfirmed;

        public ParticipantDto(com.cbnu11team.team11.domain.CalendarParticipant p) {
            this.participantId = p.getId();
            this.userId = p.getUser().getId();
            this.nickname = p.getUser().getNickname();
            this.status = p.getStatus().name();
            this.isConfirmed = p.isConfirmed();
        }
    }

    @Data
    public static class Request {
        private String title;
        private String start;
        private String end;
        private String fee;
        private String details;
    }
}