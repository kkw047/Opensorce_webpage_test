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

    public List<String> getAllDos() { return regionKorRepository.findDistinctDos(); }
    public List<String> getSisByDo(String regionDo) { return regionKorRepository.findSisByDo(regionDo); }

    public List<Category> findAllCategories() { return categoryRepository.findAllOrderByNameAsc(); }

    public Category createCategoryIfNotExists(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("카테고리 이름이 비었습니다.");
        String n = name.trim();
        return categoryRepository.findByNameIgnoreCase(n)
                .orElseGet(() -> categoryRepository.save(new Category(null, n, null)));
    }

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

    public Page<Club> search(String q,
                             String regionDo,
                             String regionSi,
                             List<Long> categoryIds,
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

    public Page<Club> searchMyClubs(Long userId,
                                    String q,
                                    String regionDo,
                                    String regionSi,
                                    List<Long> categoryIds,
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

    public Page<Club> findMyClubs(Long userId, Pageable pageable) {
        return searchMyClubs(userId, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Club> findById(Long clubId) {
        Optional<Club> optClub = clubRepository.findById(clubId);

        if (optClub.isPresent()) {
            Club club = optClub.get();

            // 멤버 목록 로딩
            List<ClubMember> members = club.getMembers();

            // 각 멤버의 유저 정보(닉네임 등)를 미리 건드려서 로딩시킴
            for (ClubMember member : members) {
                member.getUser().getNickname();
            }
        }

        return optClub;
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

        ClubDetailDto dto = ClubDetailDto.fromEntity(club, isOwner, isAlreadyMember);
        return Optional.of(dto);
    }
}