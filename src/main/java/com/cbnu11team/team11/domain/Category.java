package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "ux_categories_name", columnNames = "name"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    /** DB DEFAULT CURRENT_TIMESTAMP 사용 */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "categories")
    @ToString.Exclude
    private Set<Club> clubs = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "categories")
    @ToString.Exclude
    private Set<User> users = new LinkedHashSet<>();

    public Category(Long id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }
}
