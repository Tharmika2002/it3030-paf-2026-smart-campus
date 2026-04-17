package com.sliit.smartcampus.ticket.repository;

import com.sliit.smartcampus.ticket.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, String> {

    long countByTicketId(String ticketId);

    List<TicketAttachment> findByTicketId(String ticketId);

}