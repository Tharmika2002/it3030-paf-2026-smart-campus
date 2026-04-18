package com.sliit.smartcampus.booking.waitlist.repository;

import com.sliit.smartcampus.booking.waitlist.model.WaitlistEntry;
import com.sliit.smartcampus.booking.waitlist.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

    /** Find the first WAITING entry in queue for a given slot (lowest position). */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.resource.id = :resourceId " +
           "AND w.date = :date AND w.startTime = :startTime AND w.endTime = :endTime " +
           "AND w.status = 'WAITING' ORDER BY w.position ASC")
    Optional<WaitlistEntry> findFirstWaiting(
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /** Check if user already has a WAITING or NOTIFIED entry for the same slot. */
    @Query("SELECT COUNT(w) > 0 FROM WaitlistEntry w WHERE w.user.id = :userId " +
           "AND w.resource.id = :resourceId AND w.date = :date " +
           "AND w.startTime = :startTime AND w.endTime = :endTime " +
           "AND w.status IN ('WAITING', 'NOTIFIED')")
    boolean existsActiveEntryForUserAndSlot(
            @Param("userId") UUID userId,
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /** Get max position in queue for a slot (used to assign next position). */
    @Query("SELECT COALESCE(MAX(w.position), 0) FROM WaitlistEntry w " +
           "WHERE w.resource.id = :resourceId AND w.date = :date " +
           "AND w.startTime = :startTime AND w.endTime = :endTime " +
           "AND w.status IN ('WAITING', 'NOTIFIED')")
    Integer findMaxPosition(
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /** All WAITING entries with position > given value for a slot — used on removal to shift positions down. */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.resource.id = :resourceId " +
           "AND w.date = :date AND w.startTime = :startTime AND w.endTime = :endTime " +
           "AND w.status = 'WAITING' AND w.position > :position ORDER BY w.position ASC")
    List<WaitlistEntry> findWaitingEntriesAfterPosition(
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("position") Integer position
    );

    /** All NOTIFIED entries whose expiry time has passed. */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.status = 'NOTIFIED' AND w.expiresAt < :now")
    List<WaitlistEntry> findExpiredNotifications(@Param("now") LocalDateTime now);

    /** Logged-in user's waitlist entries ordered newest first. */
    List<WaitlistEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Admin filter query — any null parameter is ignored (match all). */
    @Query("SELECT w FROM WaitlistEntry w WHERE " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:resourceId IS NULL OR w.resource.id = :resourceId) AND " +
           "(:date IS NULL OR w.date = :date) " +
           "ORDER BY w.createdAt DESC")
    List<WaitlistEntry> findAllWithFilters(
            @Param("status") WaitlistStatus status,
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date
    );

    /** Count active (WAITING + NOTIFIED) entries for a user — used for sidebar badge. */
    @Query("SELECT COUNT(w) FROM WaitlistEntry w WHERE w.user.id = :userId " +
           "AND w.status IN ('WAITING', 'NOTIFIED')")
    long countActiveForUser(@Param("userId") UUID userId);
}
