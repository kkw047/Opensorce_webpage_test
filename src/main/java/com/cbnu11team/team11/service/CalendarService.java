package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.*;
import com.cbnu11team.team11.web.dto.ScheduleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final CalendarParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;

    /**
     * [조회] 나의 캘린더용 (내가 생성했거나, 참여 중인 모든 일정 통합 조회)
     * - 수정됨: 삭제된 findAllByUserIdAndClubIsNull 대신 findAllRelatedToUser 사용
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto> getMyPersonalEvents(Long userId) {
        return calendarRepository.findAllRelatedToUser(userId).stream()
                .map(ScheduleDto::new)
                .collect(Collectors.toList());
    }

    /**
     * [조회] 모임 일정 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto> getClubSchedules(Long clubId) {
        return calendarRepository.findAllByClubId(clubId).stream()
                .map(ScheduleDto::new)
                .collect(Collectors.toList());
    }

    /**
     * [상세] 일정 상세 조회
     */
    @Transactional(readOnly = true)
    public ScheduleDto getScheduleDetail(Long scheduleId, Long currentUserId) {
        Calendar cal = calendarRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        ScheduleDto dto = new ScheduleDto(cal);
        dto.setDetails(cal.getDescription());
        dto.setFee(cal.getFee());

        // 권한 체크: 작성자거나 모임장이면 관리자
        boolean isWriter = cal.getUser().getId().equals(currentUserId);
        boolean isClubOwner = false;
        if (cal.getClub() != null && cal.getClub().getOwner() != null) {
            if (cal.getClub().getOwner().getId().equals(currentUserId)) {
                isClubOwner = true;
            }
        }
        dto.setManager(isWriter || isClubOwner);

        // 참가자 리스트
        List<ScheduleDto.ParticipantDto> pList = cal.getParticipants().stream()
                .map(ScheduleDto.ParticipantDto::new)
                .collect(Collectors.toList());
        dto.setParticipants(pList);

        // 내 상태 확인
        participantRepository.findByCalendarIdAndUserId(scheduleId, currentUserId)
                .ifPresentOrElse(
                        p -> {
                            dto.setParticipating(true);
                            dto.setMyStatus(p.getStatus().name());
                        },
                        () -> dto.setParticipating(false)
                );

        return dto;
    }

    /**
     * [생성] 일정 등록
     * - 모임 일정인 경우 작성자를 자동으로 참가자로 등록
     */
    @Transactional
    public Long createEvent(Long userId, Long clubId, ScheduleDto.Request req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        LocalDateTime start = LocalDate.parse(req.getStart()).atStartOfDay();
        LocalDateTime end = LocalDate.parse(req.getEnd()).atStartOfDay();

        Calendar.CalendarBuilder builder = Calendar.builder()
                .user(user)
                .title(req.getTitle())
                .description(req.getDetails())
                .startDate(start)
                .endDate(end)
                .fee(req.getFee());

        if (clubId != null) {
            Club club = clubRepository.findById(clubId)
                    .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));
            builder.club(club);
        }

        // 1. 일정 저장
        Calendar savedCalendar = calendarRepository.save(builder.build());

        // 2. [작성자 자동 참가] 모임 일정인 경우, 작성자를 '승인된 참가자'로 바로 등록
        if (clubId != null) {
            CalendarParticipant maker = CalendarParticipant.builder()
                    .calendar(savedCalendar)
                    .user(user)
                    .status(ParticipantStatus.ACCEPTED)
                    .isConfirmed(true)
                    .build();
            participantRepository.save(maker);
        }

        return savedCalendar.getId();
    }

    /**
     * [수정] 일정 수정
     */
    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleDto.Request req) {
        Calendar cal = calendarRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        LocalDateTime start = LocalDate.parse(req.getStart()).atStartOfDay();
        LocalDateTime end = LocalDate.parse(req.getEnd()).atStartOfDay();

        cal.update(req.getTitle(), req.getDetails(), start, end, req.getFee());
    }

    /**
     * [삭제] 일정 삭제
     */
    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        Calendar cal = calendarRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 없습니다."));

        boolean isWriter = cal.getUser().getId().equals(userId);
        boolean isClubOwner = false;
        if (cal.getClub() != null && cal.getClub().getOwner() != null) {
            if (cal.getClub().getOwner().getId().equals(userId)) {
                isClubOwner = true;
            }
        }

        if (!isWriter && !isClubOwner) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        calendarRepository.delete(cal);
    }

    // ========================================================
    //  참가 관리
    // ========================================================

    @Transactional
    public void joinSchedule(Long scheduleId, Long userId) {
        if (participantRepository.findByCalendarIdAndUserId(scheduleId, userId).isPresent()) {
            throw new IllegalStateException("이미 신청한 일정입니다.");
        }

        Calendar calendar = calendarRepository.findById(scheduleId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        CalendarParticipant participant = CalendarParticipant.builder()
                .calendar(calendar)
                .user(user)
                .status(ParticipantStatus.PENDING)
                .isConfirmed(false)
                .build();

        participantRepository.save(participant);
    }

    @Transactional
    public void leaveSchedule(Long scheduleId, Long userId) {
        CalendarParticipant p = participantRepository.findByCalendarIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("참가 정보를 찾을 수 없습니다."));
        participantRepository.delete(p);
    }

    @Transactional
    public void changeParticipantStatus(Long participantId, String statusStr) {
        CalendarParticipant p = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참가자가 존재하지 않습니다."));
        p.setStatus(ParticipantStatus.valueOf(statusStr));
    }

    @Transactional
    public void toggleConfirm(Long participantId) {
        CalendarParticipant p = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참가자가 존재하지 않습니다."));
        p.setConfirmed(!p.isConfirmed());
    }
}