package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks which synthetic admin notification "events" an admin has already
 * clicked/read. These events (new listing submitted, report filed, warning
 * sent) aren't real rows anywhere else — they're derived on the fly in
 * AdminController.buildNotificationFeed() from Property/Report/Notification.
 * eventKey is a stable id built as "<type>-<sourceId>", e.g. "listing-42",
 * "report-7", "warning-19" — see buildNotificationFeed() for exactly how
 * each is built.
 */
@Entity
@Table(name = "adminnotificationreads")
public class AdminNotificationRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "eventKey", nullable = false, unique = true, length = 100)
    private String eventKey;

    @Column(name = "readAt")
    private LocalDateTime readAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}