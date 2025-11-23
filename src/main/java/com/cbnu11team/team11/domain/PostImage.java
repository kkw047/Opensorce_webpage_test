package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_images")
@Getter @Setter
@NoArgsConstructor
public class PostImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    private String originalFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public PostImage(String imageUrl, String originalFileName, Post post) {
        this.imageUrl = imageUrl;
        this.originalFileName = originalFileName;
        this.post = post;
    }
}