package com.cbnu11team.opensource11.club.dto;

import java.time.LocalDateTime;

public class ClubCard {
    private Long id;
    private String name;
    private String category;
    private String region;
    private String thumbnailUrl;
    private int memberCount;
    private LocalDateTime recentActivityAt;

    public ClubCard(Long id, String name, String category, String region,
                    String thumbnailUrl, int memberCount, LocalDateTime recentActivityAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.region = region;
        this.thumbnailUrl = thumbnailUrl;
        this.memberCount = memberCount;
        this.recentActivityAt = recentActivityAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getRegion() { return region; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public int getMemberCount() { return memberCount; }
    public java.time.LocalDateTime getRecentActivityAt() { return recentActivityAt; }
}
