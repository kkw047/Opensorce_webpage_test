package com.cbnu11team.team11.repository;

import com.cbnu11team.team11.domain.CalendarParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CalendarParticipantRepository extends JpaRepository<CalendarParticipant, Long> {
    Optional<CalendarParticipant> findByCalendarIdAndUserId(Long calendarId, Long userId);
}