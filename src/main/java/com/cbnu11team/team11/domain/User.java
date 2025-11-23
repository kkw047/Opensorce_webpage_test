package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users",
        indexes = @Index(name = "ix_users_region_do", columnList = "region_do"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인용 별도 아이디(선택) */
    @Column(name = "login_id", length = 50, unique = true)
    private String loginId;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "region_do", length = 50)
    private String regionDo;

    @Column(name = "region_si", length = 50)
    private String regionSi;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "user_categories",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @OrderBy("name ASC")
    @Builder.Default
    private Set<Category> categories = new LinkedHashSet<>();

    // 유저가 가입한 모임 목록 (User -> ClubMember 1:N 관계)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private Set<ClubMember> clubMemberships = new LinkedHashSet<>();
}
