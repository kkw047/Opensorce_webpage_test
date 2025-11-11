package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginIdIgnoreCase(String loginId);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByLoginId(String loginId);
}
