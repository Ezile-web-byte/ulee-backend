package com.ulee.ulee_backend.model;

import java.time.LocalDateTime;

/**
 * Display-only view of a Review, with the student's name/year resolved
 * so the templates don't need to look anything up themselves.
 * Not a JPA entity — nothing to persist here, just a carrier for the view.
 */
public class ReviewRowView {

    private Integer reviewID;
    private Integer propertyID;
    private String reviewerName;   // e.g. "Sarah M."
    private String reviewerRole;   // e.g. "Resident • 2nd Year Student"
    private String initials;       // e.g. "SM"
    private Integer rating;        // 1-5
    private String comment;
    private LocalDateTime reviewDate;

    public Integer getReviewID() { return reviewID; }
    public void setReviewID(Integer reviewID) { this.reviewID = reviewID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public String getReviewerRole() { return reviewerRole; }
    public void setReviewerRole(String reviewerRole) { this.reviewerRole = reviewerRole; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }
}