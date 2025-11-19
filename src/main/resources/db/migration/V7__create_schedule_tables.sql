-- V7: 일정 관련 테이블 완전 초기화 및 생성 (최종 스펙 반영)

-- 1. 기존 테이블 삭제 (순서 중요: 참가자 -> 일정)
DROP TABLE IF EXISTS schedule_participant;
DROP TABLE IF EXISTS club_schedule;

-- 2. 모임 일정(ClubSchedule) 테이블 생성
CREATE TABLE club_schedule (
                               club_schedule_id BIGINT NOT NULL AUTO_INCREMENT,
                               title VARCHAR(255),
                               details VARCHAR(1000),
                               start_date DATE,
                               end_date DATE,
                               fee VARCHAR(255),
                               club_id BIGINT,
                               creator_id BIGINT,
                               PRIMARY KEY (club_schedule_id)
);

-- 3. 일정 참가자(ScheduleParticipant) 테이블 생성
-- (status와 is_confirmed 컬럼을 여기에 모두 포함시켰습니다)
CREATE TABLE schedule_participant (
                                      schedule_participant_id BIGINT NOT NULL AUTO_INCREMENT,
                                      user_id BIGINT,
                                      club_schedule_id BIGINT,

    -- [통합된 컬럼들]
                                      status VARCHAR(20) DEFAULT 'PENDING',       -- 참가 상태 (대기/승인/거절)
                                      is_confirmed BOOLEAN DEFAULT FALSE,         -- 본인 참가 확정 여부

                                      PRIMARY KEY (schedule_participant_id)
);

-- 4. 외래 키(Foreign Key) 설정
ALTER TABLE club_schedule
    ADD CONSTRAINT FK_SCHEDULE_ON_CLUB
        FOREIGN KEY (club_id) REFERENCES clubs (id);

ALTER TABLE club_schedule
    ADD CONSTRAINT FK_SCHEDULE_ON_CREATOR
        FOREIGN KEY (creator_id) REFERENCES users (id);

ALTER TABLE schedule_participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_USER
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE schedule_participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_SCHEDULE
        FOREIGN KEY (club_schedule_id) REFERENCES club_schedule (club_schedule_id);