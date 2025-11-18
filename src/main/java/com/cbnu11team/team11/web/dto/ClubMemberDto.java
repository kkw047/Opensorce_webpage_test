package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.ClubMember;

public record ClubMemberDto(
        Long userId,
        String nickname,
        String role,
        String imageUrl
) {
    /**
     * ClubMember 엔티티로부터 DTO를 생성하는 정적 팩토리 메소드
     */
    public static ClubMemberDto fromEntity(ClubMember member) {
        return new ClubMemberDto(
                member.getUser().getId(),
                member.getUser().getNickname(),
                member.getRole(),
                member.getUser().getImageUrl()
        );
    }
}