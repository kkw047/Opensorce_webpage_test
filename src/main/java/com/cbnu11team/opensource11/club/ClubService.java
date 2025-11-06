package com.cbnu11team.opensource11.club;

import com.cbnu11team.opensource11.club.dto.ClubCard;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;

    public ClubService(ClubRepository clubRepository, MembershipRepository membershipRepository) {
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
    }

    /** 카테고리/검색어로 모임 카드 목록 조회 */
    public List<ClubCard> findClubs(String categoryIdOrName, String q) {
        List<Club> rows;

        // category는 문자열로 저장되어 있다고 가정
        boolean hasCategory = categoryIdOrName != null && !categoryIdOrName.isBlank()
                && !"all".equalsIgnoreCase(categoryIdOrName);
        boolean hasQuery = q != null && !q.isBlank();

        if (hasCategory && hasQuery) {
            rows = clubRepository.findByCategoryAndNameContainingIgnoreCase(categoryIdOrName, q);
        } else if (hasCategory) {
            rows = clubRepository.findByCategory(categoryIdOrName);
        } else if (hasQuery) {
            rows = clubRepository.findByNameContainingIgnoreCase(q);
        } else {
            rows = clubRepository.findAll();
        }

        return rows.stream().map(c ->
                new ClubCard(
                        c.getId(),
                        c.getName(),
                        c.getCategory(),
                        c.getRegion(),
                        c.getThumbnailUrl(),                       // 이미지
                        membershipRepository.countByClubId(c.getId()), // 멤버 수
                        c.getCreatedAt()                               // 최근활동(없으면 생성일)
                )
        ).toList();
    }
}
