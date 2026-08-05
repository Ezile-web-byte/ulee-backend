package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_document")
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer documentID;

    private Integer applicationID;
    private String fileName;
    private String filePath;
    private LocalDateTime uploadedAt;

    public Integer getDocumentID() { return documentID; }
    public void setDocumentID(Integer documentID) { this.documentID = documentID; }

    public Integer getApplicationID() { return applicationID; }
    public void setApplicationID(Integer applicationID) { this.applicationID = applicationID; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
