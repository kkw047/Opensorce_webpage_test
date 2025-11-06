package com.cbnu11team.opensource11.club;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // 모임별 멤버 수
    int countByClubId(Long clubId);
}
