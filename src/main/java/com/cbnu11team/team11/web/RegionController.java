package com.cbnu11team.team11.web;

import com.cbnu11team.team11.repository.RegionKorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionKorRepository regionKorRepository;

    // /api/regions/dos -> ["강원특별자치도", "경기도", ...]
    @GetMapping("/dos")
    public List<String> dos() {
        return regionKorRepository.findAllDistinctDo();
    }

    // /api/regions/sis?do=경기도 -> ["수원시", "용인시", ...]
    @GetMapping("/sis")
    public List<String> sis(@RequestParam("do") String regionDo) {
        return regionKorRepository.findSisByDo(regionDo);
    }
}
