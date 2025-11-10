package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository // 붙여도/빼도 동작
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();          // 좌측/모임만들기용
    boolean existsByNameIgnoreCase(String name);       // 중복 검사
    Optional<Category> findByNameIgnoreCase(String name); // 필요 시 사용
}
