package com.sliit.smartcampus.ticket.controller;

import com.sliit.smartcampus.ticket.dto.AssignRequest;
import com.sliit.smartcampus.ticket.dto.StatusUpdateRequest;
import com.sliit.smartcampus.ticket.entity.Ticket;
import com.sliit.smartcampus.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    // CREATE
    @PostMapping
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody Ticket ticket) {
        return ResponseEntity.status(201).body(ticketService.createTicket(ticket));
    }

    // GET MY TICKETS
    @GetMapping("/my")
    public ResponseEntity<List<Ticket>> getMyTickets() {
        return ResponseEntity.ok(ticketService.getMyTickets());
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    // UPDATE STATUS
    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody StatusUpdateRequest request) {

        return ResponseEntity.ok(
                ticketService.updateStatus(
                        ticketId,
                        request.getStatus(),
                        request.getResolutionNote()
                )
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable String id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    // ASSIGN
    @PatchMapping("/{ticketId}/assign")
    public ResponseEntity<Ticket> assignTechnician(
            @PathVariable String ticketId,
            @Valid @RequestBody AssignRequest request) {

        return ResponseEntity.ok(
                ticketService.assignTechnician(ticketId, request.getTechnicianId())
        );
    }
}