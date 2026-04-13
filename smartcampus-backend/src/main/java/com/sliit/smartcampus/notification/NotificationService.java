package com.sliit.smartcampus.notification;

import com.sliit.smartcampus.notification.dto.NotificationDTO;
import com.sliit.smartcampus.user.User;
import com.sliit.smartcampus.user.UserRepository;
import com.sliit.smartcampus.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Transactional
    public void notify(UUID userId, NotificationType type,
                       String title, String message,
                       UUID referenceId, ReferenceType referenceType) {

        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    toDTO(saved)
            );
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}: {}", userId, e.getMessage());
        }

        emailService.sendEmail(recipient.getEmail(), title, message);
    }

    @Transactional
    public void notifyAllAdmins(NotificationType type, String title,
                                String message, UUID referenceId,
                                ReferenceType referenceType) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notify(admin.getId(), type, title, message, referenceId, referenceType);
        }
    }

    public Page<NotificationDTO> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDTO markAsRead(Long notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!n.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        n.setRead(true);
        return toDTO(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void deleteNotification(Long notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!n.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        notificationRepository.delete(n);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}