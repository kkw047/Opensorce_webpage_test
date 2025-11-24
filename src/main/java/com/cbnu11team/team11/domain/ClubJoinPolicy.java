package com.cbnu11team.team11.domain;

public enum ClubJoinPolicy {
    AUTOMATIC, // 자동 가입 (바로 ACTIVE)
    APPROVAL   // 승인제 (WAITING -> 관리자 수락 필요)
}