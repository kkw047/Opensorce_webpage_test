-- V6: 삭제된 일정 테이블 다시 생성 (복구용)

-- 혹시라도 찌꺼기가 남아있을까봐 안전하게 삭제 후 생성
DROP TABLE IF EXISTS schedule_participant;
DROP TABLE IF EXISTS club_schedule;

-- 1. 모임 일정(ClubSchedule) 테이블 생성
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

-- 2. 일정 참가자(ScheduleParticipant) 테이블 생성
CREATE TABLE schedule_participant (
                                      schedule_participant_id BIGINT NOT NULL AUTO_INCREMENT,
                                      user_id BIGINT,
                                      club_schedule_id BIGINT,
                                      PRIMARY KEY (schedule_participant_id)
);

-- 3. 외래 키(Foreign Key) 설정
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