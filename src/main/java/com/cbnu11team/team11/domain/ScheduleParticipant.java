package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class ScheduleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_schedule_id")
    private ClubSchedule clubSchedule;

    // [추가] 참가 상태 (기본값: 대기)
    @Enumerated(EnumType.STRING)
    private ParticipationStatus status = ParticipationStatus.PENDING;
}