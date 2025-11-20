package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "ix_posts_user_id", columnList = "user_id"),
        @Index(name = "ix_posts_club_id", columnList = "club_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "longtext", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- 관계 매핑 ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK: user_id
    @ToString.Exclude
    private User author;

    //소속 클럽 (Post N : 1 Club)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false) // FK: club_id
    @ToString.Exclude
    private Club club;

    //이 게시물에 달린 댓글 목록
    @OneToMany(mappedBy = "post", /* 'Comment' 엔티티의 'post' 필드에 매핑됨 */
            cascade = CascadeType.ALL, /* 게시글이 삭제되면 댓글도 삭제 (V7의 ON DELETE CASCADE와 일치) */
            orphanRemoval = true) /* 목록에서 댓글이 제거되면 DB에서도 삭제 */
    @Builder.Default
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();
}