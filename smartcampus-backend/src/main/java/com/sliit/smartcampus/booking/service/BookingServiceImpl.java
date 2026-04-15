package com.sliit.smartcampus.booking.service;

import com.sliit.smartcampus.auth.UserPrincipal;
import com.sliit.smartcampus.booking.dto.BookingRequestDTO;
import com.sliit.smartcampus.booking.dto.BookingResponseDTO;
import com.sliit.smartcampus.booking.exception.BookingConflictException;
import com.sliit.smartcampus.booking.exception.BookingNotFoundException;
import com.sliit.smartcampus.booking.exception.UnauthorizedBookingActionException;
import com.sliit.smartcampus.booking.mapper.BookingMapper;
import com.sliit.smartcampus.booking.model.Booking;
import com.sliit.smartcampus.booking.model.BookingStatus;
import com.sliit.smartcampus.booking.repository.BookingRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO dto, UserPrincipal currentUser) {
        // Validate time range
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        // Validate resource exists and is ACTIVE
        Resource resource = resourceRepository.findById(dto.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with id: " + dto.getResourceId()));

        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Resource is not available for booking. Current status: " + resource.getStatus());
        }

        // Check for scheduling conflicts
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                dto.getResourceId(), dto.getDate(), dto.getStartTime(), dto.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new BookingConflictException(
                    "The selected time slot conflicts with an existing booking for this resource.");
        }

        // Fetch the User entity
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));

        // Build and persist the booking
        Booking booking = Booking.builder()
                .resource(resource)
                .user(user)
                .userEmail(currentUser.getEmail())
                .userName(user.getName())
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .purpose(dto.getPurpose())
                .expectedAttendees(dto.getExpectedAttendees())
                .status(BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);

        // Notify all admins about the new booking request
        notificationService.notifyAllAdmins(
                NotificationType.BOOKING_REQUEST,
                "New Booking Request",
                user.getName() + " has requested \"" + resource.getName() + "\" on " +
                        dto.getDate() + " from " + dto.getStartTime() + " to " + dto.getEndTime() + ".",
                booking.getId(),
                ReferenceType.BOOKING
        );

        return bookingMapper.toDTO(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(UUID id, UserPrincipal currentUser) {
        Booking booking = findBookingOrThrow(id);

        if (!isAdmin(currentUser) && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedBookingActionException(
                    "You are not authorized to view this booking.");
        }

        return bookingMapper.toDTO(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getMyBookings(UserPrincipal currentUser) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(bookingMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getAllBookings(BookingStatus status, UUID resourceId,
                                                   LocalDate date, UUID userId) {
        return bookingRepository.findAllWithFilters(status, resourceId, date, userId)
                .stream()
                .map(bookingMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponseDTO approveBooking(UUID id, UserPrincipal currentUser) {
        Booking booking = findBookingOrThrow(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING bookings can be approved. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.APPROVED);
        booking = bookingRepository.save(booking);

        // Notify the booking owner
        notificationService.notify(
                booking.getUser().getId(),
                NotificationType.BOOKING_APPROVED,
                "Booking Approved",
                "Your booking for \"" + booking.getResource().getName() + "\" on " +
                        booking.getDate() + " (" + booking.getStartTime() + " – " +
                        booking.getEndTime() + ") has been approved.",
                booking.getId(),
                ReferenceType.BOOKING
        );

        return bookingMapper.toDTO(booking);
    }

    @Override
    public BookingResponseDTO rejectBooking(UUID id, String reason, UserPrincipal currentUser) {
        Booking booking = findBookingOrThrow(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING bookings can be rejected. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(reason);
        booking = bookingRepository.save(booking);

        String reasonText = (reason != null && !reason.isBlank()) ? " Reason: " + reason : "";
        notificationService.notify(
                booking.getUser().getId(),
                NotificationType.BOOKING_REJECTED,
                "Booking Rejected",
                "Your booking for \"" + booking.getResource().getName() + "\" on " +
                        booking.getDate() + " has been rejected." + reasonText,
                booking.getId(),
                ReferenceType.BOOKING
        );

        return bookingMapper.toDTO(booking);
    }

    @Override
    public BookingResponseDTO cancelBooking(UUID id, String reason, UserPrincipal currentUser) {
        Booking booking = findBookingOrThrow(id);

        // Only the booking owner can cancel
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedBookingActionException("You can only cancel your own bookings.");
        }

        if (booking.getStatus() == BookingStatus.REJECTED ||
                booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot cancel a booking with status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking = bookingRepository.save(booking);

        // Notify all admins about the cancellation
        notificationService.notifyAllAdmins(
                NotificationType.BOOKING_CANCELLED,
                "Booking Cancelled",
                booking.getUserName() + " cancelled their booking for \"" +
                        booking.getResource().getName() + "\" on " + booking.getDate() + ".",
                booking.getId(),
                ReferenceType.BOOKING
        );

        return bookingMapper.toDTO(booking);
    }

    @Override
    public void deleteBooking(UUID id, UserPrincipal currentUser) {
        Booking booking = findBookingOrThrow(id);

        if (!isAdmin(currentUser) && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedBookingActionException(
                    "You are not authorized to delete this booking.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING bookings can be deleted.");
        }

        bookingRepository.delete(booking);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Booking findBookingOrThrow(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + id));
    }

    private boolean isAdmin(UserPrincipal user) {
        return "ADMIN".equals(user.getRole());
    }
}
