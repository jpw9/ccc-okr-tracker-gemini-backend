package com.ccc.okrtracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Boolean isActive = true;

    // --- Auditing ---

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    private String updatedBy;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    private String closedBy;
    private LocalDateTime closedDate;

    // Soft Delete Helper
    public void softDelete(String user) {
        this.isActive = false;
        this.closedBy = user;
        this.closedDate = LocalDateTime.now();
    }

    // Restore Helper
    public void restore() {
        this.isActive = true;
        this.closedBy = null;
        this.closedDate = null;
    }
}