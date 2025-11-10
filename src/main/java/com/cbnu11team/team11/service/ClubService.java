package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.ClubRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ClubService {

    private final ClubRepository clubRepository;

    public ClubService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    public Page<Club> list(Pageable pageable) {
        return clubRepository.findAllWithCategories(pageable);
    }

    public Page<Club> search(String regionDo, String regionSi, String keyword, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String doVal = (regionDo == null || regionDo.isBlank()) ? null : regionDo.trim();
        String siVal = (regionSi == null || regionSi.isBlank()) ? null : regionSi.trim();
        return clubRepository.search(doVal, siVal, kw, pageable);
    }
}
