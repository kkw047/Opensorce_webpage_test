package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.RegionKor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Region 도/시 목록 조회용 레포
 */
public interface RegionKorRepository extends JpaRepository<RegionKor, Long> {

    @Query("select distinct r.regionDo from RegionKor r order by r.regionDo")
    List<String> findDistinctRegionDoOrderByRegionDoAsc();

    @Query("select distinct r.regionSi from RegionKor r where r.regionDo = :regionDo order by r.regionSi")
    List<String> findDistinctRegionSiByRegionDoOrderByRegionSiAsc(@Param("regionDo") String regionDo);
}
