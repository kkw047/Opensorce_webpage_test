package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Post;
import com.cbnu11team.team11.web.dto.PostForm;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface PostService {

    Page<Post> getPostsByClubId(Long clubId, int page);

    Optional<Post> findPostById(Long postId);

    Post createPost(Long clubId, PostForm postForm, Long userId);

    void deletePost(Long postId, Long currentUserId);

    void updatePost(Long postId, PostForm postForm, Long currentUserId);
}