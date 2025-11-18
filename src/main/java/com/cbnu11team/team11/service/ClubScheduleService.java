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

    // [1. 수정] 주석 해제: 모임 멤버 확인을 위해 필수
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

        // [2. 수정] 주석 해제: 모임 멤버가 아니면 예외 발생 (백엔드 보안)
        if (!clubMemberRepository.existsByUserAndClub(creator, club)) {
            throw new IllegalStateException("이 모임에 가입된 멤버만 일정을 등록할 수 있습니다.");
        }

        ClubSchedule schedule = requestDto.toEntity(club, creator);
        scheduleRepository.save(schedule);

        return schedule.getId();
    }

    /**
     * 2. 월별 일정 조회 (캘린더용)
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
     * 3. 일정 상세 조회 (모달용)
     * - 로그인 안 한 유저(userId == null)도 조회 가능해야 함
     */
    public ScheduleDetailResponseDto getScheduleDetails(Long scheduleId, Long currentUserId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        // 1. 참가자 목록 조회
        List<ScheduleParticipant> participants = participantRepository.findAllByClubSchedule(schedule);
        List<User> participantUsers = participants.stream()
                .map(ScheduleParticipant::getUser)
                .collect(Collectors.toList());

        // 2. 로그인 여부에 따른 상태값 설정
        boolean isParticipating = false;
        boolean canManage = false;

        if (currentUserId != null) {
            // 로그인 한 경우에만 체크
            User currentUser = userRepository.findById(currentUserId).orElse(null);

            if (currentUser != null) {
                // (1) 참가 여부 확인
                isParticipating = participantRepository.existsByUserAndClubSchedule(currentUser, schedule);

                // (2) 관리 권한 확인 (작성자 이거나, 모임의 관리자)
                boolean isCreator = schedule.getCreator().getId().equals(currentUserId);
                boolean isClubAdmin = schedule.getClub().getOwner().getId().equals(currentUserId);

                canManage = isCreator || isClubAdmin;
            }
        }

        return new ScheduleDetailResponseDto(schedule, participantUsers, isParticipating, canManage);
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

        // (선택) 여기서도 모임 멤버인지 체크하고 싶다면 위 createSchedule과 동일한 로직 추가 가능

        ScheduleParticipant participant = new ScheduleParticipant();
        participant.setUser(user);
        participant.setClubSchedule(schedule);
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
     * 6. 일정 삭제 (관리자/생성자용)
     */
    @Transactional
    public void deleteSchedule(Long scheduleId, Long currentUserId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        // 권한 확인
        boolean isCreator = schedule.getCreator().getId().equals(currentUserId);
        boolean isClubAdmin = schedule.getClub().getOwner().getId().equals(currentUserId);
        boolean canManage = isCreator || isClubAdmin;

        if (!canManage) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        // 참가자 데이터 먼저 삭제 (참조 무결성)
        List<ScheduleParticipant> participants = participantRepository.findAllByClubSchedule(schedule);
        participantRepository.deleteAll(participants);

        // 일정 삭제
        scheduleRepository.delete(schedule);
    }

    /**
     * 7. 일정 수정 (관리자/생성자용)
     */
    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleCreateRequestDto requestDto, Long currentUserId) {
        ClubSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        // 권한 확인 (삭제 로직과 동일)
        Club club = schedule.getClub();
        boolean isCreator = schedule.getCreator().getId().equals(currentUserId);
        boolean isClubAdmin = club.getOwner().getId().equals(currentUserId);

        if (!isCreator && !isClubAdmin) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        // 내용 수정 (Dirty Checking으로 자동 저장됨)
        schedule.setTitle(requestDto.getTitle());
        schedule.setStartDate(requestDto.getStartDate());
        schedule.setEndDate(requestDto.getEndDate());
        schedule.setFee(requestDto.getFee());
        schedule.setDetails(requestDto.getDetails());
    }

    /**
     * 8. [나만의 캘린더] 내가 참가한 모든 일정 조회
     */
    public List<ScheduleResponseDto> getMyJoinedSchedules(Long userId, LocalDate start, LocalDate end) {
        List<ClubSchedule> schedules = scheduleRepository.findJoinedSchedulesByUser(userId, start, end);

        return schedules.stream()
                .map(ScheduleResponseDto::new)
                .collect(Collectors.toList());
    }
}