package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.domain.User;
import com.cbnu11team.team11.domain.ClubMember;
import com.cbnu11team.team11.domain.ClubMemberId;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import com.cbnu11team.team11.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /* ===== Region ===== */
    public List<String> getAllDos() { return regionKorRepository.findDistinctDos(); }
    public List<String> getSisByDo(String regionDo) { return regionKorRepository.findSisByDo(regionDo); }

    /* ===== Category ===== */
    public List<Category> findAllCategories() { return categoryRepository.findAllOrderByNameAsc(); }

    public Category createCategoryIfNotExists(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("카테고리 이름이 비었습니다.");
        String n = name.trim();
        return categoryRepository.findByNameIgnoreCase(n)
                .orElseGet(() -> categoryRepository.save(new Category(null, n, null)));
    }

    /* ===== Create Club ===== */
    @Transactional // 트랜잭션 보장
    public Club createClub(Long ownerId,
                           String name,
                           String description,
                           String regionDo,
                           String regionSi,
                           List<Long> categoryIds,
                           String newCategoryName,
                           org.springframework.web.multipart.MultipartFile imageFile) {

        // 서비스 내에서 직접 User 엔티티를 조회 (트랜잭션 내에서 관리)
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + ownerId));

        Club club = new Club();
        club.setName(name.trim());
        club.setDescription((description == null) ? null : description.trim());
        club.setRegionDo(regionDo);
        club.setRegionSi(regionSi);
        club.setOwner(owner);

        if (imageFile != null && !imageFile.isEmpty()) {
            String url = fileStorageService.save(imageFile);
            club.setImageUrl(url);
        }

        // 기존 카테고리
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> cats = categoryRepository.findAllByIdIn(categoryIds);
            club.getCategories().addAll(cats);
        }

        // 새 카테고리 추가
        if (newCategoryName != null && !newCategoryName.isBlank()) {
            Category nc = createCategoryIfNotExists(newCategoryName);
            club.getCategories().add(nc);
        }

        // 모임 생성자를 "ADMIN" 역할로 멤버 목록에 추가
        if (owner != null) {
            ClubMember ownerMembership = new ClubMember();
            ownerMembership.setId(new ClubMemberId(null, owner.getId()));
            ownerMembership.setClub(club);
            ownerMembership.setUser(owner);
            ownerMembership.setRole("ADMIN");

            club.getMembers().add(ownerMembership);
        }

        return clubRepository.save(club);
    }

    /* ===== Search/List ===== */
    public Page<Club> search(String q,
                             String regionDo,
                             String regionSi,
                             List<Long> categoryIds, // 다중 체크박스
                             Pageable pageable) {

        Specification<Club> spec = (root, query, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(root.get("name"), like));
        }
        if (regionDo != null && !regionDo.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("regionDo"), regionDo));
        }
        if (regionSi != null && !regionSi.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("regionSi"), regionSi));
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Club, Category> cats = root.join("categories", JoinType.INNER);
                return cats.get("id").in(categoryIds); // ANY-OF
            });
        }

        return clubRepository.findAll(spec, pageable);
    }
    /* ===== Detail ===== */
    public Optional<Club> findById(Long clubId) {
        return clubRepository.findById(clubId);
    }
}
