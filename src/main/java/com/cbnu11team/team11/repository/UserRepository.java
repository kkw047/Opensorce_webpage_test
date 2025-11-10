package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginIdIgnoreCase(String loginId);
    Optional<User> findByLoginId(String loginId);
}
