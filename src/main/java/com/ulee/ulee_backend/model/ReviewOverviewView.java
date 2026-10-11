package com.ulee.ulee_backend.model;

/**
 * One row on the landlord's "All Properties" reviews overview: a property
 * plus how many reviews it has and how many the landlord has not opened yet.
 * Built in PropertyController.viewPropertyReviews() from real Property and
 * Review rows.
 */
public class ReviewOverviewView {

    private Integer propertyID;
    private String title;
    private String coverUrl;
    private long totalCount;
    private long unviewedCount;

    /** True when this property has at least one review the landlord has not opened. */
    public boolean isHasNew() { return unviewedCount > 0; }

    /** Lower-case title used only for sorting A-Z. */
    public String getTitleSortKey() { return title == null ? "" : title.toLowerCase(); }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getUnviewedCount() { return unviewedCount; }
    public void setUnviewedCount(long unviewedCount) { this.unviewedCount = unviewedCount; }
}