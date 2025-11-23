package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 모든 댓글을 조회
     * (N+1 문제 방지를 위해 작성자(author) 정보를 JOIN FETCH로 함께 가져옴)
     *
     * @param postId 게시글 ID
     * @return 작성자 정보가 포함된 댓글 목록 (오래된 순)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);
}