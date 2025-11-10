package com.cbnu11team.team11.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class RegionQueryRepository {
    private final JdbcTemplate jdbc;
    public RegionQueryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<String> findDistinctDo() {
        return jdbc.queryForList(
                "select distinct region_do from region_kor order by region_do", String.class);
    }

    public List<String> findSiByDo(String regionDo) {
        return jdbc.queryForList(
                "select distinct region_si from region_kor where region_do = ? order by region_si",
                String.class, regionDo);
    }
}
