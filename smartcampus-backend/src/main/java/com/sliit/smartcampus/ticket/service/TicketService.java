package com.sliit.smartcampus.ticket.service;

import com.sliit.smartcampus.auth.UserPrincipal;
import com.sliit.smartcampus.ticket.entity.Ticket;
import com.sliit.smartcampus.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    // GET CURRENT USER OBJECT
    private UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
            throw new RuntimeException("Unauthorized user");
        }

        return user;
    }

    // GET CURRENT USER ID
    private String getCurrentUserId() {
        return getCurrentUser().getId().toString();
    }

    // CREATE
    public Ticket createTicket(Ticket ticket) {

        ticket.setReportedBy(getCurrentUserId());
        ticket.setStatus("OPEN");

        return ticketRepository.save(ticket);
    }

    // GET MY TICKETS (WITH SORT)
    public List<Ticket> getMyTickets() {
        return ticketRepository.findByReportedBy(
                getCurrentUserId(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    // ADMIN
    public List<Ticket> getAllTickets() {

        UserPrincipal user = getCurrentUser();

        if (!user.getRole().equals("ADMIN") && !user.getRole().equals("TECHNICIAN")) {
            throw new RuntimeException("Access denied");
        }

        return ticketRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
    // GET BY ID (ONLY OWNER)
    public Ticket getTicketById(String id) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getReportedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Access denied");
        }

        return ticket;
    }

    // DELETE (ONLY OWNER)
    public void deleteTicket(String id) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getReportedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("You can only delete your own tickets");
        }

        ticketRepository.deleteById(id);
    }

    // UPDATE STATUS (OWNER + TECHNICIAN)
    public Ticket updateStatus(String ticketId, String newStatus, String resolutionNote) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (newStatus.equals("REJECTED")) {
            if (resolutionNote == null || resolutionNote.isEmpty()) {
                throw new RuntimeException("Rejection reason is required");
            }

            ticket.setStatus("REJECTED");
            ticket.setResolutionNote(resolutionNote);

            return ticketRepository.save(ticket);
        }

        UserPrincipal user = getCurrentUser();

        boolean isOwner = ticket.getReportedBy().equals(user.getId().toString());
        boolean isTechnician = user.getRole().equals("TECHNICIAN");

        // only assigned technician
        boolean isAssignedTech = user.getId().toString().equals(ticket.getAssignedTo());

        if (!isOwner && !isTechnician && !isAssignedTech) {
            throw new RuntimeException("Access denied");
        }

        String currentStatus = ticket.getStatus();

        // WORKFLOW VALIDATION
        if (currentStatus.equals("OPEN") && !newStatus.equals("IN_PROGRESS")) {
            throw new RuntimeException("OPEN → IN_PROGRESS only");
        }

        if (currentStatus.equals("IN_PROGRESS") && !newStatus.equals("RESOLVED")) {
            throw new RuntimeException("IN_PROGRESS → RESOLVED only");
        }

        if (currentStatus.equals("RESOLVED") && !newStatus.equals("CLOSED")) {
            throw new RuntimeException("RESOLVED → CLOSED only");
        }

        // RESOLUTION NOTE REQUIRED
        if (newStatus.equals("RESOLVED") || newStatus.equals("CLOSED")) {
            if (resolutionNote == null || resolutionNote.isEmpty()) {
                throw new RuntimeException("Resolution note is required");
            }
            ticket.setResolutionNote(resolutionNote);
        }

        ticket.setStatus(newStatus);

        return ticketRepository.save(ticket);
    }

    // ASSIGN TECHNICIAN (OWNER OR ADMIN)
    public Ticket assignTechnician(String ticketId, String technicianId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        UserPrincipal user = getCurrentUser();

        boolean isOwner = ticket.getReportedBy().equals(user.getId().toString());
        boolean isAdmin = user.getRole().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Only owner/admin can assign technician");
        }

        ticket.setAssignedTo(technicianId);

        return ticketRepository.save(ticket);
    }
}