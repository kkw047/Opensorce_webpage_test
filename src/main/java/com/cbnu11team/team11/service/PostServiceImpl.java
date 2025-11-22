package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.Post;
import com.cbnu11team.team11.domain.PostImage;
import com.cbnu11team.team11.domain.User;

import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.PostRepository;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.web.dto.PostForm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<Post> getPostsByClubId(Long clubId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> postPage = postRepository.findPostsWithAuthorByClubId(clubId, pageable);

        postPage.forEach(post -> post.getImages().size());

        return postPage;
    }

    @Override
    @Transactional
    public Post createPost(Long clubId, PostForm postForm, Long userId) {

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NoSuchElementException("해당 클럽을 찾을 수 없습니다. ID: " + clubId));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자를 찾을 수 없습니다. ID: " + userId));

        Post post = Post.builder()
                .title(postForm.title())
                .content(postForm.content())
                .club(club)
                .author(author)
                .build();

        // 다중 이미지 저장
        if (postForm.imageFiles() != null && !postForm.imageFiles().isEmpty()) {
            for (MultipartFile file : postForm.imageFiles()) {
                if (file.isEmpty()) continue;

                String url = fileStorageService.save(file);
                PostImage postImage = new PostImage(url, file.getOriginalFilename(), post);
                post.getImages().add(postImage);
            }
        }

        return postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findPostById(Long postId) {
        return postRepository.findPostWithAuthorById(postId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findPostWithAuthorById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다. ID: " + postId));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new SecurityException("게시물을 삭제할 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void updatePost(Long postId, PostForm postForm, Long currentUserId) {
        Post post = postRepository.findPostWithAuthorById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다. ID: " + postId));

        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new SecurityException("게시물을 수정할 권한이 없습니다.");
        }

        post.setTitle(postForm.title());
        post.setContent(postForm.content());

        // 이미지 삭제 로직
        if (postForm.deleteImageIds() != null && !postForm.deleteImageIds().isEmpty()) {
            post.getImages().removeIf(img -> postForm.deleteImageIds().contains(img.getId()));
        }

        // 새 이미지 추가 로직
        if (postForm.imageFiles() != null && !postForm.imageFiles().isEmpty()) {
            for (MultipartFile file : postForm.imageFiles()) {
                if (file.isEmpty()) continue;

                String url = fileStorageService.save(file);
                PostImage postImage = new PostImage(url, file.getOriginalFilename(), post);
                post.getImages().add(postImage);
            }
        }

        postRepository.save(post);
    }
}