package com.ulee.ulee_backend.model;

import java.time.LocalDateTime;

/**
 * Display-ready view of one Review, built in PropertyController.toReviewRowView()
 * from Review + User + Student, since Review only stores raw FK ids.
 */
public class ReviewRowView {

    private Integer reviewID;
    private Integer propertyID;
    private Integer rating;
    private String comment;
    private LocalDateTime reviewDate;
    private String reviewerName;
    private String reviewerRole;
    private String initials;

    // Landlord reply, carried straight through from the entity.
    private String landlordResponse;
    private LocalDateTime responseDate;

    // Reported-review flag, carried straight through from the entity.
    private Boolean isReported;
    private String reportReason;

    public Integer getReviewID() { return reviewID; }
    public void setReviewID(Integer reviewID) { this.reviewID = reviewID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public String getReviewerRole() { return reviewerRole; }
    public void setReviewerRole(String reviewerRole) { this.reviewerRole = reviewerRole; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public String getLandlordResponse() { return landlordResponse; }
    public void setLandlordResponse(String landlordResponse) { this.landlordResponse = landlordResponse; }

    public LocalDateTime getResponseDate() { return responseDate; }
    public void setResponseDate(LocalDateTime responseDate) { this.responseDate = responseDate; }

    public Boolean getIsReported() { return isReported; }
    public void setIsReported(Boolean isReported) { this.isReported = isReported; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }
}