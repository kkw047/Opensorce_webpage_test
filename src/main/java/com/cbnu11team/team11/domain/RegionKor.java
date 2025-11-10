package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 행정구역(도/시군구) 테이블
 * 컬럼명은 Flyway 스키마의 region_kor(region_do, region_si)와 매칭.
 */
@Entity
@Table(name = "region_kor")
@Getter
@Setter
@NoArgsConstructor
public class RegionKor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_do", nullable = false, length = 50)
    private String regionDo;

    @Column(name = "region_si", nullable = false, length = 50)
    private String regionSi;
}
