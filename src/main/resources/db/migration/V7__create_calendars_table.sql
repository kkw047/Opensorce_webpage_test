CREATE TABLE IF NOT EXISTS calendars (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    start_date DATETIME(6) NOT NULL,
    end_date DATETIME(6) NOT NULL,
    fee VARCHAR(255),

    is_done BOOLEAN DEFAULT FALSE,              -- 개인 일정 완료 여부
    is_attendance_active BOOLEAN DEFAULT FALSE, -- [추가됨] 출석 체크 활성화 여부

    user_id BIGINT NOT NULL,
    club_id BIGINT,

    CONSTRAINT fk_calendars_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_calendars_club FOREIGN KEY (club_id) REFERENCES clubs (id) ON DELETE CASCADE
    );

-- 3. 참가자 테이블 생성
CREATE TABLE IF NOT EXISTS calendar_participants (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     calendar_id BIGINT NOT NULL,
                                                     user_id BIGINT NOT NULL,
                                                     status VARCHAR(20) NOT NULL,
    is_confirmed BOOLEAN DEFAULT FALSE,
    is_attended BOOLEAN DEFAULT FALSE,          -- 참가자 개별 출석 여부
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_cp_calendar FOREIGN KEY (calendar_id) REFERENCES calendars (id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    );