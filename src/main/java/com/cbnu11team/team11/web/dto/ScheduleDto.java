package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.Calendar;
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
    private String writer; // 작성자 닉네임

    // [권한/상태 관련 Boolean 필드]
    // Jackson이 'is'를 떼어버리는 것을 방지하기 위해 @JsonProperty 명시

    @JsonProperty("isManager")
    private boolean isManager;        // 관리자 여부 (수정/삭제/승인 권한)

    @JsonProperty("isParticipating")
    private boolean isParticipating;  // 내가 참가 중인지

    @JsonProperty("isDone")
    private boolean isDone;           // 완료(출석) 여부 (개인: 완료, 모임: 출석)

    @JsonProperty("isAttendanceActive")
    private boolean isAttendanceActive; // [추가] 관리자가 출석 체크를 활성화했는지

    private String myStatus;          // 나의 참가 상태 (ACCEPTED, PENDING...)
    private List<ParticipantDto> participants;

    // Entity -> DTO 변환 생성자
    public ScheduleDto(Calendar entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.start = entity.getStartDate().toLocalDate().toString();
        this.end = entity.getEndDate().toLocalDate().toString();
        this.fee = entity.getFee();
        this.details = entity.getDescription();

        // 기본값 매핑
        this.isDone = entity.isDone(); // 개인 일정일 경우 엔티티 값 사용
        this.isAttendanceActive = entity.isAttendanceActive(); // 출석 활성화 여부

        if (entity.getUser() != null) {
            this.writer = entity.getUser().getNickname();
        }
    }

    @Data
    @NoArgsConstructor
    public static class ParticipantDto {
        private Long participantId;
        private Long userId;
        private String nickname;
        private String status;

        @JsonProperty("isConfirmed")
        private boolean isConfirmed; // 참가 확정 여부

        @JsonProperty("isAttended")
        private boolean isAttended;  // [추가] 개별 출석 여부

        public ParticipantDto(com.cbnu11team.team11.domain.CalendarParticipant p) {
            this.participantId = p.getId();
            this.userId = p.getUser().getId();
            this.nickname = p.getUser().getNickname();
            this.status = p.getStatus().name();
            this.isConfirmed = p.isConfirmed();
            this.isAttended = p.isAttended(); // 엔티티에서 가져옴
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