package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Post;
import com.cbnu11team.team11.web.dto.PostForm;

import java.util.List;
import java.util.Optional;

public interface PostService {

    /**
     * 특정 클럽의 게시물 목록을 조회합니다.
     */
    List<Post> getPostsByClubId(Long clubId);

    // ID로 게시물 1건 조회
    Optional<Post> findPostById(Long postId);

    /**
     * 새 게시물을 생성하고 저장합니다.
     */
    Post createPost(Long clubId, PostForm postForm, Long userId);
}