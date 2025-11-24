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

    // [헬퍼] DTO 변환
    private ScheduleDto convertToDtoWithStatus(Calendar cal, Long userId) {
        ScheduleDto dto = new ScheduleDto(cal);

        if (cal.getClub() == null) {
            dto.setDone(cal.isDone());
        } else {
            boolean attended = participantRepository.findByCalendarIdAndUserId(cal.getId(), userId)
                    .map(CalendarParticipant::isAttended)
                    .orElse(false);
            dto.setDone(attended);
        }
        return dto;
    }

    /**
     * [조회] 나의 캘린더 (필터링 적용: 미승인 일정 제외)
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto> getMyPersonalEvents(Long userId) {
        return calendarRepository.findAllRelatedToUser(userId).stream()
                .filter(cal -> {
                    // 1. 개인 일정은 무조건 표시
                    if (cal.getClub() == null) return true;

                    // 2. 모임 일정은 '승인(ACCEPTED)'된 경우만 표시
                    // (참고: 일정 생성자는 생성 시 자동으로 ACCEPTED가 되므로 여기서 걸러지지 않음)
                    return participantRepository.findByCalendarIdAndUserId(cal.getId(), userId)
                            .map(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                            .orElse(false);
                })
                .map(cal -> convertToDtoWithStatus(cal, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> getClubSchedules(Long clubId, Long userId) {
        return calendarRepository.findAllByClubId(clubId).stream()
                .map(cal -> convertToDtoWithStatus(cal, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScheduleDto getScheduleDetail(Long scheduleId, Long currentUserId) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();
        ScheduleDto dto = convertToDtoWithStatus(cal, currentUserId);
        dto.setDetails(cal.getDescription());
        dto.setFee(cal.getFee());

        boolean isWriter = cal.getUser().getId().equals(currentUserId);
        boolean isClubOwner = (cal.getClub() != null && cal.getClub().getOwner().getId().equals(currentUserId));
        dto.setManager(isWriter || isClubOwner);

        List<ScheduleDto.ParticipantDto> pList = cal.getParticipants().stream()
                .map(ScheduleDto.ParticipantDto::new)
                .collect(Collectors.toList());
        dto.setParticipants(pList);

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

    @Transactional
    public Long createEvent(Long userId, Long clubId, ScheduleDto.Request req) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime start = LocalDate.parse(req.getStart()).atStartOfDay();
        LocalDateTime end = LocalDate.parse(req.getEnd()).atStartOfDay();

        Calendar.CalendarBuilder builder = Calendar.builder()
                .user(user).title(req.getTitle()).description(req.getDetails())
                .startDate(start).endDate(end).fee(req.getFee());

        if (clubId != null) {
            Club club = clubRepository.findById(clubId).orElseThrow();
            builder.club(club);
        }
        Calendar saved = calendarRepository.save(builder.build());

        if (clubId != null) {
            participantRepository.save(CalendarParticipant.builder()
                    .calendar(saved).user(user)
                    .status(ParticipantStatus.ACCEPTED).isConfirmed(true).build());
        }
        return saved.getId();
    }

    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleDto.Request req) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();
        LocalDateTime start = LocalDate.parse(req.getStart()).atStartOfDay();
        LocalDateTime end = LocalDate.parse(req.getEnd()).atStartOfDay();
        cal.update(req.getTitle(), req.getDetails(), start, end, req.getFee());
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();
        boolean isWriter = cal.getUser().getId().equals(userId);
        boolean isClubOwner = (cal.getClub() != null && cal.getClub().getOwner().getId().equals(userId));
        if (!isWriter && !isClubOwner) throw new IllegalStateException("권한이 없습니다.");
        calendarRepository.delete(cal);
    }

    @Transactional
    public boolean toggleDone(Long scheduleId, Long userId) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();

        if (cal.getClub() == null) {
            if (!cal.getUser().getId().equals(userId)) throw new IllegalStateException("권한이 없습니다.");
            cal.toggleDone();
            return cal.isDone();
        } else {
            if (!cal.isAttendanceActive()) {
                throw new IllegalStateException("아직 출석 체크가 시작되지 않았습니다.");
            }
            CalendarParticipant p = participantRepository.findByCalendarIdAndUserId(scheduleId, userId).orElseThrow();
            p.toggleAttended();
            return p.isAttended();
        }
    }

    @Transactional
    public boolean toggleAttendanceActive(Long scheduleId, Long userId) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();
        boolean isWriter = cal.getUser().getId().equals(userId);
        boolean isClubOwner = (cal.getClub() != null && cal.getClub().getOwner().getId().equals(userId));
        if (!isWriter && !isClubOwner) throw new IllegalStateException("권한이 없습니다.");

        cal.toggleAttendanceActive();
        return cal.isAttendanceActive();
    }

    @Transactional
    public void joinSchedule(Long scheduleId, Long userId) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        // 재신청 로직: 거절된 상태면 다시 PENDING으로, 아니면 신규 생성
        participantRepository.findByCalendarIdAndUserId(scheduleId, userId).ifPresentOrElse(
                p -> {
                    if (p.getStatus() == ParticipantStatus.REJECTED) {
                        p.setStatus(ParticipantStatus.PENDING);
                    } else {
                        throw new IllegalStateException("이미 신청한 일정입니다.");
                    }
                },
                () -> {
                    participantRepository.save(CalendarParticipant.builder()
                            .calendar(cal).user(user).status(ParticipantStatus.PENDING).isConfirmed(false).build());
                }
        );
    }

    @Transactional
    public void leaveSchedule(Long scheduleId, Long userId) {
        CalendarParticipant p = participantRepository.findByCalendarIdAndUserId(scheduleId, userId).orElseThrow();
        participantRepository.delete(p);
    }

    @Transactional
    public void changeParticipantStatus(Long participantId, String statusStr) {
        CalendarParticipant p = participantRepository.findById(participantId).orElseThrow();
        p.setStatus(ParticipantStatus.valueOf(statusStr));
    }

    @Transactional
    public void toggleConfirm(Long participantId) {
        CalendarParticipant p = participantRepository.findById(participantId).orElseThrow();
        p.setConfirmed(!p.isConfirmed());
    }
}