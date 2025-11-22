package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.ClubMember;
import com.cbnu11team.team11.domain.ClubMemberId;
import com.cbnu11team.team11.domain.ClubMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ClubMemberRepository extends JpaRepository<ClubMember, ClubMemberId> {
    @Query("""
           select cm.club.id as clubId, count(cm) as cnt
           from ClubMember cm
           where cm.club.id in :ids AND cm.status = 'ACTIVE'
           group by cm.club.id
           """)
    List<ClubMemberCount> countMembersByClubIds(@Param("ids") List<Long> clubIds);

    interface ClubMemberCount{
        Long getClubId();
        Long getCnt();
    }

    @Query("SELECT cm FROM ClubMember cm JOIN FETCH cm.user WHERE cm.club.id = :clubId AND cm.status = :status")
    List<ClubMember> findByClubIdAndStatusWithUser(@Param("clubId") Long clubId, @Param("status") ClubMemberStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId")
    void deleteByClubIdAndUserId(@Param("clubId") Long clubId, @Param("userId") Long userId);
}