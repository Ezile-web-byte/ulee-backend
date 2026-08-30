package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A single complaint filed against a property. Multiple Reports can exist
 * per property — together they form the "Report Timeline" shown on the
 * admin's Handle Reported Property page.
 *
 * The student who filed the report IS stored (studentID), because admins
 * need to be able to investigate. It is NEVER included in anything sent to
 * the landlord — see AdminController#warnLandlordForListing, which builds
 * the warning email from getReason()/getDescription() only.
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reportID;

    private Integer propertyID;

    // Nullable: a report can be filed by a student, or logged by staff/an
    // inspector (e.g. "Report filed by Staff ID: 22766773" in the mockup).
    private Integer studentID;
    private Integer staffID;

    private String reason; // short category, e.g. "Maintenance", "Safety"

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime reportedAt;

    public Integer getReportID() { return reportID; }
    public void setReportID(Integer reportID) { this.reportID = reportID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public Integer getStudentID() { return studentID; }
    public void setStudentID(Integer studentID) { this.studentID = studentID; }

    public Integer getStaffID() { return staffID; }
    public void setStaffID(Integer staffID) { this.staffID = staffID; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    /** e.g. "Student ID: 21804423" or "Staff ID: 22766773" — for the timeline label. */
    @Transient
    public String getFiledByLabel() {
        if (studentID != null) return "Student ID: " + studentID;
        if (staffID != null) return "Staff ID: " + staffID;
        return "Anonymous";
    }
}