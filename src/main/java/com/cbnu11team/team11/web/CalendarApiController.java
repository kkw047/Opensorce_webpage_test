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

    @PostMapping("/events/{eventId}/done") // 또는 "/schedule/{scheduleId}/done" (컨트롤러에 맞춰서)
    public ResponseEntity<Boolean> toggleDone(@PathVariable Long eventId, HttpSession session) {
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean result = calendarService.toggleDone(eventId, userId);
        return ResponseEntity.ok(result);
    }
}