package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.service.ClubScheduleService;
import com.cbnu11team.team11.web.dto.*;
import jakarta.servlet.http.HttpSession; // [추가] 세션 사용을 위해 필수
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
    private final HttpSession httpSession; // [추가] 세션 주입

    // [헬퍼 1] 로그인한 유저 ID를 가져오거나, 없으면 null 반환 (세션 -> UserDetails -> Context 순서로 확인)
    private Long getUserIdOrNull(UserDetails userDetails) {
        // 1. 세션값 확인 (가장 확실한 방법)
        // CustomLoginSuccessHandler에서 넣어준 값을 우선적으로 사용합니다.
        Long sessionId = (Long) httpSession.getAttribute("LOGIN_USER_ID");
        if (sessionId != null) {
            return sessionId;
        }

        // 2. 인자로 받은 userDetails 확인
        if (userDetails != null) {
            return userRepository.findByLoginId(userDetails.getUsername())
                    .map(User::getId).orElse(null);
        }

        // 3. (비상용) SecurityContextHolder에서 직접 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            return userRepository.findByLoginId(principal.getUsername())
                    .map(User::getId).orElse(null);
        }

        return null;
    }

    // [헬퍼 2] 유저 ID 가져오기 (로그인 필수 - 없으면 에러 발생)
    private Long getUserIdRequired(UserDetails userDetails) {
        Long id = getUserIdOrNull(userDetails);
        if (id == null) throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        return id;
    }

    /**
     * 1. 일정 생성 (로그인 필수)
     */
    @PostMapping("/schedule")
    public ResponseEntity<Long> createSchedule(
            @PathVariable Long clubId,
            @RequestBody ScheduleCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserIdRequired(userDetails); // 로그인 검증
        Long scheduleId = scheduleService.createSchedule(clubId, requestDto, userId);
        return ResponseEntity.ok(scheduleId);
    }

    /**
     * 2. 월별 일정 조회 (누구나 가능)
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponseDto>> getSchedules(
            @PathVariable Long clubId,
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end
    ) {
        // 조회는 로그인 여부 상관없이 가능
        List<ScheduleResponseDto> schedules = scheduleService.getSchedulesForMonth(clubId, start.toLocalDate(), end.toLocalDate());
        return ResponseEntity.ok(schedules);
    }

    /**
     * 3. 일정 상세 조회 (누구나 가능하지만, 버튼 표시 여부는 로그인 정보에 따라 다름)
     */
    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponseDto> getScheduleDetail(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 로그인 안 했으면 userId는 null로 전달됨 (Service에서 버튼 숨김 처리)
        Long userId = getUserIdOrNull(userDetails);
        ScheduleDetailResponseDto dto = scheduleService.getScheduleDetails(scheduleId, userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * 4. 일정 참가 (로그인 필수)
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
     * 5. 참가 취소 (로그인 필수)
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
     * 6. 일정 삭제 (로그인 필수)
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
     * 7. 일정 수정 (PUT) - [추가된 메소드]
     */
    @PutMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestBody ScheduleCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // [수정] 메소드 이름을 통일했습니다 (getCurrentUserId -> getUserIdRequired)
        Long userId = getUserIdRequired(userDetails);
        scheduleService.updateSchedule(scheduleId, requestDto, userId);
        return ResponseEntity.ok().build();
    }
}