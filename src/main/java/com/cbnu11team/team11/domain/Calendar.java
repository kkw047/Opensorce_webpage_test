package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calendars")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calendar {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description; // 상세 내용 (details)

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    private String fee; // 참여비

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 작성자 (관리자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    // 참가자 목록
    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalendarParticipant> participants = new ArrayList<>();

    public void update(String title, String description, LocalDateTime start, LocalDateTime end, String fee) {
        this.title = title;
        this.description = description;
        this.startDate = start;
        this.endDate = end;
        this.fee = fee;
    }

    @Column(columnDefinition = "boolean default false")
    private boolean isDone; // [추가]

    public void toggleDone() { this.isDone = !this.isDone; }
    public boolean isDone() { return isDone; }
}