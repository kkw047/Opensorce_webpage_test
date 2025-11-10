package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    // 최신순 카드 목록 (카테고리 N+1 줄이기)
    @EntityGraph(attributePaths = "categories")
    Page<Club> findAllByOrderByIdDesc(Pageable pageable);

    // 검색(키워드/지역/카테고리) + 최신순
    @EntityGraph(attributePaths = "categories")
    @Query(
            value = """
            select distinct c
            from Club c
            left join c.categories cat
            where (:kw is null or lower(c.name) like lower(concat('%', :kw, '%'))
                   or lower(c.description) like lower(concat('%', :kw, '%')))
              and (:rdo is null or c.regionDo = :rdo)
              and (:rsi is null or c.regionSi = :rsi)
              and (:hasCat = false or cat.id in :categoryIds)
            order by c.id desc
        """,
            countQuery = """
            select count(distinct c)
            from Club c
            left join c.categories cat
            where (:kw is null or lower(c.name) like lower(concat('%', :kw, '%'))
                   or lower(c.description) like lower(concat('%', :kw, '%')))
              and (:rdo is null or c.regionDo = :rdo)
              and (:rsi is null or c.regionSi = :rsi)
              and (:hasCat = false or cat.id in :categoryIds)
        """
    )
    Page<Club> search(
            @Param("kw") String keyword,
            @Param("rdo") String regionDo,
            @Param("rsi") String regionSi,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("hasCat") boolean hasCat,
            Pageable pageable
    );
}
