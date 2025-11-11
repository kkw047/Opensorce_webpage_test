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

    /**
     * 메인 목록: 최신 id 내림차순 + categories 미리 로딩
     * open-in-view=false 환경에서 템플릿에서 club.categories 접근 시 Lazy 방지
     */
    @EntityGraph(attributePaths = {"categories"})
    Page<Club> findAllByOrderByIdDesc(Pageable pageable);

    /**
     * 검색: 도/시군구/키워드/카테고리(다중)
     * - @EntityGraph 로 categories 미리 로딩 (Provider가 JOIN 또는 2nd query로 해결)
     * - fetch join을 쓰지 않아 페이징 경고(HHH90003004) 제거
     */
    @EntityGraph(attributePaths = {"categories"})
    @Query("""
        select distinct c
        from Club c
        left join c.categories cat
        where (:regionDo is null or c.regionDo = :regionDo)
          and (:regionSi is null or c.regionSi = :regionSi)
          and (
                :keyword is null
             or lower(c.name) like lower(concat('%', :keyword, '%'))
             or lower(c.description) like lower(concat('%', :keyword, '%'))
          )
          and (:hasCats = false or cat.id in :categoryIds)
        order by c.id desc
    """)
    List<Club> search(
            @Param("regionDo") String regionDo,
            @Param("regionSi") String regionSi,
            @Param("keyword") String keyword,
            @Param("hasCats") boolean hasCats,
            @Param("categoryIds") List<Long> categoryIds,
            Pageable pageable
    );
}
