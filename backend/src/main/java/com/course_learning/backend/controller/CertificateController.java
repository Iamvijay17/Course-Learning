package com.course_learning.backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.course_learning.backend.model.Certificate;
import com.course_learning.backend.service.CertificateService;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;

    @GetMapping("/user")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<Certificate>> getUserCertificates(Authentication authentication) {
        String userId = authentication.getName(); // Assuming username is userId

        // For now, return empty list - you might want to implement this based on your user service
        // This would require getting certificates by user ID
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Certificate> getCertificateByEnrollment(@PathVariable String enrollmentId, Authentication authentication) {
        Optional<Certificate> certificate = certificateService.getCertificateByEnrollment(enrollmentId);

        if (!certificate.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // TODO: Add authorization check to ensure user can access this certificate
        // For now, allowing access - you should implement proper authorization

        return ResponseEntity.ok(certificate.get());
    }

    @GetMapping("/download/{certificateId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String certificateId, Authentication authentication) {
        try {
            Optional<Certificate> certificateOpt = certificateService.getCertificateById(certificateId);
            if (!certificateOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Certificate certificate = certificateOpt.get();

            // TODO: Add authorization check to ensure user can download this certificate
            // Check if the authenticated user is the certificate owner or an admin

            byte[] pdfBytes = certificateService.getCertificateFile(certificateId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", certificate.getFileName());
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/view/{certificateId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> viewCertificate(@PathVariable String certificateId, Authentication authentication) {
        try {
            Optional<Certificate> certificateOpt = certificateService.getCertificateById(certificateId);
            if (!certificateOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Certificate certificate = certificateOpt.get();

            // TODO: Add authorization check to ensure user can view this certificate

            byte[] pdfBytes = certificateService.getCertificateFile(certificateId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", certificate.getFileName());
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{certificateId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Certificate> getCertificate(@PathVariable String certificateId, Authentication authentication) {
        Optional<Certificate> certificate = certificateService.getCertificateById(certificateId);

        if (!certificate.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // TODO: Add authorization check to ensure user can access this certificate

        return ResponseEntity.ok(certificate.get());
    }
}
