package com.cbnu11team.opensource11.club;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {

    // 이름 검색
    List<Club> findByNameContainingIgnoreCase(String q);

    // 카테고리 단건
    List<Club> findByCategory(String category);

    // 카테고리 + 이름 검색
    List<Club> findByCategoryAndNameContainingIgnoreCase(String category, String q);
}
