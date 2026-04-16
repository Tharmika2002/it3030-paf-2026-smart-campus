package com.sliit.smartcampus.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String provider;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "bookingApproved",      column = @Column(name = "notif_booking_approved")),
            @AttributeOverride(name = "bookingRejected",      column = @Column(name = "notif_booking_rejected")),
            @AttributeOverride(name = "bookingRequest",       column = @Column(name = "notif_booking_request")),
            @AttributeOverride(name = "bookingCancelled",     column = @Column(name = "notif_booking_cancelled")),
            @AttributeOverride(name = "ticketStatusChanged",  column = @Column(name = "notif_ticket_status")),
            @AttributeOverride(name = "ticketAssigned",       column = @Column(name = "notif_ticket_assigned")),
            @AttributeOverride(name = "ticketCommentAdded",   column = @Column(name = "notif_ticket_comment")),
            @AttributeOverride(name = "resourceOutOfService", column = @Column(name = "notif_resource_oos"))
    })
    @Builder.Default
    private NotifPrefs notifPrefs = new NotifPrefs();

    @CreationTimestamp
    private LocalDateTime createdAt;
}