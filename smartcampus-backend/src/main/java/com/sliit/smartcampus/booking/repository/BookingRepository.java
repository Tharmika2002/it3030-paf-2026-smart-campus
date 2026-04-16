package com.sliit.smartcampus.booking.repository;

import com.sliit.smartcampus.booking.model.Booking;
import com.sliit.smartcampus.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Finds bookings that overlap with the given time window for a resource on a date.
     * Only checks PENDING and APPROVED bookings (REJECTED/CANCELLED don't block slots).
     * Overlap condition: existingStart < newEnd AND existingEnd > newStart
     */
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.date = :date " +
           "AND b.status IN ('PENDING', 'APPROVED') " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findConflictingBookings(
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /** Returns the current user's bookings ordered newest first. */
    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Admin filter query – any null parameter is ignored (treated as "match all").
     */
    @Query("SELECT b FROM Booking b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:resourceId IS NULL OR b.resource.id = :resourceId) AND " +
           "(:date IS NULL OR b.date = :date) AND " +
           "(:userId IS NULL OR b.user.id = :userId) " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findAllWithFilters(
            @Param("status") BookingStatus status,
            @Param("resourceId") UUID resourceId,
            @Param("date") LocalDate date,
            @Param("userId") UUID userId
    );
}
