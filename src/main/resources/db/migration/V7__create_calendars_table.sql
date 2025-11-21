-- 1. 일정 테이블 (참여비 fee 추가)
CREATE TABLE calendars (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           title VARCHAR(255) NOT NULL,
                           description VARCHAR(500),
                           start_date DATETIME(6) NOT NULL,
                           end_date DATETIME(6) NOT NULL,
                           fee VARCHAR(255),               -- 참여비 (예: "10,000원")
                           user_id BIGINT NOT NULL,        -- 생성자(User)
                           club_id BIGINT,                 -- 모임 ID (개인 일정이면 NULL)

                           CONSTRAINT fk_calendars_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                           CONSTRAINT fk_calendars_club FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE
);

-- 2. 일정 참가자 테이블 (신규 추가)
CREATE TABLE calendar_participants (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       calendar_id BIGINT NOT NULL,
                                       user_id BIGINT NOT NULL,
                                       status VARCHAR(20) NOT NULL,    -- PENDING(대기), ACCEPTED(승인), REJECTED(거절)
                                       is_confirmed BOOLEAN DEFAULT FALSE, -- 최종 확정 여부 (JS의 toggleConfirm 대응)
                                       created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),

                                       CONSTRAINT fk_cp_calendar FOREIGN KEY (calendar_id) REFERENCES calendars (id) ON DELETE CASCADE,
                                       CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);