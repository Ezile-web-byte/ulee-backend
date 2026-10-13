package com.ulee.ulee_backend.model;

import java.time.LocalDateTime;

/**
 * One card on the landlord's "All Properties" applications overview: a
 * property plus how many applications it has in each state. Built in
 * PropertyController.viewApplicationsOverview() from real Property and
 * Application rows — nothing here is typed in by hand.
 */
public class ApplicationOverviewView {

    private Integer propertyID;
    private String title;
    private String suburb;
    private String coverUrl;
    private int capacity;
    private long pendingCount;      // new applications still waiting for a decision
    private long acceptedCount;
    private long rejectedCount;
    private long totalCount;
    private long acceptedPercent;   // accepted / capacity, 0-100
    private LocalDateTime latestApplicationDate;  // most recent application of any status
    private LocalDateTime oldestPendingDate;      // the longest-waiting pending application

    /** True when this property has at least one application waiting. */
    public boolean isHasNew() { return pendingCount > 0; }

    /** True when every place is taken. */
    public boolean isFull() { return capacity > 0 && acceptedCount >= capacity; }

    /** Lower-case title used only for sorting A-Z. */
    public String getTitleSortKey() { return title == null ? "" : title.toLowerCase(); }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSuburb() { return suburb; }
    public void setSuburb(String suburb) { this.suburb = suburb; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

    public long getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(long acceptedCount) { this.acceptedCount = acceptedCount; }

    public long getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(long rejectedCount) { this.rejectedCount = rejectedCount; }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getAcceptedPercent() { return acceptedPercent; }
    public void setAcceptedPercent(long acceptedPercent) { this.acceptedPercent = acceptedPercent; }

    public LocalDateTime getLatestApplicationDate() { return latestApplicationDate; }
    public void setLatestApplicationDate(LocalDateTime latestApplicationDate) { this.latestApplicationDate = latestApplicationDate; }

    public LocalDateTime getOldestPendingDate() { return oldestPendingDate; }
    public void setOldestPendingDate(LocalDateTime oldestPendingDate) { this.oldestPendingDate = oldestPendingDate; }
}