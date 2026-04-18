package com.sliit.smartcampus.booking.waitlist.service;

import com.sliit.smartcampus.auth.UserPrincipal;
import com.sliit.smartcampus.booking.dto.BookingResponseDTO;
import com.sliit.smartcampus.booking.mapper.BookingMapper;
import com.sliit.smartcampus.booking.model.Booking;
import com.sliit.smartcampus.booking.model.BookingStatus;
import com.sliit.smartcampus.booking.repository.BookingRepository;
import com.sliit.smartcampus.booking.waitlist.dto.WaitlistRequestDTO;
import com.sliit.smartcampus.booking.waitlist.dto.WaitlistResponseDTO;
import com.sliit.smartcampus.booking.waitlist.exception.WaitlistDuplicateException;
import com.sliit.smartcampus.booking.waitlist.exception.WaitlistEntryNotFoundException;
import com.sliit.smartcampus.booking.waitlist.exception.WaitlistExpiredException;
import com.sliit.smartcampus.booking.waitlist.mapper.WaitlistMapper;
import com.sliit.smartcampus.booking.waitlist.model.WaitlistEntry;
import com.sliit.smartcampus.booking.waitlist.model.WaitlistStatus;
import com.sliit.smartcampus.booking.waitlist.repository.WaitlistRepository;
import com.sliit.smartcampus.notification.NotificationService;
import com.sliit.smartcampus.notification.NotificationType;
import com.sliit.smartcampus.notification.ReferenceType;
import com.sliit.smartcampus.resource.entity.Resource;
import com.sliit.smartcampus.resource.entity.ResourceStatus;
import com.sliit.smartcampus.resource.exception.ResourceNotFoundException;
import com.sliit.smartcampus.resource.repository.ResourceRepository;
import com.sliit.smartcampus.user.User;
import com.sliit.smartcampus.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WaitlistServiceImpl implements WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final WaitlistMapper waitlistMapper;
    private final NotificationService notificationService;

    // ── Join Waitlist ─────────────────────────────────────────────────────────

    @Override
    public WaitlistResponseDTO joinWaitlist(WaitlistRequestDTO dto, UserPrincipal currentUser) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        Resource resource = resourceRepository.findById(dto.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + dto.getResourceId()));

        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Cannot join waitlist for an inactive resource. Status: " + resource.getStatus());
        }

        // Reject if user already has an active booking for this slot
        List<com.sliit.smartcampus.booking.model.Booking> existingBookings =
                bookingRepository.findConflictingBookings(
                        dto.getResourceId(), dto.getDate(), dto.getStartTime(), dto.getEndTime());
        boolean userAlreadyBooked = existingBookings.stream()
                .anyMatch(b -> b.getUser().getId().equals(currentUser.getId()));
        if (userAlreadyBooked) {
            throw new WaitlistDuplicateException(
                    "You already have a PENDING or APPROVED booking for this time slot.");
        }

        // Reject if user already has an active waitlist entry for this exact slot
        boolean alreadyWaiting = waitlistRepository.existsActiveEntryForUserAndSlot(
                currentUser.getId(), dto.getResourceId(),
                dto.getDate(), dto.getStartTime(), dto.getEndTime());
        if (alreadyWaiting) {
            throw new WaitlistDuplicateException(
                    "You are already on the waitlist for this resource and time slot.");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));

        Integer maxPosition = waitlistRepository.findMaxPosition(
                dto.getResourceId(), dto.getDate(), dto.getStartTime(), dto.getEndTime());
        int nextPosition = (maxPosition == null ? 0 : maxPosition) + 1;

        WaitlistEntry entry = WaitlistEntry.builder()
                .resource(resource)
                .user(user)
                .userEmail(currentUser.getEmail())
                .userName(user.getName())
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .purpose(dto.getPurpose())
                .expectedAttendees(dto.getExpectedAttendees())
                .status(WaitlistStatus.WAITING)
                .position(nextPosition)
                .build();

        entry = waitlistRepository.save(entry);

        // Notify user they joined the waitlist
        notificationService.notify(
                user.getId(),
                NotificationType.BOOKING_REQUEST,
                "Joined Waitlist",
                "You have joined the waitlist for \"" + resource.getName() + "\" on " +
                        dto.getDate() + " (" + dto.getStartTime() + " – " + dto.getEndTime() +
                        "). Your position: #" + nextPosition + ".",
                entry.getId(),
                ReferenceType.BOOKING
        );

        log.info("User {} joined waitlist for resource {} on {} at position {}",
                user.getEmail(), resource.getName(), dto.getDate(), nextPosition);

        return waitlistMapper.toDTO(entry);
    }

    // ── Read Operations ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistResponseDTO> getMyWaitlist(UserPrincipal currentUser) {
        return waitlistRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(waitlistMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WaitlistResponseDTO getWaitlistById(UUID id, UserPrincipal currentUser) {
        WaitlistEntry entry = findEntryOrThrow(id);

        if (!isAdmin(currentUser) && !entry.getUser().getId().equals(currentUser.getId())) {
            throw new com.sliit.smartcampus.booking.exception.UnauthorizedBookingActionException(
                    "You are not authorized to view this waitlist entry.");
        }

        return waitlistMapper.toDTO(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistResponseDTO> getAllWaitlist(WaitlistStatus status, UUID resourceId, LocalDate date) {
        return waitlistRepository.findAllWithFilters(status, resourceId, date)
                .stream()
                .map(waitlistMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ── Remove from Waitlist ──────────────────────────────────────────────────

    @Override
    public void removeFromWaitlist(UUID id, UserPrincipal currentUser) {
        WaitlistEntry entry = findEntryOrThrow(id);

        if (!entry.getUser().getId().equals(currentUser.getId())) {
            throw new com.sliit.smartcampus.booking.exception.UnauthorizedBookingActionException(
                    "You can only remove yourself from the waitlist.");
        }

        if (entry.getStatus() != WaitlistStatus.WAITING && entry.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalArgumentException(
                    "Cannot remove a waitlist entry with status: " + entry.getStatus());
        }

        int removedPosition = entry.getPosition();
        UUID resourceId = entry.getResource().getId();
        LocalDate date = entry.getDate();
        LocalTime startTime = entry.getStartTime();
        LocalTime endTime = entry.getEndTime();

        entry.setStatus(WaitlistStatus.REMOVED);
        waitlistRepository.save(entry);

        // Shift positions of all WAITING entries that were behind this one
        List<WaitlistEntry> toShift = waitlistRepository.findWaitingEntriesAfterPosition(
                resourceId, date, startTime, endTime, removedPosition);
        toShift.forEach(e -> e.setPosition(e.getPosition() - 1));
        waitlistRepository.saveAll(toShift);

        log.info("User {} removed from waitlist entry {}", currentUser.getEmail(), id);
    }

    // ── Confirm Waitlist Slot ─────────────────────────────────────────────────

    @Override
    public BookingResponseDTO confirmWaitlistSlot(UUID id, UserPrincipal currentUser) {
        WaitlistEntry entry = findEntryOrThrow(id);

        if (!entry.getUser().getId().equals(currentUser.getId())) {
            throw new com.sliit.smartcampus.booking.exception.UnauthorizedBookingActionException(
                    "You can only confirm your own waitlist entry.");
        }

        if (entry.getStatus() == WaitlistStatus.EXPIRED) {
            throw new WaitlistExpiredException(
                    "This waitlist slot has expired. You did not confirm within 24 hours.");
        }

        if (entry.getStatus() == WaitlistStatus.CONFIRMED) {
            throw new WaitlistExpiredException(
                    "This waitlist slot has already been confirmed.");
        }

        if (entry.getStatus() != WaitlistStatus.NOTIFIED) {
            throw new IllegalArgumentException(
                    "Only NOTIFIED waitlist entries can be confirmed. Current status: " + entry.getStatus());
        }

        if (entry.getExpiresAt() != null && LocalDateTime.now().isAfter(entry.getExpiresAt())) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
            notifyNextInWaitlist(entry.getResource().getId(), entry.getDate(),
                    entry.getStartTime(), entry.getEndTime());
            throw new WaitlistExpiredException(
                    "Your waitlist slot has expired. The next person in queue has been notified.");
        }

        // Create a real booking (PENDING — still needs admin approval)
        Resource resource = entry.getResource();
        User user = entry.getUser();

        Booking booking = Booking.builder()
                .resource(resource)
                .user(user)
                .userEmail(entry.getUserEmail())
                .userName(entry.getUserName())
                .date(entry.getDate())
                .startTime(entry.getStartTime())
                .endTime(entry.getEndTime())
                .purpose(entry.getPurpose())
                .expectedAttendees(entry.getExpectedAttendees())
                .status(BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);

        // Mark waitlist entry as CONFIRMED
        entry.setStatus(WaitlistStatus.CONFIRMED);
        waitlistRepository.save(entry);

        // Notify user their booking is pending approval
        notificationService.notify(
                user.getId(),
                NotificationType.BOOKING_REQUEST,
                "Booking Created from Waitlist",
                "Your waitlist confirmation for \"" + resource.getName() + "\" on " +
                        entry.getDate() + " has created a booking request. It is now PENDING admin approval.",
                booking.getId(),
                ReferenceType.BOOKING
        );

        // Notify admins about the new booking
        notificationService.notifyAllAdmins(
                NotificationType.BOOKING_REQUEST,
                "New Booking Request (from Waitlist)",
                user.getName() + " confirmed their waitlist slot and created a booking for \"" +
                        resource.getName() + "\" on " + entry.getDate() + " (" +
                        entry.getStartTime() + " – " + entry.getEndTime() + ").",
                booking.getId(),
                ReferenceType.BOOKING
        );

        log.info("User {} confirmed waitlist slot {}; booking {} created",
                user.getEmail(), id, booking.getId());

        return bookingMapper.toDTO(booking);
    }

    // ── Notify Next In Queue ──────────────────────────────────────────────────

    @Override
    public void notifyNextInWaitlist(UUID resourceId, LocalDate date,
                                     LocalTime startTime, LocalTime endTime) {
        waitlistRepository.findFirstWaiting(resourceId, date, startTime, endTime)
                .ifPresent(entry -> {
                    entry.setStatus(WaitlistStatus.NOTIFIED);
                    entry.setNotifiedAt(LocalDateTime.now());
                    entry.setExpiresAt(LocalDateTime.now().plusHours(24));
                    waitlistRepository.save(entry);

                    String resourceName = entry.getResource().getName();

                    notificationService.notify(
                            entry.getUser().getId(),
                            NotificationType.BOOKING_APPROVED,
                            "Waitlist Slot Available!",
                            "A slot opened up for \"" + resourceName + "\" on " + date +
                                    " at " + startTime + " – " + endTime +
                                    "! You have 24 hours to confirm your booking.",
                            entry.getId(),
                            ReferenceType.BOOKING
                    );

                    log.info("Notified waitlist user {} for resource {} on {}",
                            entry.getUserEmail(), resourceName, date);
                });
    }

    // ── Badge Count ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public long getActiveCountForUser(UUID userId) {
        return waitlistRepository.countActiveForUser(userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WaitlistEntry findEntryOrThrow(UUID id) {
        return waitlistRepository.findById(id)
                .orElseThrow(() -> new WaitlistEntryNotFoundException(
                        "Waitlist entry not found with id: " + id));
    }

    private boolean isAdmin(UserPrincipal user) {
        return "ADMIN".equals(user.getRole());
    }
}
