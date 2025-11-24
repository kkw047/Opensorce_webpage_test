package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.*;
import com.cbnu11team.team11.web.dto.ClubActivityStatDto; // [추가됨]
import com.cbnu11team.team11.web.dto.ClubDetailDto;
import com.cbnu11team.team11.web.dto.ClubForm;
import com.cbnu11team.team11.web.dto.ClubMemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.*;
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

    // [병합 포인트 1] 활동 지표 조회를 위해 CalendarRepository 추가
    private final CalendarRepository calendarRepository;

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

        Club savedClub = clubRepository.save(club);

        // 모임 생성자를 "MANAGER" (또는 ADMIN) 역할로 멤버 목록에 추가
        ClubMember ownerMembership = new ClubMember();
        ownerMembership.setId(new ClubMemberId(savedClub.getId(), owner.getId()));
        ownerMembership.setClub(savedClub);
        ownerMembership.setUser(owner);
        ownerMembership.setRole(ClubRole.MANAGER);
        ownerMembership.setStatus(ClubMemberStatus.ACTIVE);

        clubMemberRepository.save(ownerMembership);

        return savedClub;
    }

    /* ===== Member Management (Ban, Join, Approve, etc.) ===== */
    @Transactional
    public void banMember(Long clubId, Long userId) {
        ClubMemberId id = new ClubMemberId(clubId, userId);
        ClubMember member = clubMemberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다."));

        member.setStatus(ClubMemberStatus.BANNED);
        member.setRole(ClubRole.MEMBER);
    }

    @Transactional
    public ClubMemberStatus joinClub(Long clubId, Long userId) {
        ClubMemberId memberId = new ClubMemberId(clubId, userId);

        Optional<ClubMember> existingMember = clubMemberRepository.findById(memberId);
        if (existingMember.isPresent()) {
            ClubMemberStatus status = existingMember.get().getStatus();
            if (status == ClubMemberStatus.BANNED) {
                throw new IllegalStateException("이 모임에서 차단되어 재가입할 수 없습니다.");
            }
            if (status == ClubMemberStatus.WAITING) {
                throw new IllegalStateException("이미 가입 신청이 완료된 모임입니다. 승인을 기다려주세요.");
            }
            throw new IllegalStateException("이미 가입된 모임입니다.");
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다. ID: " + clubId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. ID: " + userId));

        ClubMember newMember = new ClubMember();
        newMember.setId(memberId);
        newMember.setClub(club);
        newMember.setUser(user);
        newMember.setRole(ClubRole.MEMBER);

        // 정책에 따른 초기 상태 설정
        if (club.getJoinPolicy() == ClubJoinPolicy.AUTOMATIC) {
            newMember.setStatus(ClubMemberStatus.ACTIVE);
        } else {
            newMember.setStatus(ClubMemberStatus.WAITING);
        }

        clubMemberRepository.save(newMember);
        return newMember.getStatus();
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
        if (optClub.isEmpty()) return Optional.empty();

        Club club = optClub.get();
        boolean isOwner = club.getOwner() != null && club.getOwner().getId().equals(currentUserId);
        boolean isAlreadyMember = false;
        boolean isManager = false;
        String myStatus = null;

        if (currentUserId != null) {
            ClubMemberId memberId = new ClubMemberId(clubId, currentUserId);
            Optional<ClubMember> memberOpt = clubMemberRepository.findById(memberId);

            if (memberOpt.isPresent()) {
                isAlreadyMember = true;
                myStatus = memberOpt.get().getStatus().name();
                if (memberOpt.get().getRole() == ClubRole.MANAGER || memberOpt.get().getRole() == ClubRole.ADMIN) {
                    isManager = true;
                }
            }
        }

        List<ClubMember> activeMembers = club.getMembers().stream()
                .filter(m -> m.getStatus() == ClubMemberStatus.ACTIVE)
                .toList();

        ClubDetailDto dto = new ClubDetailDto(
                club.getId(),
                club.getName(),
                club.getDescription(),
                club.getImageUrl(),
                isOwner,
                isManager,
                isAlreadyMember,
                club.getCategories().stream().map(Category::getName).toList(),
                activeMembers.stream().map(ClubMemberDto::fromEntity).toList(),
                club.getJoinPolicy(),
                club.getOwner().getId(),
                myStatus
        );

        return Optional.of(dto);
    }

    // --- 관리자 기능 ---
    @Transactional(readOnly = true)
    public List<ClubMember> getMembersByStatus(Long clubId, ClubMemberStatus status) {
        return clubMemberRepository.findByClubIdAndStatusWithUser(clubId, status);
    }

    @Transactional
    public void approveMember(Long clubId, Long memberId) {
        ClubMemberId id = new ClubMemberId(clubId, memberId);
        ClubMember member = clubMemberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다."));
        member.setStatus(ClubMemberStatus.ACTIVE);
    }

    @Transactional
    public void kickMember(Long clubId, Long memberId) {
        ClubMemberId id = new ClubMemberId(clubId, memberId);
        clubMemberRepository.deleteById(id);
    }

    @Transactional
    public void rejectMember(Long clubId, Long userId, String reason) {
        ClubMemberId id = new ClubMemberId(clubId, userId);
        clubMemberRepository.deleteById(id);
    }

    @Transactional
    public void deleteClub(Long clubId) {
        clubRepository.deleteById(clubId);
    }

    @Transactional
    public void updateClub(Long clubId, ClubForm form, Long userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));
        club.setName(form.name());
        club.setDescription(form.description());

        if (form.imageFile() != null && !form.imageFile().isEmpty()) {
            String imageUrl = fileStorageService.save(form.imageFile());
            club.setImageUrl(imageUrl);
        }
    }

    @Transactional
    public void updateClubPolicy(Long clubId, ClubJoinPolicy policy) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));
        club.setJoinPolicy(policy);

        if (policy == ClubJoinPolicy.AUTOMATIC) {
            List<ClubMember> waitingMembers = clubMemberRepository.findByClubIdAndStatusWithUser(clubId, ClubMemberStatus.WAITING);
            for (ClubMember member : waitingMembers) {
                member.setStatus(ClubMemberStatus.ACTIVE);
            }
        }
    }

    @Transactional
    public void leaveClub(Long clubId, Long userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));

        if (club.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("모임장은 탈퇴할 수 없습니다. 모임을 삭제하거나 권한을 양도하세요.");
        }
        clubMemberRepository.deleteByClubIdAndUserId(clubId, userId);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    /**
     * [병합 포인트 2] 모임 활동 지표 (최근 일정 3개의 출석률)
     * 두 번째 코드에서 가져온 기능입니다.
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