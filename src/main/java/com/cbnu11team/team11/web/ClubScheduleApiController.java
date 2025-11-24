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

    // [수정됨] 세션에서 userId를 꺼내서 서비스로 전달
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleDto>> getSchedules(@PathVariable Long clubId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        // 비로그인 상태면 빈 목록이나 에러를 줄 수 있지만, 여기선 일단 401
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 서비스에 userId도 같이 넘김
        return ResponseEntity.ok(calendarService.getClubSchedules(clubId, userId));
    }

    // ... (나머지 메서드들은 기존과 동일) ...

    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleDto> getScheduleDetail(@PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(calendarService.getScheduleDetail(scheduleId, userId));
    }

    @PostMapping("/schedule")
    public ResponseEntity<Void> createSchedule(@PathVariable Long clubId, @RequestBody ScheduleDto.Request request, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        calendarService.createEvent(userId, clubId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> updateSchedule(@PathVariable Long scheduleId, @RequestBody ScheduleDto.Request request) {
        calendarService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        calendarService.deleteSchedule(scheduleId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/schedule/{scheduleId}/join")
    public ResponseEntity<Void> joinSchedule(@PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        calendarService.joinSchedule(scheduleId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/schedule/{scheduleId}/join")
    public ResponseEntity<Void> leaveSchedule(@PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        calendarService.leaveSchedule(scheduleId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/schedule/{scheduleId}/participant/{participantId}")
    public ResponseEntity<Void> changeStatus(@PathVariable Long participantId, @RequestParam String status) {
        calendarService.changeParticipantStatus(participantId, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/schedule/{scheduleId}/done")
    // (참고: 만약 CalendarApiController를 쓰고 계시다면 그 파일의 toggleDone을 수정하세요)
    public ResponseEntity<?> toggleDone(@PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            // 성공 시: 변경된 상태(Boolean) 반환
            boolean result = calendarService.toggleDone(scheduleId, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            // [핵심] 실패 시: 서비스에서 던진 에러 메시지("아직 출석 체크가...")를 그대로 반환
            // 400 Bad Request 상태코드 사용
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // [추가] 출석 활성화 토글 (관리자용)
    @PostMapping("/schedule/{scheduleId}/attendance-active")
    public ResponseEntity<Boolean> toggleAttendanceActive(@PathVariable Long scheduleId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(calendarService.toggleAttendanceActive(scheduleId, userId));
    }
}