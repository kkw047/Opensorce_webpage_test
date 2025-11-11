package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;

    /* ---------- Regions ---------- */
    @Transactional(readOnly = true)
    public List<String> getAllDos() {
        return regionKorRepository.findAllDos();
    }

    @Transactional(readOnly = true)
    public List<String> getSisByDo(String rdo) {
        return regionKorRepository.findSisByDo(rdo);
    }

    /* ---------- Categories ---------- */
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category createCategoryIfNotExists(String name) {
        String n = StringUtils.hasText(name) ? name.trim() : "";
        return categoryRepository.findByNameIgnoreCase(n)
                .orElseGet(() -> categoryRepository.save(new Category(n)));
    }

    /* ---------- Clubs ---------- */
    @Transactional(readOnly = true)
    public Page<Club> search(String rdo, String rsi, String kw, List<Long> catIds, Pageable pageable) {
        Specification<Club> spec = Specification.where(null);

        if (StringUtils.hasText(rdo)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("regionDo"), rdo));
        }
        if (StringUtils.hasText(rsi)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("regionSi"), rsi));
        }
        if (StringUtils.hasText(kw)) {
            String like = "%" + kw.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like)
            ));
        }
        if (catIds != null && !catIds.isEmpty()) {
            spec = spec.and((root, q, cb) -> {
                q.distinct(true);
                Join<Object, Object> j = root.join("categories", JoinType.LEFT);
                return j.get("id").in(catIds);
            });
        }

        return clubRepository.findAll(spec, pageable);
    }

    @Transactional
    public Club createClub(String name,
                           String description,
                           String regionDo,
                           String regionSi,
                           List<Long> categoryIds,
                           String imageUrl,
                           List<String> newCategoryNames) {

        Set<Category> cats = new HashSet<>();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            cats.addAll(categoryRepository.findAllByIdIn(categoryIds));
        }
        if (newCategoryNames != null && !newCategoryNames.isEmpty()) {
            for (String nm : newCategoryNames) {
                if (StringUtils.hasText(nm)) {
                    cats.add(createCategoryIfNotExists(nm.trim()));
                }
            }
        }

        Club c = new Club();
        c.setName(name);
        c.setDescription(description);
        c.setRegionDo(regionDo);
        c.setRegionSi(regionSi);
        c.setImageUrl(imageUrl);
        c.setCategories(cats);

        return clubRepository.save(c);
    }
}
