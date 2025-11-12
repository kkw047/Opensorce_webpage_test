package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionApiController {

    private final ClubService clubService;

    @GetMapping("/dos")
    public List<String> getDos() {
        return clubService.getAllDos();
    }

    // ?do= 또는 ?regionDo= 또는 ?rdo= 어느 쿼리파라미터로 와도 동작
    @GetMapping("/si")
    public List<String> getSis(
            @RequestParam(name = "do", required = false) String regionDo,
            @RequestParam(name = "regionDo", required = false) String regionDo2,
            @RequestParam(name = "rdo", required = false) String regionDo3
    ) {
        String rdo = firstNonBlank(regionDo, regionDo2, regionDo3);
        if (!StringUtils.hasText(rdo)) return List.of();
        return clubService.getSisByDo(rdo.trim());
    }

    private String firstNonBlank(String... xs) {
        if (xs == null) return null;
        for (String s : xs) if (StringUtils.hasText(s)) return s;
        return null;
    }
}
