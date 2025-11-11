package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findAllByIdIn(Collection<Long> ids);
}
