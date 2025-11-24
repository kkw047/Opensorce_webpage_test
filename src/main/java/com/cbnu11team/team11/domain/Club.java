package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "clubs", indexes = {
        @Index(name = "ix_clubs_region_do", columnList = "region_do"),
        @Index(name = "ix_clubs_created_at", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    /** DV: longtext */
    @Lob
    @Column(columnDefinition = "longtext")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "region_do", length = 50, nullable = false)
    private String regionDo;

    @Column(name = "region_si", length = 50, nullable = false)
    private String regionSi;

    /** DV에 category_id(FK) 컬럼은 남아있지만, 다대다로 일관 구현 → 매핑하지 않음 */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    private User owner;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
            name = "club_categories",
            joinColumns = @JoinColumn(name = "club_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @OrderBy("name ASC")
    @Builder.Default
    private Set<Category> categories = new LinkedHashSet<>();

    //멤버 목록 (Club -> ClubMember 1:N 관계)
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("role ASC, joinedAt ASC") // role, 가입일 오름차순
    @Builder.Default
    @ToString.Exclude
    private List<ClubMember> members = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy")
    @Builder.Default
    private ClubJoinPolicy joinPolicy = ClubJoinPolicy.AUTOMATIC;
}
