package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Comment;
import com.cbnu11team.team11.web.dto.CommentForm;

import java.util.List;

public interface CommentService {

    List<Comment> getCommentsByPostId(Long postId);

    Comment createComment(Long postId, Long userId, CommentForm form);

    // 댓글 삭제 (권한 검사 포함)
    void deleteComment(Long commentId, Long currentUserId);

    // 댓글 수정
    void updateComment(Long commentId, CommentForm commentForm, Long currentUserId);

    // 단일 댓글 조회
    Comment getCommentById(Long commentId);
}