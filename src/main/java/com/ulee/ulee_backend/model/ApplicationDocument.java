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

    // The merged database ended up with BOTH camelCase (fileName/filePath/
    // uploadedAt) and snake_case (file_name/file_path/uploaded_at) columns.
    // The real data — and the NOT NULL constraint on fileName — live on the
    // camelCase columns, but Hibernate's default naming maps these fields to
    // the snake_case ones, so every insert left the NOT NULL fileName empty
    // and failed (the "couldn't save your documents" error). Pin the mapping
    // to the real columns so applications submit and existing docs still read.
    @Column(name = "fileName")
    private String fileName;
    @Column(name = "filePath")
    private String filePath;
    @Column(name = "uploadedAt")
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
