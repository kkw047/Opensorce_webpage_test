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
     * [헬퍼 메서드] 엔티티 -> DTO 변환 시 출석/완료 여부(isDone)를 자동 계산
     */
    private ScheduleDto convertToDtoWithStatus(Calendar cal, Long userId) {
        ScheduleDto dto = new ScheduleDto(cal);

        if (cal.getClub() == null) {
            // 개인 일정: 작성자의 isDone 필드 사용
            dto.setDone(cal.isDone());
        } else {
            // 모임 일정: 나의 참가 정보(CalendarParticipant)의 isAttended 확인
            boolean attended = participantRepository.findByCalendarIdAndUserId(cal.getId(), userId)
                    .map(CalendarParticipant::isAttended)
                    .orElse(false);
            dto.setDone(attended);
        }
        return dto;
    }

    /**
     * [조회] 나의 캘린더용 (수정됨: 상태값 포함)
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto> getMyPersonalEvents(Long userId) {
        return calendarRepository.findAllRelatedToUser(userId).stream()
                .map(cal -> convertToDtoWithStatus(cal, userId)) // 헬퍼 메서드 사용
                .collect(Collectors.toList());
    }

    /**
     * [조회] 모임 일정 목록 조회 (수정됨: 상태값 포함)
     */
    @Transactional(readOnly = true)
    public List<ScheduleDto> getClubSchedules(Long clubId, Long userId) {
        return calendarRepository.findAllByClubId(clubId).stream()
                .map(cal -> convertToDtoWithStatus(cal, userId)) // 헬퍼 메서드 사용
                .collect(Collectors.toList());
    }

    /**
     * [상세] 일정 상세 조회
     */
    @Transactional(readOnly = true)
    public ScheduleDto getScheduleDetail(Long scheduleId, Long currentUserId) {
        Calendar cal = calendarRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        // 상세 조회에서도 헬퍼 메서드 로직을 활용하거나, 기존처럼 수동 세팅
        ScheduleDto dto = convertToDtoWithStatus(cal, currentUserId);
        dto.setDetails(cal.getDescription()); // 상세는 목록에 없을 수 있으니 추가 세팅
        dto.setFee(cal.getFee());

        // 권한 체크
        boolean isWriter = cal.getUser().getId().equals(currentUserId);
        boolean isClubOwner = false;
        if (cal.getClub() != null && cal.getClub().getOwner() != null) {
            if (cal.getClub().getOwner().getId().equals(currentUserId)) isClubOwner = true;
        }
        dto.setManager(isWriter || isClubOwner);

        // 참가자 리스트
        List<ScheduleDto.ParticipantDto> pList = cal.getParticipants().stream()
                .map(ScheduleDto.ParticipantDto::new)
                .collect(Collectors.toList());
        dto.setParticipants(pList);

        // 내 참가 상태 (ACCEPTED 등) 확인
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

    // --- [아래 createEvent, update, delete 등 기존 로직은 그대로 유지] ---

    @Transactional
    public Long createEvent(Long userId, Long clubId, ScheduleDto.Request req) {
        User user = userRepository.findById(userId).orElseThrow();
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
            Club club = clubRepository.findById(clubId).orElseThrow();
            builder.club(club);
        }

        Calendar savedCalendar = calendarRepository.save(builder.build());

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
        if (!isWriter && !isClubOwner) throw new IllegalStateException("권한 없음");
        calendarRepository.delete(cal);
    }

    @Transactional
    public boolean toggleDone(Long scheduleId, Long userId) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();
        if (cal.getClub() == null) {
            if (!cal.getUser().getId().equals(userId)) throw new IllegalStateException("권한 없음");
            cal.toggleDone();
            return cal.isDone();
        } else {
            CalendarParticipant p = participantRepository.findByCalendarIdAndUserId(scheduleId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("참가자 아님"));
            p.toggleAttended();
            return p.isAttended();
        }
    }

    @Transactional
    public void joinSchedule(Long scheduleId, Long userId) {
        Calendar cal = calendarRepository.findById(scheduleId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        if(participantRepository.findByCalendarIdAndUserId(scheduleId, userId).isPresent()) throw new IllegalStateException("이미 신청함");
        participantRepository.save(CalendarParticipant.builder().calendar(cal).user(user).status(ParticipantStatus.PENDING).isConfirmed(false).build());
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