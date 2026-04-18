package com.sliit.smartcampus.ticket.repository;

import com.sliit.smartcampus.ticket.entity.Ticket;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    List<Ticket> findByReportedBy(String userId, Sort sort);

    List<Ticket> findByAssignedTo(String technicianId, Sort sort);
}