package com.sliit.smartcampus.ticket.controller;

import com.sliit.smartcampus.ticket.entity.TicketAttachment;
import com.sliit.smartcampus.ticket.service.TicketAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketAttachmentController {

    @Autowired
    private TicketAttachmentService attachmentService;

    // UPLOAD
    @PostMapping("/{ticketId}/attachments")
    public ResponseEntity<TicketAttachment> uploadAttachment(
            @PathVariable String ticketId,
            @RequestParam("file") MultipartFile file) throws Exception {

        return ResponseEntity.status(201)
                .body(attachmentService.uploadFile(ticketId, file));
    }

    // DOWNLOAD
    @GetMapping("/attachments/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) throws Exception {

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File file = new File(uploadDir + fileName);

        if (!file.exists()) {
            throw new RuntimeException("File not found");
        }

        Resource resource = new UrlResource(file.toURI());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .body(resource);
    }

    // DELETE
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable String attachmentId) {

        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

    // GET ALL
    @GetMapping("/{ticketId}/attachments")
    public ResponseEntity<List<TicketAttachment>> getAttachments(@PathVariable String ticketId) {

        return ResponseEntity.ok(
                attachmentService.getAttachmentsByTicket(ticketId)
        );
    }
}