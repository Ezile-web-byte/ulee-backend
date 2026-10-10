package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One row per deactivation of an account.
 *
 * state:  PENDING   - admin clicked Deactivate; waiting for the 10-minute notice to pass
 *         ACTIVE    - account is deactivated and its properties are hidden
 *         ENDED     - admin reactivated the account
 *         CANCELLED - admin stopped the deactivation before it happened
 * reason: ADMIN | WARNING_LIMIT (automatic, landlord went over the warning limit)
 *
 * hiddenPropertyIds remembers exactly which properties THIS deactivation hid,
 * so reactivating restores only those (not listings that were already hidden).
 */
@Entity
@Table(name = "account_suspension")
public class AccountSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suspensionID")
    private Integer suspensionID;

    @Column(name = "userID", nullable = false)
    private Integer userID;

    @Column(name = "state", nullable = false, length = 20)
    private String state;

    @Column(name = "reason", length = 30)
    private String reason;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "scheduledFor")
    private LocalDateTime scheduledFor;

    @Column(name = "appliedAt")
    private LocalDateTime appliedAt;

    @Column(name = "endedAt")
    private LocalDateTime endedAt;

    @Column(name = "hiddenPropertyIds", length = 2000)
    private String hiddenPropertyIds;

    public Integer getSuspensionID() { return suspensionID; }
    public void setSuspensionID(Integer suspensionID) { this.suspensionID = suspensionID; }
    public Integer getUserID() { return userID; }
    public void setUserID(Integer userID) { this.userID = userID; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public String getHiddenPropertyIds() { return hiddenPropertyIds; }
    public void setHiddenPropertyIds(String hiddenPropertyIds) { this.hiddenPropertyIds = hiddenPropertyIds; }
}