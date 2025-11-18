package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.service.ClubScheduleService;
import com.cbnu11team.team11.web.dto.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/club/{clubId}")
public class ClubScheduleApiController {

    private final ClubScheduleService scheduleService;
    private final UserRepository userRepository;
    private final HttpSession httpSession;

    // [헬퍼 1] 로그인한 유저 ID를 가져오거나, 없으면 null 반환
    private Long getUserIdOrNull(UserDetails userDetails) {
        // 1. 세션값 확인 (가장 확실)
        Long sessionId = (Long) httpSession.getAttribute("LOGIN_USER_ID");
        if (sessionId != null) {
            return sessionId;
        }

        // 2. UserDetails 확인
        if (userDetails != null) {
            return userRepository.findByLoginId(userDetails.getUsername())
                    .map(User::getId).orElse(null);
        }

        // 3. SecurityContext 확인 (비상용)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            return userRepository.findByLoginId(principal.getUsername())
                    .map(User::getId).orElse(null);
        }

        return null;
    }

    // [헬퍼 2] 유저 ID 가져오기 (로그인 필수)
    private Long getUserIdRequired(UserDetails userDetails) {
        Long id = getUserIdOrNull(userDetails);
        if (id == null) throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        return id;
    }

    /**
     * 1. 일정 생성
     */
    @PostMapping("/schedule")
    public ResponseEntity<Long> createSchedule(
            @PathVariable Long clubId,
            @RequestBody ScheduleCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserIdRequired(userDetails);
        Long scheduleId = scheduleService.createSchedule(clubId, requestDto, userId);
        return ResponseEntity.ok(scheduleId);
    }

    /**
     * 2. 월별 일정 조회
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponseDto>> getSchedules(
            @PathVariable Long clubId,
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end
    ) {
        List<ScheduleResponseDto> schedules = scheduleService.getSchedulesForMonth(clubId, start.toLocalDate(), end.toLocalDate());
        return ResponseEntity.ok(schedules);
    }

    /**
     * 3. 일정 상세 조회
     */
    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponseDto> getScheduleDetail(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserIdOrNull(userDetails);
        ScheduleDetailResponseDto dto = scheduleService.getScheduleDetails(scheduleId, userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * 4. 일정 참가
     */
    @PostMapping("/schedule/{scheduleId}/join")
    public ResponseEntity<Void> joinSchedule(
            @PathVariable Long clubId, @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserIdRequired(userDetails);
        scheduleService.joinSchedule(scheduleId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 5. 참가 취소
     */
    @DeleteMapping("/schedule/{scheduleId}/join")
    public ResponseEntity<Void> leaveSchedule(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        scheduleService.leaveSchedule(scheduleId, getUserIdRequired(userDetails));
        return ResponseEntity.ok().build();
    }

    /**
     * 6. 일정 삭제
     */
    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        scheduleService.deleteSchedule(scheduleId, getUserIdRequired(userDetails));
        return ResponseEntity.ok().build();
    }

    /**
     * 7. 일정 수정
     */
    @PutMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestBody ScheduleCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserIdRequired(userDetails);
        scheduleService.updateSchedule(scheduleId, requestDto, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * [추가됨] 8. 참가자 승인/거절 상태 변경 (관리자용)
     * 이 메소드가 없어서 오류가 발생했습니다. 이제 추가되었습니다!
     */
    @PutMapping("/schedule/{scheduleId}/participant/{participantId}")
    public ResponseEntity<Void> updateParticipantStatus(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @PathVariable Long participantId,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long managerId = getUserIdRequired(userDetails);
        scheduleService.manageParticipant(scheduleId, participantId, status, managerId);
        return ResponseEntity.ok().build();
    }
}