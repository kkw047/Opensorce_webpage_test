package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.Post;
import com.cbnu11team.team11.domain.User;

import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.PostRepository;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.web.dto.PostForm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsByClubId(Long clubId) {
        return postRepository.findPostsWithAuthorByClubId(clubId);
    }

    @Override
    @Transactional
    public Post createPost(Long clubId, PostForm postForm, Long userId) {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NoSuchElementException("해당 클럽을 찾을 수 없습니다. ID: " + clubId));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        Post newPost = Post.builder()
                .title(postForm.title())
                .content(postForm.content())
                .club(club)
                .author(author)
                .build();

        return postRepository.save(newPost);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findPostById(Long postId) {
        return postRepository.findPostWithAuthorById(postId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        // 게시물을 찾기
        Post post = postRepository.findPostWithAuthorById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다. ID: " + postId));

        // 삭제 권한 확인
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new SecurityException("게시물을 삭제할 권한이 없습니다.");
        }

        // 권한이 있으면 삭제
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void updatePost(Long postId, PostForm postForm, Long currentUserId) {
        //게시물 찾기
        Post post = postRepository.findPostWithAuthorById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다. ID: " + postId));

        //수정 권한 확인
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new SecurityException("게시물을 수정할 권한이 없습니다.");
        }

        //폼 DTO의 새 데이터로 엔티티의 값을 변경
        post.setTitle(postForm.title());
        post.setContent(postForm.content());
    }
}