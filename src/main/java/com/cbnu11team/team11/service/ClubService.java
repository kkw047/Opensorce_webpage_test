package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;

    /* ---------- 검색 ---------- */
    @Transactional(readOnly = true)
    public Page<Club> search(String rdo, String rsi, String kw, List<Long> cats, Pageable pageable) {
        String regionDo = emptyToNull(rdo);
        String regionSi = emptyToNull(rsi);
        String keyword  = emptyToNull(kw);

        boolean hasCats = cats != null && !cats.isEmpty();
        List<Long> catParam = hasCats ? cats : List.of(-1L); // 파라미터 바인딩 보장용 더미

        return clubRepository.search(regionDo, regionSi, keyword, hasCats, catParam, pageable);
    }

    /* ---------- 생성 ---------- */
    @Transactional
    public Club createClub(String name,
                           String description,
                           String regionDo,
                           String regionSi,
                           List<Long> categoryIds,
                           String imageUrl,
                           List<String> newCategoryNames) {

        // 중복 방지: 선택 카테고리 + 새 카테고리 모두 Set으로 정규화
        Set<Long> idSet = new LinkedHashSet<>();
        if (categoryIds != null) idSet.addAll(categoryIds);

        if (newCategoryNames != null) {
            for (String raw : newCategoryNames) {
                if (!StringUtils.hasText(raw)) continue;
                String norm = raw.trim();
                Category cat = categoryRepository.findByName(norm).orElseGet(() -> {
                    Category c = new Category();
                    c.setName(norm);
                    return categoryRepository.save(c);
                });
                idSet.add(cat.getId());
            }
        }

        List<Category> categories = idSet.isEmpty()
                ? List.of()
                : categoryRepository.findAllById(idSet);

        Club c = new Club();
        c.setName(name);
        c.setDescription(description);
        c.setRegionDo(regionDo);
        c.setRegionSi(regionSi);
        c.setImageUrl(imageUrl);
        c.setCategories(new HashSet<>(categories)); // ManyToMany: Set으로 중복 차단

        return clubRepository.save(c);
    }

    /* ---------- 카테고리 유틸 (모달에서 '카테고리 추가'용) ---------- */
    @Transactional
    public Category createCategoryIfNotExists(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw new IllegalArgumentException("카테고리 이름이 비어 있습니다.");
        }
        String name = rawName.trim();
        return categoryRepository.findByName(name).orElseGet(() -> {
            try {
                Category c = new Category();
                c.setName(name);
                return categoryRepository.save(c);
            } catch (DataIntegrityViolationException e) {
                // 동시성으로 인해 유니크 제약 위반 시 재조회
                return categoryRepository.findByName(name)
                        .orElseThrow(() -> e);
            }
        });
    }

    /* ---------- 지역 목록 ---------- */
    @Transactional(readOnly = true)
    public List<String> getAllDos() {
        return regionKorRepository.findAllDos();
    }

    @Transactional(readOnly = true)
    public List<String> getSisByDo(String regionDo) {
        if (!StringUtils.hasText(regionDo)) return List.of();
        return regionKorRepository.findSisByDo(regionDo);
    }

    /* ---------- 카테고리 ---------- */
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
