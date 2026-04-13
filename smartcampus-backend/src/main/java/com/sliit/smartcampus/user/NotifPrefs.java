package com.sliit.smartcampus.user;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotifPrefs {
    private boolean bookingApproved = true;
    private boolean bookingRejected = true;
    private boolean bookingRequest = true;
    private boolean bookingCancelled = true;
    private boolean ticketStatusChanged = true;
    private boolean ticketAssigned = true;
    private boolean ticketCommentAdded = true;
    private boolean resourceOutOfService = true;
}