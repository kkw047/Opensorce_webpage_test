package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionApiController {

    private final ClubService clubService;

    @GetMapping("/do")
    public List<String> getDos() {
        return clubService.getAllDos();
    }

    @GetMapping("/si")
    public List<String> getSis(@RequestParam("rdo") String rdo) {
        return clubService.getSisByDo(rdo);
    }
}
