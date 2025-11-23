package com.cbnu11team.team11.service;

import com.cbnu11team.team11.domain.*;
import com.cbnu11team.team11.repository.CategoryRepository;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.repository.RegionKorRepository;
import com.cbnu11team.team11.repository.UserRepository;
import com.cbnu11team.team11.repository.ClubMemberRepository;
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

        Club savedClub = clubRepository.save(club);

        ClubMember ownerMembership = new ClubMember();

        // 모임 생성자를 "ADMIN" 역할로 멤버 목록에 추가
        ownerMembership.setId(new ClubMemberId(savedClub.getId(), owner.getId()));
        ownerMembership.setClub(savedClub);
        ownerMembership.setUser(owner);
        ownerMembership.setRole(ClubRole.MANAGER);
        ownerMembership.setStatus(ClubMemberStatus.ACTIVE);

        clubMemberRepository.save(ownerMembership);

        return savedClub;
    }

    @Transactional
    public void banMember(Long clubId, Long userId) {
        ClubMemberId id = new ClubMemberId(clubId, userId);
        ClubMember member = clubMemberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다."));

        member.setStatus(ClubMemberStatus.BANNED);
        member.setRole(ClubRole.MEMBER); // 혹시 매니저였다면 일반 등급으로 강등 후 차단
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
            // 상태에 따라 에러 메시지 분리
            if (status == ClubMemberStatus.WAITING) {
                throw new IllegalStateException("이미 가입 신청이 완료된 모임입니다. 승인을 기다려주세요.");
            }
            // ACTIVE인 경우
            throw new IllegalStateException("이미 가입된 모임입니다.");
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
        newMember.setRole(ClubRole.MEMBER);

        if (club.getJoinPolicy() == ClubJoinPolicy.AUTOMATIC) {
            newMember.setStatus(ClubMemberStatus.ACTIVE); // 자동 가입 -> 활동 중
        } else {
            newMember.setStatus(ClubMemberStatus.WAITING); // 승인제 -> 대기 중
        }

        clubMemberRepository.save(newMember);

        return newMember.getStatus();
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
    @Transactional(readOnly = true)
    public List<ClubMember> getMembersByStatus(Long clubId, ClubMemberStatus status) {
        return clubMemberRepository.findByClubIdAndStatusWithUser(clubId, status);
    }

    // 가입 승인 (WAITING -> ACTIVE)
    @Transactional
    public void approveMember(Long clubId, Long memberId) {
        ClubMemberId id = new ClubMemberId(clubId, memberId);
        ClubMember member = clubMemberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다."));

        member.setStatus(ClubMemberStatus.ACTIVE); // 상태 변경
    }

    // 추방 또는 거절 (DB에서 삭제)
    @Transactional
    public void kickMember(Long clubId, Long memberId) {
        ClubMemberId id = new ClubMemberId(clubId, memberId);
        clubMemberRepository.deleteById(id); // 아예 삭제해버림
    }

    @Transactional
    public void rejectMember(Long clubId, Long userId, String reason) {
        ClubMemberId id = new ClubMemberId(clubId, userId);

        System.out.println("========== 가입 거절 ==========");
        System.out.println("Club ID: " + clubId + ", User ID: " + userId);
        System.out.println("거절 사유: " + reason);
        System.out.println("=============================");

        clubMemberRepository.deleteById(id);
    }

    @Transactional
    public void deleteClub(Long clubId) {
        // 관련된 데이터(멤버, 게시글 등)는 Entity의 CascadeType.ALL 설정에 의해 같이 삭제되거나, DB의 ON DELETE CASCADE 설정에 의해 삭제됨
        clubRepository.deleteById(clubId);
    }

    @Transactional
    public void updateClub(Long clubId, ClubForm form, Long userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("모임을 찾을 수 없습니다."));

        club.setName(form.name());
        club.setDescription(form.description());

        // 파일이 있으면 교체
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

            // 로그 찍기
            if (!waitingMembers.isEmpty()) {
                System.out.println(clubId + "번 모임이 자동 승인으로 변경되어, 대기자 " + waitingMembers.size() + "명이 일괄 승인되었습니다.");
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
}