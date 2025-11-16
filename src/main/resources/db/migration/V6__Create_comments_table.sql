CREATE TABLE comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content TEXT NOT NULL, /* 댓글 내용 (길 수 있으므로 TEXT) */

    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    post_id BIGINT NOT NULL,  /* 어느 게시글에 달렸는지 (FK) */
    user_id BIGINT NOT NULL,  /* 누가 작성했는지 (FK) */

    PRIMARY KEY (id),

    /* 외래 키 (Foreign Keys) */
    CONSTRAINT fk_comments_to_post
        FOREIGN KEY (post_id) REFERENCES posts (id)
        ON DELETE CASCADE, /* 게시글이 삭제되면 댓글도 함께 삭제 */

    CONSTRAINT fk_comments_to_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

/* 인덱스: 특정 게시글의 댓글을 빠르게 찾기 위해 */
CREATE INDEX ix_comments_post_id ON comments (post_id);
/* 인덱스: 특정 유저가 쓴 댓글을 빠르게 찾기 위해 */
CREATE INDEX ix_comments_user_id ON comments (user_id);