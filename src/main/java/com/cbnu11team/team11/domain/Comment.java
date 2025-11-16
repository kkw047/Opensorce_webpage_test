package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "ix_comments_post_id", columnList = "post_id"),
        @Index(name = "ix_comments_user_id", columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob // 긴 텍스트를 위한 @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- 관계 매핑 ---

    //작성자 (Comment N : 1 User) (N+1 문제 방지를 위해 LAZY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK: user_id
    @ToString.Exclude
    private User author;

    //소속 게시글 (Comment N : 1 Post) (N+1 문제 방지를 위해 LAZY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false) // FK: post_id
    @ToString.Exclude
    private Post post;
}