package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.RegionKor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegionKorRepository extends JpaRepository<RegionKor, Long> {

    @Query("select distinct r.regionDo from RegionKor r where r.regionDo is not null order by r.regionDo asc")
    List<String> findAllDos();

    @Query("select distinct r.regionSi from RegionKor r where r.regionDo = :rdo and r.regionSi is not null order by r.regionSi asc")
    List<String> findSisByDo(@Param("rdo") String rdo);
}
