package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // 소개글: 길이 제약 완화
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "region_do", nullable = false, length = 50)
    private String regionDo;

    @Column(name = "region_si", nullable = false, length = 50)
    private String regionSi;

    // ✅ DB의 longblob(LONGBLOB)과 매칭
    @Lob
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @ManyToMany
    @JoinTable(
            name = "club_categories",
            joinColumns = @JoinColumn(name = "club_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}
