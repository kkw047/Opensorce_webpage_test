package com.cbnu11team.team11.domain; // 1. domain 패키지로 변경

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts", indexes = { // 2. User/Club처럼 복수형 테이블명 사용
        @Index(name = "ix_posts_user_id", columnList = "user_id"),
        @Index(name = "ix_posts_club_id", columnList = "club_id")
})
@Getter @Setter // 3. User/Club과 동일한 Lombok 어노테이션 사용
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // 4. PK 생성 전략 동일하게
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    /** Club.description의 패턴을 따라 longtext로 지정 */
    @Lob
    @Column(columnDefinition = "longtext", nullable = false)
    private String content;

    /** * Club.createdAt의 스키마를 따름
     * (DB에서 생성 시 타임스탬프를 찍는다고 가정)
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- 관계 매핑 ---

    /**
     * 작성자 (Post N : 1 User)
     * Club.owner의 패턴(Lazy, ToString.Exclude)을 따름
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK: user_id
    @ToString.Exclude
    private User author;

    /**
     * 소속 클럽 (Post N : 1 Club)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false) // FK: club_id
    @ToString.Exclude
    private Club club;
}