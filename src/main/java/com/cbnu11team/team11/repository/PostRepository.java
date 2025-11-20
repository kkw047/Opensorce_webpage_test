package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.club.id = :clubId ORDER BY p.createdAt DESC")
    List<Post> findPostsWithAuthorByClubId(@Param("clubId") Long clubId);

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :postId")
    Optional<Post> findPostWithAuthorById(@Param("postId") Long postId);
}