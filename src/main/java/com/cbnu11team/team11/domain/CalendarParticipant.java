package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "calendar_participants")
public class CalendarParticipant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status; // PENDING, ACCEPTED, REJECTED

    private boolean isConfirmed; // 참여 확정 여부

    @Column(columnDefinition = "boolean default false")
    private boolean isAttended; // [추가]

    public void toggleAttended() { this.isAttended = !this.isAttended; }
    public boolean isAttended() { return isAttended; }
}