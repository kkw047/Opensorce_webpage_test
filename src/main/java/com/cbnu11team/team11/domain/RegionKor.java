package com.cbnu11team.team11.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "region_kor")
public class RegionKor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_do", nullable = false, length = 50)
    private String regionDo;

    @Column(name = "region_si", nullable = false, length = 50)
    private String regionSi;

    // --- getters ---
    public Long getId() { return id; }
    public String getRegionDo() { return regionDo; }
    public String getRegionSi() { return regionSi; }
}
