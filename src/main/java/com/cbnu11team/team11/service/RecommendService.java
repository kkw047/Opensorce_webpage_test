package com.cbnu11team.team11.service;

import com.cbnu11team.team11.repository.ClubRecommendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final ClubRecommendRepository clubRecommendRepository;

    private static final int DEFAULT_LIMIT = 20;

    @RequiredArgsConstructor
    @lombok.Getter
    public static class RecommendClubView {
        private final Long clubId;
        private final String name;
        private final String description;
        private final String regionDo;
        private final String regionSi;
        private final String imageUrl;
        private final long matchedCategoryCount;
    }

    @RequiredArgsConstructor
    @lombok.Getter
    public static class PopularClubView {
        private final Long clubId;
        private final String name;
        private final String description;
        private final String regionDo;
        private final String regionSi;
        private final String imageUrl;
        private final long memberCount;
    }

    @RequiredArgsConstructor
    @lombok.Getter
    public static class ActiveClubView {
        private final Long clubId;
        private final String name;
        private final String description;
        private final String regionDo;
        private final String regionSi;
        private final String imageUrl;
        private final Long calendarId;
        private final LocalDateTime latestEndDate;
        private final long attendedCount;
        private final long totalCount;
        private final double attendanceRate; // %
    }

    public List<RecommendClubView> getRecommendedClubs(Long userId) {
        if (userId == null) return Collections.emptyList();

        var rows = clubRecommendRepository.findRecommendedClubs(userId, DEFAULT_LIMIT);
        return rows.stream()
                .map(r -> new RecommendClubView(
                        r.getClubId(),
                        r.getName(),
                        r.getDescription(),
                        r.getRegionDo(),
                        r.getRegionSi(),
                        r.getImageUrl(),
                        r.getMatchedCategoryCount() == null ? 0L : r.getMatchedCategoryCount()
                ))
                .sorted(
                        Comparator
                                .comparingLong(RecommendClubView::getMatchedCategoryCount).reversed()
                                .thenComparing(RecommendClubView::getName, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();
    }

    public List<PopularClubView> getPopularClubs(Long userId) {
        if (userId == null) return Collections.emptyList();

        var rows = clubRecommendRepository.findPopularClubs(userId, DEFAULT_LIMIT);
        return rows.stream()
                .map(r -> new PopularClubView(
                        r.getClubId(),
                        r.getName(),
                        r.getDescription(),
                        r.getRegionDo(),
                        r.getRegionSi(),
                        r.getImageUrl(),
                        r.getMemberCount() == null ? 0L : r.getMemberCount()
                ))
                .sorted(
                        Comparator
                                .comparingLong(PopularClubView::getMemberCount).reversed()
                                .thenComparing(PopularClubView::getName, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();
    }

    public List<ActiveClubView> getActiveClubs(Long userId) {
        if (userId == null) return Collections.emptyList();

        var rows = clubRecommendRepository.findActiveClubs(userId, DEFAULT_LIMIT);
        return rows.stream()
                .map(r -> new ActiveClubView(
                        r.getClubId(),
                        r.getName(),
                        r.getDescription(),
                        r.getRegionDo(),
                        r.getRegionSi(),
                        r.getImageUrl(),
                        r.getCalendarId(),
                        r.getLatestEndDate() == null
                                ? null
                                : r.getLatestEndDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime(),
                        r.getAttendedCount() == null ? 0L : r.getAttendedCount(),
                        r.getTotalCount() == null ? 0L : r.getTotalCount(),
                        r.getAttendanceRate() == null ? 0.0 : r.getAttendanceRate()
                ))
                .sorted(
                        Comparator
                                .comparingDouble(ActiveClubView::getAttendanceRate).reversed()
                                .thenComparing(
                                        ActiveClubView::getLatestEndDate,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                                .thenComparing(ActiveClubView::getName, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();
    }
}
