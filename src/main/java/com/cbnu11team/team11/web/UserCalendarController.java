package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.service.ClubScheduleService;
import com.cbnu11team.team11.service.ClubService;
import com.cbnu11team.team11.web.dto.ScheduleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserCalendarController {

    private final ClubScheduleService scheduleService;
    private final UserRepository userRepository;
    private final ClubService clubService;

    // 1. 화면 보여주기
    @GetMapping("/my-calendar")
    public String myCalendarPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // [수정] 로그인 안 했어도 쫓아내지 않음 (if userDetails == null check 삭제)

        if (userDetails != null) {
            userRepository.findByLoginId(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("currentUser", user.getId());
            });
        }

        // 공통 레이아웃 데이터
        model.addAttribute("dos", clubService.getAllDos());
        model.addAttribute("categories", clubService.findAllCategories());
        model.addAttribute("activeSidebarMenu", "calendar");
        model.addAttribute("searchActionUrl", "/clubs");
        model.addAttribute("selectedDo", null);
        model.addAttribute("selectedSi", null);
        model.addAttribute("selectedCategoryIds", List.of());
        model.addAttribute("q", null);

        return "users/my_calendar";
    }

    // 2. 데이터 API
    @GetMapping("/api/my/schedules")
    @ResponseBody
    public ResponseEntity<List<ScheduleResponseDto>> getMySchedules(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end
    ) {
        // [수정] 로그인 안 했으면 에러(401) 대신 빈 리스트([]) 반환 -> 캘린더가 안 깨짐
        if (userDetails == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        User user = userRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        List<ScheduleResponseDto> schedules = scheduleService.getMyJoinedSchedules(
                user.getId(),
                start.toLocalDate(),
                end.toLocalDate()
        );

        return ResponseEntity.ok(schedules);
    }
}