package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ClubService {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate; // region_kor에서 지역 목록 뽑는다(새 파일 추가 없음)

    // --- 목록/검색 ---
    @Transactional(readOnly = true)
    public Page<Club> list(Pageable pageable) {
        return clubRepository.findAllByOrderByIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Club> search(String rdo, String rsi, String kw, List<Long> cats, Pageable pageable) {
        boolean hasCats = (cats != null && !cats.isEmpty());
        List<Club> list = clubRepository.search(
                nullIfBlank(rdo),
                nullIfBlank(rsi),
                nullIfBlank(kw),
                hasCats,
                hasCats ? cats : Collections.emptyList(),
                pageable
        );
        // 수동 페이징(검색은 List 반환이므로)
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), list.size());
        List<Club> page = from > list.size() ? Collections.emptyList() : list.subList(from, to);
        return new PageImpl<>(page, pageable, list.size());
    }

    // --- 생성 ---
    public Club createClub(String name, String desc, String rdo, String rsi, List<Long> categoryIds, String imagePath) {
        Club club = Club.builder()
                .name(name)
                .description(desc)
                .regionDo(rdo)
                .regionSi(rsi)
                .imagePath(imagePath)
                .build();

        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> cats = categoryRepository.findAllById(categoryIds);
            club.getCategories().addAll(cats);
        }
        return clubRepository.save(club);
    }

    // --- 카테고리 ---
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category createCategoryIfNotExists(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("empty category");
        return categoryRepository.findByName(trimmed)
                .orElseGet(() -> categoryRepository.save(Category.builder().name(trimmed).build()));
    }

    // --- 지역(도/시군구) : region_kor 테이블 사용 ---
    @Transactional(readOnly = true)
    public List<String> getAllDos() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT DISTINCT region_do FROM region_kor ORDER BY region_do", String.class);
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<String> getSisByDo(String regionDo) {
        if (regionDo == null || regionDo.isBlank()) return Collections.emptyList();
        try {
            return jdbcTemplate.queryForList(
                    "SELECT DISTINCT region_si FROM region_kor WHERE region_do=? ORDER BY region_si",
                    String.class, regionDo);
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
