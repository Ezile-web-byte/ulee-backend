package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One row per login session (any role, not just admins — the Settings >
 * Active Sessions panel just happens to be the only UI that reads this so
 * far). Created by roleBasedSuccessHandler() in SecurityConfig when someone
 * logs in, updated on request activity, and marked inactive on logout
 * (LogoutHandler) or when the underlying HttpSession is destroyed/expires
 * (AdminSessionListener).
 *
 * NOTE: table/column names deliberately match this codebase's existing
 * convention of camelCase columns (userID, propertyID, etc. elsewhere) — if
 * your DB actually uses snake_case, adjust the CREATE TABLE script
 * (admin-sessions-login-activity.sql) and these @Column-less fields will
 * still map 1:1 as long as your Hibernate naming strategy is consistent
 * with the rest of the app.
 */
@Entity
@Table(name = "admin_sessions")
public class AdminSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer userID;

    /** Spring session id (HttpSession#getId()) — used to find/expire this row on logout. */
    @Column(unique = true, length = 100)
    private String sessionId;

    /** Human-readable "Chrome on Windows" style label, parsed from the User-Agent header. */
    private String deviceLabel;

    private String ipAddress;

    /** Best-effort city/country from the IP. Null/blank until a geolocation lookup is wired in. */
    private String location;

    private LocalDateTime loginAt;
    private LocalDateTime lastActiveAt;
    private Boolean active;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserID() { return userID; }
    public void setUserID(Integer userID) { this.userID = userID; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}