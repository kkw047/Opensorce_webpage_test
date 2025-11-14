package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.ClubMember;
import com.cbnu11team.team11.domain.ClubMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClubMemberRepository extends JpaRepository<ClubMember, ClubMemberId> {
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