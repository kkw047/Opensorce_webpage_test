package com.cbnu11team.team11.web;

import com.cbnu11team.team11.repository.RegionKorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 프론트(region.js)에서 도/시군구 드롭다운 채우는 용도
 */
@RestController
@RequiredArgsConstructor
public class RegionController {

    private final RegionKorRepository regionKorRepository;

    @GetMapping("/regions/dos")
    public List<String> dos() {
        return regionKorRepository.findDistinctRegionDoOrderByRegionDoAsc();
    }

    @GetMapping("/regions/sis")
    public List<String> sis(@RequestParam("do") String regionDo) {
        if (regionDo == null || regionDo.isBlank()) return Collections.emptyList();
        return regionKorRepository.findDistinctRegionSiByRegionDoOrderByRegionSiAsc(regionDo.trim());
    }
}
