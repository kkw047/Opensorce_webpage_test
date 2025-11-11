package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "clubs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Club {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Lob
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "region_do", length = 50, nullable = false)
    private String regionDo;

    @Column(name = "region_si", length = 80, nullable = false)
    private String regionSi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_at", columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "club_categories",
            joinColumns = @JoinColumn(name = "club_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new LinkedHashSet<>();
}
