package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"categories"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"categories"})
    Optional<User> findByLoginId(String loginId);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByLoginId(String loginId);
}
