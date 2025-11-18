package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Club; // [추가]
import com.cbnu11team.team11.domain.ClubMember;
import com.cbnu11team.team11.domain.ClubMemberId;
import com.cbnu11team.team11.domain.User; // [추가]
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubMemberRepository extends JpaRepository<ClubMember, ClubMemberId> {

    // [추가된 메소드] 유저가 해당 클럽의 멤버인지 확인 (존재하면 true)
    // Spring Data JPA가 ClubMember 엔티티의 'user'와 'club' 필드를 기준으로 쿼리를 자동 생성합니다.
    boolean existsByUserAndClub(User user, Club club);

    // --- [기존 코드 유지] ---
    @Query("""
           select cm.club.id as clubId, count(cm) as cnt
           from ClubMember cm
           where cm.club.id in :ids
           group by cm.club.id
           """)
    List<ClubMemberCount> countMembersByClubIds(@Param("ids") List<Long> clubIds);

    interface ClubMemberCount{
        Long getClubId();
        Long getCnt();
    }
}