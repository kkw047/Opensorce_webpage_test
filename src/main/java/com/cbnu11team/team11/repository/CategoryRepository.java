package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("select c from Category c order by c.name asc")
    List<Category> findAllOrderByNameAsc();

    List<Category> findAllByIdIn(List<Long> ids);
}
