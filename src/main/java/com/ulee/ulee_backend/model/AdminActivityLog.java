package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One row per admin action (approve/reject a listing, warn a landlord,
 * delete a user, etc.), written by logActivity() in AdminController.
 * Powers the Dashboard's "Recent Activity" feed and the full Activity Log
 * page. Not to be confused with AdminLoginActivity, which tracks login
 * attempts instead of admin actions.
 */
@Entity
@Table(name = "admin_activity_log")
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logID")
    private Integer id;

    /** Display name of whoever performed the action, e.g. "Unakho Gadavu". */
    private String actorName;

    /** Short action key, e.g. "Approved", "Rejected", "Suspended", "Warned landlord" — see AdminController.ActivityItem.getTone() for the full list this is matched against. */
    private String action;

    /** Full human-readable sentence shown in the feed, e.g. "Approved \"Hlomane Riverside House\"". */
    private String message;

    private LocalDateTime timestamp;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}