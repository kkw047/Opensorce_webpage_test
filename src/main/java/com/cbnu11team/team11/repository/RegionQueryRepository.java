package com.cbnu11team.team11.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RegionQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<String> findAllDos() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT region_do FROM region_kor ORDER BY region_do", String.class);
    }

    public List<String> findSisByDo(String regionDo) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT region_si FROM region_kor WHERE region_do=? ORDER BY region_si",
                String.class, regionDo);
    }
}
