package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * An in-app notification for a landlord OR a student. Used for official
 * warnings (issue #4/#6): instead of relying on an outbound email (which
 * needs SMTP config that this project doesn't have — see
 * application.properties), the admin's "Send Official Warning" / generic
 * "Warn" actions create one of these, and the recipient's dashboard shows it
 * in a notification bell/dropdown.
 *
 * landlordID and studentID are both nullable — exactly one is set depending
 * on which role the notification is for. This lets the same table and the
 * same landlord-facing bell/dropdown pattern extend to students without a
 * second, parallel table.
 *
 * Property-linked warning messages are built entirely from
 * Report.reason / Report.description — never studentID/staffID — so
 * complainers stay anonymous to the landlord.
 * See AdminController#warnLandlordForListing.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationID;

    private Integer landlordID;

    // Nullable — set instead of landlordID when this notification targets a
    // student (e.g. a generic account warning) rather than a landlord.
    private Integer studentID;

    // Nullable — not every notification has to be about a specific listing,
    // but official warnings always set this so the bell can deep-link back
    // to /admin's equivalent listing page for the landlord, if one exists.
    private Integer propertyID;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;

    private Boolean isRead = false;

    public Integer getNotificationID() { return notificationID; }
    public void setNotificationID(Integer notificationID) { this.notificationID = notificationID; }

    public Integer getLandlordID() { return landlordID; }
    public void setLandlordID(Integer landlordID) { this.landlordID = landlordID; }

    public Integer getStudentID() { return studentID; }
    public void setStudentID(Integer studentID) { this.studentID = studentID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
}