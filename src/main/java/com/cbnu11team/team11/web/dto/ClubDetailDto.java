package com.cbnu11team.team11.web.dto;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;

import java.util.List;
import java.util.stream.Collectors;

public record ClubDetailDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        boolean isOwner,
        boolean isManager,
        boolean isAlreadyMember,
        List<String> categories,
        List<ClubMemberDto> members
) {
    /**
     * Club 엔티티와 부가 정보로부터 DTO를 생성하는 정적 팩토리 메소드
     */
    public static ClubDetailDto fromEntity(Club club, boolean isOwner, boolean isManager, boolean isAlreadyMember) {
        return new ClubDetailDto(
                club.getId(),
                club.getName(),
                club.getDescription(),
                club.getImageUrl(),
                isOwner,
                isManager,
                isAlreadyMember,
                club.getCategories().stream() // 지연 로딩 회피 (서비스단에서 조회 완료)
                        .map(Category::getName)
                        .collect(Collectors.toList()),
                club.getMembers().stream() // 지연 로딩 회피 (서비스단에서 조회 완료)
                        .map(ClubMemberDto::fromEntity) // 내부 DTO로 변환
                        .collect(Collectors.toList())
        );
    }
}