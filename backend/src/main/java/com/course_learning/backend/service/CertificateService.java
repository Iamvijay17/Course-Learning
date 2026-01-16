package com.course_learning.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.course_learning.backend.model.Certificate;
import com.course_learning.backend.model.Enrollment;
import com.course_learning.backend.repository.CertificateRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

@Service
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Value("${app.certificate.storage.path:certificates}")
    private String certificateStoragePath;

    @Transactional
    public Certificate generateCertificate(Enrollment enrollment) {
        // Check if certificate already exists
        Optional<Certificate> existingCertificate = certificateRepository.findByEnrollment_EnrollmentId(enrollment.getEnrollmentId());
        if (existingCertificate.isPresent()) {
            return existingCertificate.get();
        }

        // Generate PDF certificate
        byte[] pdfBytes = generateCertificatePDF(enrollment);

        // Save PDF to file system
        String fileName = generateFileName(enrollment);
        String filePath = saveCertificateToFile(pdfBytes, fileName);

        // Create certificate record
        Certificate certificate = new Certificate(enrollment);
        certificate.setFileName(fileName);
        certificate.setCertificateUrl("/api/certificates/download/" + certificate.getCertificateId());

        return certificateRepository.save(certificate);
    }

    public Optional<Certificate> getCertificateByEnrollment(String enrollmentId) {
        return certificateRepository.findByEnrollment_EnrollmentId(enrollmentId);
    }

    public Optional<Certificate> getCertificateById(String certificateId) {
        return certificateRepository.findById(certificateId);
    }

    public Optional<Certificate> getCertificateByUserAndCourse(String userId, String courseId) {
        return certificateRepository.findByUserIdAndCourseId(userId, courseId);
    }

    public byte[] getCertificateFile(String certificateId) throws IOException {
        Optional<Certificate> certificateOpt = certificateRepository.findById(certificateId);
        if (!certificateOpt.isPresent()) {
            throw new RuntimeException("CERTIFICATE_NOT_FOUND:Certificate not found");
        }

        Certificate certificate = certificateOpt.get();
        Path filePath = Paths.get(certificateStoragePath, certificate.getFileName());

        if (!Files.exists(filePath)) {
            throw new RuntimeException("CERTIFICATE_FILE_NOT_FOUND:Certificate file not found");
        }

        return Files.readAllBytes(filePath);
    }

    private byte[] generateCertificatePDF(Enrollment enrollment) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Certificate Title
            Paragraph title = new Paragraph("CERTIFICATE OF COMPLETION")
                    .setFontSize(36)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);

            // Subtitle
            Paragraph subtitle = new Paragraph("This is to certify that")
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subtitle);

            // Student Name
            String fullName = enrollment.getUser().getFirstName() + " " + enrollment.getUser().getLastName();
            Paragraph studentName = new Paragraph(fullName)
                    .setFontSize(28)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setMarginBottom(20);
            document.add(studentName);

            // Completion Text
            Paragraph completionText = new Paragraph("has successfully completed the course")
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(completionText);

            // Course Title
            Paragraph courseTitle = new Paragraph(enrollment.getCourse().getTitle())
                    .setFontSize(24)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setMarginBottom(30);
            document.add(courseTitle);

            // Certificate Details
            String certificateNumber = "CERT-" + LocalDateTime.now().getYear() + "-" + String.format("%06d", (int)(Math.random() * 1000000));

            Paragraph details = new Paragraph()
                    .add(new Text("Certificate Number: ").setBold())
                    .add(certificateNumber)
                    .add(new Text("\n\nCompletion Date: ").setBold())
                    .add(enrollment.getCompletedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                    .add(new Text("\n\nInstructor: ").setBold())
                    .add(enrollment.getCourse().getInstructorId()) // You might want to get instructor name
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(40);
            document.add(details);

            // Signature line
            Paragraph signature = new Paragraph("______________________________\nCourse Instructor")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(60);
            document.add(signature);

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("ERROR_GENERATING_CERTIFICATE:Failed to generate certificate PDF", e);
        }

        return outputStream.toByteArray();
    }

    private String generateFileName(Enrollment enrollment) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "certificate_" + enrollment.getEnrollmentId() + "_" + timestamp + ".pdf";
    }

    private String saveCertificateToFile(byte[] pdfBytes, String fileName) {
        try {
            Path storagePath = Paths.get(certificateStoragePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            Path filePath = storagePath.resolve(fileName);
            Files.write(filePath, pdfBytes);

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("ERROR_SAVING_CERTIFICATE:Failed to save certificate file", e);
        }
    }

    public boolean hasCertificate(String enrollmentId) {
        return certificateRepository.existsByEnrollment_EnrollmentId(enrollmentId);
    }
}
