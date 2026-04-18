package com.sliit.smartcampus.ticket.repository;

import com.sliit.smartcampus.ticket.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, String> {

    long countByTicket_Id(String ticketId); //

    List<TicketAttachment> findByTicket_Id(String ticketId); //
}