package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.RegionKor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegionKorRepository extends JpaRepository<RegionKor, Long> {

    // 도 목록 (중복 제거 + 정렬)
    @Query("select distinct r.regionDo from RegionKor r order by r.regionDo")
    List<String> findAllDistinctDo();

    // 특정 도의 시/군/구 목록 (중복 제거 + 정렬)
    @Query("select distinct r.regionSi from RegionKor r where r.regionDo = :regionDo order by r.regionSi")
    List<String> findSisByDo(@Param("regionDo") String regionDo);
}
