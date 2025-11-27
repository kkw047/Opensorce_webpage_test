package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.Club;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface ClubRecommendRepository extends Repository<Club, Long> {
    interface RecommendedClubRow {
        Long getClubId();
        String getName();
        String getDescription();
        String getRegionDo();
        String getRegionSi();
        String getImageUrl();
        Long getMatchedCategoryCount();
    }

    interface PopularClubRow {
        Long getClubId();
        String getName();
        String getDescription();
        String getRegionDo();
        String getRegionSi();
        String getImageUrl();
        Long getMemberCount();
    }

    interface ActiveClubRow {
        Long getClubId();
        String getName();
        String getDescription();
        String getRegionDo();
        String getRegionSi();
        String getImageUrl();
        Long getCalendarId();
        Timestamp getLatestEndDate();
        Long getAttendedCount();
        Long getTotalCount();
        Double getAttendanceRate();
    }

    @Query(value = """
        SELECT
            c.id            AS clubId,
            c.name          AS name,
            c.description   AS description,
            c.region_do     AS regionDo,
            c.region_si     AS regionSi,
            c.image_url     AS imageUrl,
            COUNT(DISTINCT cc.category_id) AS matchedCategoryCount
        FROM clubs c
        JOIN club_categories cc
          ON cc.club_id = c.id
        JOIN user_categories uc
          ON uc.category_id = cc.category_id
        JOIN users u
          ON u.id = uc.user_id
        WHERE u.id = :userId
          AND c.region_do = u.region_do
          AND c.region_si = u.region_si
        GROUP BY c.id, c.name, c.description, c.region_do, c.region_si, c.image_url
        ORDER BY matchedCategoryCount DESC, c.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecommendedClubRow> findRecommendedClubs(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT
            c.id          AS clubId,
            c.name        AS name,
            c.description AS description,
            c.region_do   AS regionDo,
            c.region_si   AS regionSi,
            c.image_url   AS imageUrl,
            COUNT(DISTINCT cm.user_id) AS memberCount
        FROM clubs c
        JOIN users u
          ON u.id = :userId
         AND c.region_do = u.region_do
         AND c.region_si = u.region_si
        LEFT JOIN club_members cm
          ON cm.club_id = c.id
         AND cm.status = 'ACTIVE'
        GROUP BY c.id, c.name, c.description, c.region_do, c.region_si, c.image_url
        ORDER BY memberCount DESC, c.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<PopularClubRow> findPopularClubs(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT
            c.id          AS clubId,
            c.name        AS name,
            c.description AS description,
            c.region_do   AS regionDo,
            c.region_si   AS regionSi,
            c.image_url   AS imageUrl,
            cal.id        AS calendarId,
            cal.end_date  AS latestEndDate,
            SUM(cp.is_attended)          AS attendedCount,
            COUNT(*)                      AS totalCount,
            ROUND(SUM(cp.is_attended) / COUNT(*) * 100, 1) AS attendanceRate
        FROM clubs c
        JOIN users u
          ON u.id = :userId
         AND c.region_do = u.region_do
         AND c.region_si = u.region_si
        JOIN (
            SELECT club_id, MAX(end_date) AS latest_end
            FROM calendars
            WHERE end_date <= NOW()
            GROUP BY club_id
        ) last_cal
          ON last_cal.club_id = c.id
        JOIN calendars cal
          ON cal.club_id = c.id
         AND cal.end_date = last_cal.latest_end
        JOIN calendar_participants cp
          ON cp.calendar_id = cal.id
        GROUP BY
            c.id, c.name, c.description,
            c.region_do, c.region_si, c.image_url,
            cal.id, cal.end_date
        ORDER BY attendanceRate DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ActiveClubRow> findActiveClubs(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );
}
