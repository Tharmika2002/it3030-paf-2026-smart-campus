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

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
            throw new RuntimeException("Unauthorized user");
        }

        return user.getId().toString();
    }

    // ADD COMMENT
    public TicketComment addComment(String ticketId, TicketComment comment) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getReportedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Access denied");
        }

        comment.setAuthorId(getCurrentUserId());
        comment.setTicket(ticket);

        return commentRepository.save(comment);
    }

    // GET COMMENTS
    public List<TicketComment> getComments(String ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getReportedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Access denied");
        }

        return commentRepository.findByTicketId(ticketId);
    }

    // UPDATE
    public TicketComment updateComment(String id, String content) {

        TicketComment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(getCurrentUserId())) {
            throw new RuntimeException("You can only edit your own comments");
        }

        comment.setContent(content);

        return commentRepository.save(comment);
    }

    // DELETE
    public void deleteComment(String id) {

        TicketComment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthorId().equals(getCurrentUserId())) {
            throw new RuntimeException("You can only delete your own comments");
        }

        commentRepository.deleteById(id);
    }
}