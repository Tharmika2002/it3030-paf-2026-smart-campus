package com.sliit.smartcampus.ticket.service;

import com.sliit.smartcampus.auth.UserPrincipal;
import com.sliit.smartcampus.ticket.entity.Ticket;
import com.sliit.smartcampus.ticket.entity.TicketAttachment;
import com.sliit.smartcampus.ticket.repository.TicketAttachmentRepository;
import com.sliit.smartcampus.ticket.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class TicketAttachmentService {

    @Autowired
    private TicketAttachmentRepository attachmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/";

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
            throw new RuntimeException("Unauthorized user");
        }

        return user.getId().toString();
    }

    public TicketAttachment uploadFile(String ticketId, MultipartFile file) throws IOException {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getReportedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Access denied");
        }

        if (attachmentRepository.countByTicketId(ticketId) >= 3) {
            throw new RuntimeException("Max 3 files allowed");
        }

        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
            throw new RuntimeException("Only JPG/PNG allowed");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Max 5MB allowed");
        }

        String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        file.transferTo(new File(uploadDir + storedName));

        TicketAttachment attachment = new TicketAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setStoredName(storedName);
        attachment.setMimeType(contentType);
        attachment.setFileSize(file.getSize());
        attachment.setTicket(ticket);

        return attachmentRepository.save(attachment);
    }

    public void deleteAttachment(String attachmentId) {

        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        if (!attachment.getTicket().getReportedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Access denied");
        }

        File file = new File(uploadDir + attachment.getStoredName());
        if (file.exists()) file.delete();

        attachmentRepository.deleteById(attachmentId);
    }

    public List<TicketAttachment> getAttachmentsByTicket(String ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getReportedBy().equals(getCurrentUserId())) {
            throw new RuntimeException("Access denied");
        }

        return attachmentRepository.findByTicketId(ticketId);
    }
}