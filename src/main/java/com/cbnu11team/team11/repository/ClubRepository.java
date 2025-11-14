package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long>, JpaSpecificationExecutor<Club> {

    @EntityGraph(attributePaths = {"categories"})
    @Override
    Page<Club> findAll(org.springframework.data.jpa.domain.Specification<Club> spec, Pageable pageable);

    List<Club> findByOwner(User owner);

    List<Club> findByRegionDoAndRegionSi(String regionDo, String regionSi);

    //상세 페이지용
    @EntityGraph(attributePaths = {"owner", "categories", "members", "members.user"})
    @Override
    Optional<Club> findById(Long id);

    // 사용자가 가입한 모임 목록을 조회하는 쿼리 추가
    @Query("SELECT c FROM Club c JOIN c.members cm WHERE cm.user.id = :userId")
    @EntityGraph(attributePaths = {"categories"}) // 카테고리 정보도 함께 조회
    Page<Club> findClubsByMemberId(@Param("userId") Long userId, Pageable pageable);
}