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

    // 1. 이 참가 '정보'가 어떤 '유저'에 대한 것인지 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 2. 이 참가 '정보'가 어떤 '일정'에 대한 것인지 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_schedule_id")
    private ClubSchedule clubSchedule;

    // 3. (선택 사항) 나중에 '참여비 입금 여부' 같은 추가 정보가 필요하면
    // private boolean paid; // 여기에 필드를 추가하면 됩니다.
}