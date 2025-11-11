package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query(
            value = """
            select distinct c
            from Club c
            where (:rdo is null or c.regionDo = :rdo)
              and (:rsi is null or c.regionSi = :rsi)
              and (
                    :kw is null or
                    lower(c.name) like concat('%', lower(:kw), '%') or
                    lower(coalesce(cast(c.description as string), '')) like concat('%', lower(:kw), '%')
                  )
              and ( :hasCats = false or exists (
                    select 1 from c.categories cat
                    where cat.id in :cats
                  ))
            """,
            countQuery = """
            select count(distinct c)
            from Club c
            where (:rdo is null or c.regionDo = :rdo)
              and (:rsi is null or c.regionSi = :rsi)
              and (
                    :kw is null or
                    lower(c.name) like concat('%', lower(:kw), '%') or
                    lower(coalesce(cast(c.description as string), '')) like concat('%', lower(:kw), '%')
                  )
              and ( :hasCats = false or exists (
                    select 1 from c.categories cat
                    where cat.id in :cats
                  ))
            """
    )
    Page<Club> search(
            @Param("rdo") String regionDo,
            @Param("rsi") String regionSi,
            @Param("kw")  String keyword,
            @Param("hasCats") boolean hasCats,
            @Param("cats") List<Long> categoryIds,
            Pageable pageable
    );
}
