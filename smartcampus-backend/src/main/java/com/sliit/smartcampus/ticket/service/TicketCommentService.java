package com.sliit.smartcampus.ticket.service;

import com.sliit.smartcampus.auth.UserPrincipal;
import com.sliit.smartcampus.ticket.entity.Ticket;
import com.sliit.smartcampus.ticket.entity.TicketComment;
import com.sliit.smartcampus.ticket.repository.TicketCommentRepository;
import com.sliit.smartcampus.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class TicketCommentService {

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    // Get current user object
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

    // ADD COMMENT (Owner + Technician + Admin)
    public TicketComment addComment(String ticketId, TicketComment comment) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UserPrincipal user = getCurrentUser();

        boolean isOwner = ticket.getReportedBy().equals(user.getId().toString());
        boolean isTechnician = user.getRole().equals("TECHNICIAN");
        boolean isAdmin = user.getRole().equals("ADMIN");

        if (!isOwner && !isTechnician && !isAdmin) {
            throw new RuntimeException("Access denied");
        }

        comment.setAuthorId(getCurrentUserId());
        comment.setTicket(ticket);

        return commentRepository.save(comment);
    }

    // GET COMMENTS (Owner + Technician + Admin)
    public List<TicketComment> getComments(String ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UserPrincipal user = getCurrentUser();

        boolean isOwner = ticket.getReportedBy().equals(user.getId().toString());
        boolean isTechnician = user.getRole().equals("TECHNICIAN");
        boolean isAdmin = user.getRole().equals("ADMIN");

        if (!isOwner && !isTechnician && !isAdmin) {
            throw new RuntimeException("Access denied");
        }

        return commentRepository.findByTicketId(ticketId);
    }

    // UPDATE (ONLY AUTHOR)
    public TicketComment updateComment(String id, String content) {

        TicketComment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(getCurrentUserId())) {
            throw new RuntimeException("You can only edit your own comments");
        }

        comment.setContent(content);

        return commentRepository.save(comment);
    }

    // DELETE (ONLY AUTHOR)
    public void deleteComment(String id) {

        TicketComment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(getCurrentUserId())) {
            throw new RuntimeException("You can only delete your own comments");
        }

        commentRepository.deleteById(id);
    }
}