package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.*;
import com.cbnu11team.team11.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClubScheduleService {

    private final ClubScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository participantRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;

    /**
     * 1. 일정 생성
     */
    @Transactional
    public Long createSchedule(Long clubId, ScheduleCreateRequestDto requestDto, Long userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        if (!clubMemberRepository.existsByUserAndClub(creator, club)) {
            throw new IllegalStateException("이 모임에 가입된 멤버만 일정을 등록할 수 있습니다.");
        }

        ClubSchedule schedule = requestDto.toEntity(club, creator);
        scheduleRepository.save(schedule);

        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setUser(creator);
        participant.setClubSchedule(schedule);
        participant.setStatus(ParticipationStatus.ACCEPTED);
        participantRepository.save(participant);

        return schedule.getId();
    }

    /**
     * 2. 월별 일정 조회
     */
    public List<ScheduleResponseDto> getSchedulesForMonth(Long clubId, LocalDate startDate, LocalDate endDate) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임이 존재하지 않습니다."));

        List<ClubSchedule> schedules = scheduleRepository.findSchedulesForCalendar(club, startDate, endDate);

        return schedules.stream()
                .map(ScheduleResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 3. 일정 상세 조회 (로그 제거됨)
     */
    public ScheduleDetailResponseDto getScheduleDetails(Long scheduleId, Long currentUserId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        List<ScheduleParticipant> participants = participantRepository.findAllByClubSchedule(schedule);

        ScheduleParticipant myParticipation = null;
        boolean canManage = false;

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);

            if (currentUser != null) {
                myParticipation = participants.stream()
                        .filter(p -> p.getUser().getId().equals(currentUserId))
                        .findFirst().orElse(null);

                boolean isCreator = schedule.getCreator().getId().equals(currentUserId);
                // Club 엔티티 구조에 따라 getOwner() 사용
                boolean isClubAdmin = false;
                if (schedule.getClub().getOwner() != null) {
                    isClubAdmin = schedule.getClub().getOwner().getId().equals(currentUserId);
                }

                canManage = isCreator || isClubAdmin;
            }
        }

        return new ScheduleDetailResponseDto(schedule, participants, myParticipation, canManage);
    }

    /**
     * 4. 일정 참가하기
     */
    @Transactional
    public void joinSchedule(Long scheduleId, Long userId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        if (participantRepository.existsByUserAndClubSchedule(user, schedule)) {
            throw new IllegalStateException("이미 참가한 일정입니다.");
        }

        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setUser(user);
        participant.setClubSchedule(schedule);
        participant.setStatus(ParticipationStatus.PENDING);

        participantRepository.save(participant);
    }

    /**
     * 5. 일정 참가 취소하기
     */
    @Transactional
    public void leaveSchedule(Long scheduleId, Long userId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

        ScheduleParticipant participant = participantRepository.findByUserAndClubSchedule(user, schedule)
                .orElseThrow(() -> new IllegalStateException("참가하지 않은 일정입니다."));

        participantRepository.delete(participant);
    }

    /**
     * 6. 일정 삭제
     */
    @Transactional
    public void deleteSchedule(Long scheduleId, Long currentUserId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        Club club = schedule.getClub();
        boolean isCreator = schedule.getCreator().getId().equals(currentUserId);
        boolean isClubAdmin = club.getOwner().getId().equals(currentUserId);

        if (!isCreator && !isClubAdmin) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        List<ScheduleParticipant> participants = participantRepository.findAllByClubSchedule(schedule);
        participantRepository.deleteAll(participants);
        scheduleRepository.delete(schedule);
    }

    /**
     * 7. 일정 수정
     */
    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleCreateRequestDto requestDto, Long currentUserId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        Club club = schedule.getClub();
        boolean isCreator = schedule.getCreator().getId().equals(currentUserId);
        boolean isClubAdmin = club.getOwner().getId().equals(currentUserId);

        if (!isCreator && !isClubAdmin) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        schedule.setTitle(requestDto.getTitle());
        schedule.setStartDate(requestDto.getStartDate());
        schedule.setEndDate(requestDto.getEndDate());
        schedule.setFee(requestDto.getFee());
        schedule.setDetails(requestDto.getDetails());
    }

    /**
     * 8. 내 캘린더 조회
     */
    public List<ScheduleResponseDto> getMyJoinedSchedules(Long userId, LocalDate start, LocalDate end) {
        List<ClubSchedule> schedules = scheduleRepository.findJoinedSchedulesByUser(userId, start, end);
        return schedules.stream().map(ScheduleResponseDto::new).collect(Collectors.toList());
    }

    /**
     * 9. 참가자 승인 관리
     */
    @Transactional
    public void manageParticipant(Long scheduleId, Long participantId, String statusStr, Long managerId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        boolean isCreator = schedule.getCreator().getId().equals(managerId);
        boolean isClubAdmin = schedule.getClub().getOwner().getId().equals(managerId);

        if (!isCreator && !isClubAdmin) {
            throw new IllegalStateException("관리 권한이 없습니다.");
        }

        ScheduleParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참가자 정보가 없습니다."));

        try {
            ParticipationStatus newStatus = ParticipationStatus.valueOf(statusStr.toUpperCase());
            participant.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 상태값입니다.");
        }
    }

    /**
     * 10. [추가] 본인 참가 확정 체크 토글 (Toggle)
     */
    @Transactional
    public void toggleConfirmation(Long scheduleId, Long participantId, Long currentUserId) {
        ScheduleParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참가 정보가 없습니다."));

        // 1. 본인 확인
        if (!participant.getUser().getId().equals(currentUserId)) {
            throw new IllegalStateException("본인의 참가 상태만 변경할 수 있습니다.");
        }

        // 2. 승인된 상태에서만 체크 가능
        if (participant.getStatus() != ParticipationStatus.ACCEPTED) {
            throw new IllegalStateException("참가 승인이 된 상태에서만 체크할 수 있습니다.");
        }

        // 3. 상태 토글 (true <-> false)
        participant.setConfirmed(!participant.isConfirmed());
    }
}