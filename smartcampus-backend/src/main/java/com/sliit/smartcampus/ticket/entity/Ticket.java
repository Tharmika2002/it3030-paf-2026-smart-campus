package com.sliit.smartcampus.ticket.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    private String category;
    private String priority;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String location;

    private String contactDetails;

    private String reportedBy;
    private String assignedTo;

    private String resolutionNote;

    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.status = "OPEN";
    }
}