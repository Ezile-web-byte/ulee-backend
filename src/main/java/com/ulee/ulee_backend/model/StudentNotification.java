package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One notification for one student. Kept separate from the landlord
 * Notification entity so that table does not need to change.
 * (Use javax.persistence.* if your project is on Spring Boot 2.)
 */
@Entity
@Table(name = "studentnotifications")
public class StudentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationID;

    @Column(name = "studentID", nullable = false)
    private Integer studentID;

    // accepted | rejected | review | report | handled  (drives the icon in the bell)
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    // where clicking the notification goes, e.g. /my-applications or /property/12
    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "isRead", nullable = false)
    private Boolean isRead = false;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StudentNotification() {}

    public StudentNotification(Integer studentID, String type, String message, String link) {
        this.studentID = studentID;
        this.type = type;
        this.message = message;
        this.link = link;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getNotificationID() { return notificationID; }
    public Integer getStudentID() { return studentID; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}