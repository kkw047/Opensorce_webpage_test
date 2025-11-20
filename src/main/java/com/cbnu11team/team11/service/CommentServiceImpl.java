package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.*; // Comment, Post, User
import com.cbnu11team.team11.repository.ClubMemberRepository; // 멤버 검사용
import com.cbnu11team.team11.repository.CommentRepository;
import com.cbnu11team.team11.repository.PostRepository;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.web.dto.CommentForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository; // 멤버 검사를 위해 주입

    /**
     * 댓글 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdWithAuthor(postId);
    }

    /**
     * 새 댓글 생성
     */
    @Override
    @Transactional
    public Comment createComment(Long postId, Long userId, CommentForm form) {

        // 필요한 엔티티 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 작성할 게시글을 찾을 수 없습니다."));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("댓글 작성자를 찾을 수 없습니다."));

        // 클럽 멤버인지 검사
        Long clubId = post.getClub().getId();
        ClubMemberId memberId = new ClubMemberId(clubId, userId);

        boolean isMember = clubMemberRepository.existsById(memberId);

        if (!isMember) {
            // 멤버가 아니면 예외를 발생시켜 댓글 생성을 중단
            throw new SecurityException("댓글을 작성할 권한이 없습니다 (클럽 멤버가 아닙니다).");
        }

        // 댓글 엔티티 생성 및 저장
        Comment newComment = Comment.builder()
                .content(form.content())
                .post(post)
                .author(author)
                .build();

        return commentRepository.save(newComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        // 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 권한 검사
        if (!comment.getAuthor().getId().equals(currentUserId)) {
            throw new SecurityException("댓글을 삭제할 권한이 없습니다.");
        }

        // 삭제
        commentRepository.delete(comment);
    }

    // 단일 댓글 조회
    @Override
    @Transactional(readOnly = true)
    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    }

    // 댓글 수정
    @Override
    @Transactional
    public void updateComment(Long commentId, CommentForm commentForm, Long currentUserId) {
        Comment comment = getCommentById(commentId);

        // 권한 검사
        if (!comment.getAuthor().getId().equals(currentUserId)) {
            throw new SecurityException("댓글을 수정할 권한이 없습니다.");
        }

        // 내용 변경
        comment.setContent(commentForm.content());

        // @Transactional이 변경 감지하여 자동 UPDATE
    }
}