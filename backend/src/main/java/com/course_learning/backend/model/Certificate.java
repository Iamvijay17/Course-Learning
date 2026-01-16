package com.course_learning.backend.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "certificates")
public class Certificate {
    @Id
    private String certificateId;

    @ManyToOne
    @JoinColumn(name = "enrollmentId", nullable = false)
    private Enrollment enrollment;

    private String certificateNumber; // Unique certificate number
    private String certificateUrl; // URL to download/view certificate
    private String fileName; // Name of the certificate file
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Certificate() {
    }

    public Certificate(Enrollment enrollment) {
        this.enrollment = enrollment;
        this.issuedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.certificateNumber = generateCertificateNumber();
    }

    @PrePersist
    public void generateCertificateId() {
        if (this.certificateId == null) {
            this.certificateId = "CERT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
    }

    private String generateCertificateNumber() {
        // Generate a unique certificate number like "CERT-2026-001234"
        String year = String.valueOf(LocalDateTime.now().getYear());
        String random = String.valueOf((int)(Math.random() * 900000) + 100000);
        return "CERT-" + year + "-" + random;
    }

    // Getters and Setters
    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
