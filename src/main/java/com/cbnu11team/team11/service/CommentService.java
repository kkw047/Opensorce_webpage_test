package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Comment;
import com.cbnu11team.team11.web.dto.CommentForm;

import java.util.List;

public interface CommentService {

    /**
     * 특정 게시글의 댓글 목록을 조회합니다. (작성자 정보 포함)
     * @param postId 게시글 ID
     * @return 댓글 목록
     */
    List<Comment> getCommentsByPostId(Long postId);

    /**
     * 새 댓글을 생성합니다. (클럽 회원인지 검사 포함)
     * @param postId 댓글을 달 게시글 ID
     * @param userId 댓글 작성자 ID (세션에서 가져옴)
     * @param form 댓글 내용 DTO
     * @return 생성된 Comment 엔티티
     */
    Comment createComment(Long postId, Long userId, CommentForm form);

    // 댓글 삭제 (권한 검사 포함)
    void deleteComment(Long commentId, Long currentUserId);

    // 댓글 수정
    void updateComment(Long commentId, CommentForm commentForm, Long currentUserId);

    // 단일 댓글 조회
    Comment getCommentById(Long commentId);
}