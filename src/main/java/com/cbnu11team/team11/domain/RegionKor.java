package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "region_kor",
        indexes = @Index(name = "ix_region_kor_do", columnList = "region_do"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionKor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_do", length = 50, nullable = false)
    private String regionDo;

    @Column(name = "region_si", length = 50, nullable = false)
    private String regionSi;
}
