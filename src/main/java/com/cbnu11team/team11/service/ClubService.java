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
import com.cbnu11team.team11.repository.ClubMemberRepository;
import com.cbnu11team.team11.web.dto.ClubDetailDto;
import com.cbnu11team.team11.web.dto.ClubForm;
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
    private final ClubMemberRepository clubMemberRepository;

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
    @Transactional
    public Club createClub(Long ownerId, ClubForm form) { // 파라미터를 DTO로 받음

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + ownerId));

        Club club = new Club();
        club.setName(form.name().trim());
        club.setDescription((form.description() == null) ? null : form.description().trim());
        club.setRegionDo(form.regionDo());
        club.setRegionSi(form.regionSi());
        club.setOwner(owner);

        if (form.imageFile() != null && !form.imageFile().isEmpty()) {
            String url = fileStorageService.save(form.imageFile());
            club.setImageUrl(url);
        }

        if (form.categoryIds() != null && !form.categoryIds().isEmpty()) {
            List<Category> cats = categoryRepository.findAllByIdIn(form.categoryIds());
            club.getCategories().addAll(cats);
        }

        if (form.newCategoryName() != null && !form.newCategoryName().isBlank()) {
            Category nc = createCategoryIfNotExists(form.newCategoryName());
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

    /* ===== Join Club ===== */
    @Transactional
    public void joinClub(Long clubId, Long userId) {
        ClubMemberId memberId = new ClubMemberId(clubId, userId);
        if (clubMemberRepository.existsById(memberId)) {
            throw new IllegalStateException("이미 가입한 모임입니다.");
        }

        // 연관 엔티티 조회
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다. ID: " + clubId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));

        // 새 멤버 생성
        ClubMember newMember = new ClubMember();
        newMember.setId(memberId);
        newMember.setClub(club);
        newMember.setUser(user);
        newMember.setRole("MEMBER");

        clubMemberRepository.save(newMember);
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

    // 내 모임 목록 조회 서비스 메소드 추가
    public Page<Club> searchMyClubs(Long userId,
                                    String q,
                                    String regionDo,
                                    String regionSi,
                                    List<Long> categoryIds,
                                    Pageable pageable) {

        // 기본 검색 스펙 (search 메소드와 동일)
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
                return cats.get("id").in(categoryIds);
            });
        }

        spec = spec.and((root, query, cb) -> {
            query.distinct(true);
            Join<Club, ClubMember> members = root.join("members");
            return cb.equal(members.get("user").get("id"), userId);
        });

        return clubRepository.findAll(spec, pageable);
    }

    /* ===== Detail ===== */
    @Transactional(readOnly = true)
    public Optional<Club> findById(Long clubId) {
        return clubRepository.findById(clubId);
    }

    /* ===== Detail DTO 로직 ===== */
    /**
     * 모임 상세 정보와 현재 사용자 관련 정보를 DTO로 조회
     * @param clubId 모임 ID
     * @param currentUserId 현재 로그인한 사용자 ID (null일 수 있음)
     * @return 뷰에 전달할 ClubDetailDto
     */
    @Transactional(readOnly = true)
    public Optional<ClubDetailDto> getClubDetail(Long clubId, Long currentUserId) {
        // @EntityGraph가 적용된 findById 호출 (members, categories 등 모두 EAGER 조회)
        Optional<Club> optClub = clubRepository.findById(clubId);
        if (optClub.isEmpty()) {
            return Optional.empty();
        }

        Club club = optClub.get();

        boolean isOwner = club.getOwner() != null && club.getOwner().getId().equals(currentUserId);

        boolean isAlreadyMember = false;
        if (currentUserId != null) {
            ClubMemberId memberId = new ClubMemberId(clubId, currentUserId);
            isAlreadyMember = clubMemberRepository.existsById(memberId);
        }

        // 엔티티 -> DTO 변환
        ClubDetailDto dto = ClubDetailDto.fromEntity(club, isOwner, isAlreadyMember);
        return Optional.of(dto);
    }
}