package com.cbnu11team.team11.web;

import com.cbnu11team.team11.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionController {

    private final ClubService clubService;

    @GetMapping("/dos")
    public List<String> dos() {
        return clubService.getAllDos();
    }

    @GetMapping("/sis")
    public List<String> sis(@RequestParam("do") String regionDo) {
        return clubService.getSisByDo(regionDo);
    }
}
