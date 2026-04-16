package com.sliit.smartcampus.ticket.repository;

import com.sliit.smartcampus.ticket.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, String> {

    List<TicketComment> findByTicketId(String ticketId);

}