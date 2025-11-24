package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.*;
import com.cbnu11team.team11.web.dto.ClubActivityStatDto; // [추가]
import com.cbnu11team.team11.web.dto.ClubDetailDto;
import com.cbnu11team.team11.web.dto.ClubForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final CategoryRepository categoryRepository;
    private final RegionKorRepository regionKorRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CalendarRepository calendarRepository; // [추가]

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
    public Club createClub(Long ownerId, ClubForm form) {
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

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다. ID: " + clubId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));

        ClubMember newMember = new ClubMember();
        newMember.setId(memberId);
        newMember.setClub(club);
        newMember.setUser(user);
        newMember.setRole("MEMBER");

        clubMemberRepository.save(newMember);
    }

    /* ===== Search/List ===== */
    public Page<Club> search(String q, String regionDo, String regionSi, List<Long> categoryIds, Pageable pageable) {
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
        return clubRepository.findAll(spec, pageable);
    }

    public Page<Club> searchMyClubs(Long userId, String q, String regionDo, String regionSi, List<Long> categoryIds, Pageable pageable) {
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

    @Transactional(readOnly = true)
    public Optional<ClubDetailDto> getClubDetail(Long clubId, Long currentUserId) {
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
        return Optional.of(ClubDetailDto.fromEntity(club, isOwner, isAlreadyMember));
    }

    /**
     * [추가] 모임 활동 지표 (최근 일정 3개의 출석률)
     */
    @Transactional(readOnly = true)
    public List<ClubActivityStatDto> getRecentActivityStats(Long clubId) {
        // 1. 해당 모임의 모든 일정을 가져와서 -> 날짜 내림차순(최신순) 정렬 -> 상위 3개만 가져옴
        return calendarRepository.findAllByClubId(clubId).stream()
                .sorted((c1, c2) -> c2.getStartDate().compareTo(c1.getStartDate())) // 최신순 정렬
                .limit(3) // 3개만
                .map(cal -> {
                    // 승인된 참가자 수 (PENDING, REJECTED 제외)
                    long total = cal.getParticipants().stream()
                            .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                            .count();

                    // 출석한 참가자 수
                    long attended = cal.getParticipants().stream()
                            .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED && p.isAttended())
                            .count();

                    return new ClubActivityStatDto(
                            cal.getTitle(),
                            cal.getStartDate().toLocalDate().toString(),
                            (int) total,
                            (int) attended
                    );
                })
                .collect(Collectors.toList());
    }
}