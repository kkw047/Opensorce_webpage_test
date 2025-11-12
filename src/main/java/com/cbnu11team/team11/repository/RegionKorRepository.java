package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.RegionKor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegionKorRepository extends JpaRepository<RegionKor, Long> {

    @Query("select distinct r.regionDo from RegionKor r order by r.regionDo asc")
    List<String> findDistinctDos();

    @Query("select distinct r.regionSi from RegionKor r where r.regionDo = ?1 order by r.regionSi asc")
    List<String> findSisByDo(String regionDo);
}
