package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 좌측 카테고리 필터/모임만들기 폼에서 사용
    List<Category> findAllByOrderByNameAsc();

    // 중복 검사
    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCase(String name);
}
