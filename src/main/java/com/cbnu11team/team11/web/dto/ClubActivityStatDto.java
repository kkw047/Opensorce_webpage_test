package com.cbnu11team.team11.web.dto;

import lombok.Data;

@Data
public class ClubActivityStatDto {
    private String title;       // 일정 제목
    private String date;        // 날짜
    private int totalMember;    // 승인된 전체 인원
    private int attendedMember; // 실제 출석한 인원
    private int rate;           // 출석률 (%)

    public ClubActivityStatDto(String title, String date, int total, int attended) {
        this.title = title;
        this.date = date;
        this.totalMember = total;
        this.attendedMember = attended;
        // 0으로 나누기 방지
        this.rate = (total == 0) ? 0 : (int) ((double) attended / total * 100);
    }
}