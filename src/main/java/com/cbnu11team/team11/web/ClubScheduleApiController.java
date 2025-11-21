package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.CalendarService;
import com.cbnu11team.team11.web.dto.ScheduleDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/club/{clubId}")
@RequiredArgsConstructor
public class ClubScheduleApiController {

    private final CalendarService calendarService;

    // [1. 조회] 모임 일정 목록 가져오기
    @GetMapping("/schedules")
    public List<ScheduleDto> getSchedules(@PathVariable Long clubId) {
        return calendarService.getClubSchedules(clubId);
    }

    // [2. 상세] 일정 상세 정보
    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleDto> getScheduleDetail(
            @PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(calendarService.getScheduleDetail(scheduleId, userId));
    }

    // [3. 등록] 일정 생성 (로그 추가됨)
    @PostMapping("/schedule")
    public ResponseEntity<Void> createSchedule(
            @PathVariable Long clubId,
            @RequestBody ScheduleDto.Request request,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        // --- [디버깅용 로그 시작] ---
        System.out.println("=========================================");
        System.out.println(">>> [Controller] 일정 등록 요청 도착");
        System.out.println(">>> Club ID: " + clubId);
        System.out.println(">>> User ID: " + userId);
        if (request != null) {
            System.out.println(">>> Title: " + request.getTitle());
            System.out.println(">>> StartDate: " + request.getStart());
        } else {
            System.out.println(">>> Request Body is NULL!");
        }
        System.out.println("=========================================");
        // --- [디버깅용 로그 끝] ---

        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // [중요] clubId를 반드시 두 번째 인자로 넘겨야 함
        calendarService.createEvent(userId, clubId, request);

        return ResponseEntity.ok().build();
    }

    // [4. 수정]
    @PutMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleDto.Request request) {
        calendarService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok().build();
    }

    // [5. 삭제]
    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        calendarService.deleteSchedule(scheduleId, userId);
        return ResponseEntity.ok().build();
    }

    // [6. 참가 신청]
    @PostMapping("/schedule/{scheduleId}/join")
    public ResponseEntity<Void> joinSchedule(
            @PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        calendarService.joinSchedule(scheduleId, userId);
        return ResponseEntity.ok().build();
    }

    // [7. 참가 취소]
    @DeleteMapping("/schedule/{scheduleId}/join")
    public ResponseEntity<Void> leaveSchedule(
            @PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        calendarService.leaveSchedule(scheduleId, userId);
        return ResponseEntity.ok().build();
    }

    // [8. 승인/거절]
    @PutMapping("/schedule/{scheduleId}/participant/{participantId}")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long participantId,
            @RequestParam String status) {
        calendarService.changeParticipantStatus(participantId, status);
        return ResponseEntity.ok().build();
    }

    // [9. 최종 확정]
    @PostMapping("/schedule/{scheduleId}/participant/{participantId}/confirm")
    public ResponseEntity<Void> toggleConfirm(@PathVariable Long participantId) {
        calendarService.toggleConfirm(participantId);
        return ResponseEntity.ok().build();
    }
}