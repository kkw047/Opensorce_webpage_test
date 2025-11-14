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

    // 의존성 주입
    private final PostRepository postRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    /**
     * [게시물 목록 조회]
     * PostRepository의 findByClubIdOrderByCreatedAtDesc 메서드를 호출합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsByClubId(Long clubId) {
        return postRepository.findPostsWithAuthorByClubId(clubId);
    }

    /**
     * [새 게시물 생성]
     */
    @Override
    @Transactional
    public Post createPost(Long clubId, PostForm postForm, Long userId) {

        // 1. 클럽 조회 (domain.Club)
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NoSuchElementException("해당 클럽을 찾을 수 없습니다. ID: " + clubId));

        // 2. 작성자 조회 (domain.User)
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        // 3. Post 엔티티 생성 (domain.Post)
        // Post 엔티티의 @Builder 사용
        Post newPost = Post.builder()
                .title(postForm.title())
                .content(postForm.content())
                .club(club)     // 관계 설정
                .author(author) // 관계 설정
                .build();

        // 4. DB에 저장 (createdAt은 DB에서 자동 생성됨)
        return postRepository.save(newPost);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findPostById(Long postId) {
        return postRepository.findPostWithAuthorById(postId);
    }
}