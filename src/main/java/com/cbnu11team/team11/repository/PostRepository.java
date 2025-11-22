package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author WHERE p.club.id = :clubId",
            countQuery = "SELECT count(p) FROM Post p WHERE p.club.id = :clubId")
    Page<Post> findPostsWithAuthorByClubId(@Param("clubId") Long clubId, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.images WHERE p.id = :postId")
    Optional<Post> findPostWithAuthorById(@Param("postId") Long postId);
}