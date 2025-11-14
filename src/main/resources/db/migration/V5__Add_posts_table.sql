-- posts 테이블 생성
CREATE TABLE posts (
    -- 1. 기본 키 (ID)
    id BIGINT NOT NULL AUTO_INCREMENT,

    -- 2. 필드 (Post.java 엔티티와 매핑)
    title VARCHAR(100) NOT NULL,
    content LONGTEXT NOT NULL,

    -- createdAt 필드 (엔티티에서 insertable=false, updatable=false 였으므로 DB 기본값 설정)
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    -- 3. 외래 키 (User, Club과 연결)
    user_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL,

    -- 4. 제약 조건
    PRIMARY KEY (id),

    -- 5. 인덱스 (Post.java에 정의한 @Index)
    INDEX ix_posts_user_id (user_id),
    INDEX ix_posts_club_id (club_id),

    -- 6. 외래 키 관계 정의 (users 테이블, clubs 테이블 참조)
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;