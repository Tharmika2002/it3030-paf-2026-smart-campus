package com.sliit.smartcampus.ticket.repository;

import com.sliit.smartcampus.ticket.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, String> {

    // ALL COMMENTS (Admin / Technician)
    List<TicketComment> findByTicket_IdOrderByCreatedAtAsc(String ticketId);

    // ONLY PUBLIC COMMENTS (User)
    List<TicketComment> findByTicket_IdAndIsInternalFalseOrderByCreatedAtAsc(String ticketId);
}