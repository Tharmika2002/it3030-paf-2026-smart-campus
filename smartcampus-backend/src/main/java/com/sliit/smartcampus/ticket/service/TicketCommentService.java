package com.sliit.smartcampus.ticket.service;

import com.sliit.smartcampus.auth.UserPrincipal;
import com.sliit.smartcampus.notification.NotificationService;
import com.sliit.smartcampus.notification.NotificationType;
import com.sliit.smartcampus.notification.ReferenceType;
import com.sliit.smartcampus.ticket.entity.Ticket;
import com.sliit.smartcampus.ticket.entity.TicketComment;
import com.sliit.smartcampus.ticket.repository.TicketCommentRepository;
import com.sliit.smartcampus.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class TicketCommentService {

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private NotificationService notificationService;

    // GET CURRENT USER
    private UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
            throw new RuntimeException("Unauthorized user");
        }

        return user;
    }

    private String getCurrentUserId() {
        return getCurrentUser().getId().toString();
    }

    // ADD COMMENT
    public TicketComment addComment(String ticketId, TicketComment comment) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getStatus().equals("REJECTED") || ticket.getStatus().equals("CLOSED")) {
            throw new RuntimeException("Cannot add comment to closed/rejected ticket");
        }

        UserPrincipal user = getCurrentUser();

        boolean isOwner = ticket.getReportedBy().equals(user.getId().toString());
        boolean isAssigned = ticket.getAssignedTo() != null &&
                ticket.getAssignedTo().equals(user.getId().toString());
        boolean isAdmin = user.getRole().equals("ADMIN");

        if (!isOwner && !isAssigned && !isAdmin) {
            throw new RuntimeException("Access denied");
        }

        //  INTERNAL COMMENT CHECK
        if (comment.isInternal()) {
            if (!(isAdmin || isAssigned)) {
                throw new RuntimeException("Only admin/technician can add internal comments");
            }
        }

        //  SET AUTHOR DETAILS (VERY IMPORTANT)
        comment.setAuthorId(user.getId().toString());
        comment.setAuthorName(user.getName());
        comment.setAuthorRole(user.getRole());

        comment.setTicket(ticket);

        TicketComment saved = commentRepository.save(comment);

        // NOTIFY OWNER
        notificationService.notify(
                UUID.fromString(ticket.getReportedBy()),
                NotificationType.TICKET_COMMENT_ADDED,
                "New Comment on Ticket",
                "A new comment has been added.\n\n" +
                        "Title: " + ticket.getTitle() +
                        "\nBy: " + user.getRole() +
                        "\nComment: " + saved.getContent(),
                UUID.fromString(ticket.getId()),
                ReferenceType.TICKET
        );

        // NOTIFY TECHNICIAN
        if (ticket.getAssignedTo() != null) {
            notificationService.notify(
                    UUID.fromString(ticket.getAssignedTo()),
                    NotificationType.TICKET_COMMENT_ADDED,
                    "New Comment",
                    "New comment added to assigned ticket",
                    UUID.fromString(ticket.getId()),
                    ReferenceType.TICKET
            );
        }

        return saved;
    }

    // GET COMMENTS
    public List<TicketComment> getComments(String ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UserPrincipal user = getCurrentUser();

        boolean isOwner = ticket.getReportedBy().equals(user.getId().toString());

        boolean isAssigned = ticket.getAssignedTo() != null &&
                ticket.getAssignedTo().equals(user.getId().toString());

        boolean isAdmin = user.getRole().equals("ADMIN");

        // ACCESS CONTROL
        if (!isOwner && !isAssigned && !isAdmin) {
            throw new RuntimeException("Access denied");
        }

        // USER → only public comments
        if (isOwner) {
            return commentRepository
                    .findByTicket_IdAndIsInternalFalseOrderByCreatedAtAsc(ticketId);
        }

        // ADMIN / TECH → all comments
        return commentRepository
                .findByTicket_IdOrderByCreatedAtAsc(ticketId);
    }

    // UPDATE COMMENT
    public TicketComment updateComment(String id, String content) {

        TicketComment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(getCurrentUserId())) {
            throw new RuntimeException("You can only edit your own comments");
        }

        comment.setContent(content);

        return commentRepository.save(comment);
    }

    // DELETE COMMENT
    public void deleteComment(String id) {

        TicketComment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(getCurrentUserId())) {
            throw new RuntimeException("You can only delete your own comments");
        }

        commentRepository.deleteById(id);
    }
}