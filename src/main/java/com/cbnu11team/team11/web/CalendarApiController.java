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
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarApiController {

    private final CalendarService calendarService;

    // [목록 조회]
    @GetMapping("/events")
    public ResponseEntity<List<ScheduleDto>> getEvents(HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(calendarService.getMyPersonalEvents(userId));
    }

    // [추가됨] 상세 조회 (이게 있어야 클릭 시 정보를 가져옵니다!)
    @GetMapping("/events/{eventId}")
    public ResponseEntity<ScheduleDto> getEventDetail(@PathVariable Long eventId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 서비스의 getScheduleDetail 메서드 재사용
        return ResponseEntity.ok(calendarService.getScheduleDetail(eventId, userId));
    }

    // [등록]
    @PostMapping("/events")
    public ResponseEntity<Long> createEvent(@RequestBody ScheduleDto.Request request, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(calendarService.createEvent(userId, null, request));
    }

    // [삭제]
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            calendarService.deleteSchedule(eventId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // [수정] 출석/완료 토글 (예외 처리 추가)
    @PostMapping("/events/{eventId}/done")
    public ResponseEntity<?> toggleDone(@PathVariable Long eventId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            // 성공하면 true/false 반환
            boolean result = calendarService.toggleDone(eventId, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            // [핵심] 실패하면(아직 시작 안함 등) 에러 메시지를 문자열로 반환 (400 에러)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}