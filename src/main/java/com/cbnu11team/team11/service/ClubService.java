package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;

    public Page<Club> list(Pageable pageable) {
        return clubRepository.findAllByOrderByIdDesc(pageable);
    }

    public Page<Club> search(String keyword,
                             String regionDo,
                             String regionSi,
                             List<Long> categoryIds,
                             Pageable pageable) {
        boolean hasCat = categoryIds != null && !categoryIds.isEmpty();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String rdo = (regionDo == null || regionDo.isBlank()) ? null : regionDo.trim();
        String rsi = (regionSi == null || regionSi.isBlank()) ? null : regionSi.trim();
        return clubRepository.search(kw, rdo, rsi, categoryIds, hasCat, pageable);
    }
}
