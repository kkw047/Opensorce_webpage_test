package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

    // 메인 목록 + 카테고리까지 즉시 로딩(템플릿 Lazy 예외 방지)
    @EntityGraph(attributePaths = {"categories"})
    Page<Club> findAllByOrderByIdDesc(Pageable pageable);

    // 서비스에서 호출하는 메서드 (컴파일 에러 해결용, 위와 동일 효과)
    @EntityGraph(attributePaths = {"categories"})
    @Query("select c from Club c")
    Page<Club> findAllWithCategories(Pageable pageable);

    // 지역/키워드 검색(카테고리 즉시 로딩)
    @EntityGraph(attributePaths = {"categories"})
    @Query("""
           select c from Club c
           where (:doVal is null or c.regionDo = :doVal)
             and (:siVal is null or c.regionSi = :siVal)
             and (
                  :keyword is null
                  or lower(c.name) like lower(concat('%', :keyword, '%'))
                  or lower(c.description) like lower(concat('%', :keyword, '%'))
                 )
           """)
    Page<Club> search(@Param("doVal") String regionDo,
                      @Param("siVal") String regionSi,
                      @Param("keyword") String keyword,
                      Pageable pageable);
}
