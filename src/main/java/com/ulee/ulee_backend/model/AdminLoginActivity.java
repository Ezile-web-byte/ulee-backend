package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One row per login attempt (success or failure), written by
 * roleBasedSuccessHandler() / the new failure handler in SecurityConfig.
 * userID is null for failed attempts where the email didn't match any
 * account — emailAttempted is kept either way so a failed attempt is still
 * visible in the log.
 */
@Entity
@Table(name = "admin_login_activity")
public class AdminLoginActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer userID;

    private String emailAttempted;

    private Boolean success;

    private String deviceLabel;

    private String ipAddress;

    private LocalDateTime timestamp;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserID() { return userID; }
    public void setUserID(Integer userID) { this.userID = userID; }

    public String getEmailAttempted() { return emailAttempted; }
    public void setEmailAttempted(String emailAttempted) { this.emailAttempted = emailAttempted; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}