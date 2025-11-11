package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;

    /** 메인 목록: 최신 id 내림차순 (categories 사전 로딩됨) */
    public Page<Club> findAllOrderByIdDesc(Pageable pageable) {
        return clubRepository.findAllByOrderByIdDesc(pageable);
    }

    /**
     * 검색: (도, 시군구, 키워드, 카테고리목록, 페이지)
     * JPQL의 "IN (:list)" 빈 리스트 문제를 피하기 위해 sentinel 사용
     */
    public List<Club> search(String regionDo, String regionSi, String keyword,
                             List<Long> categoryIds, Pageable pageable) {
        boolean hasCat = categoryIds != null && !categoryIds.isEmpty();
        List<Long> safeIds = hasCat ? categoryIds : Collections.singletonList(-1L); // 사용되지더라도 바인딩 안전
        return clubRepository.search(regionDo, regionSi, keyword, hasCat, safeIds, pageable);
    }

    @Transactional
    public Club save(Club club) {
        return clubRepository.save(club);
    }

    public Optional<Club> findById(Long id) {
        return clubRepository.findById(id);
    }
}
