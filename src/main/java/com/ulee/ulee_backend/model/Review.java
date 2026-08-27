package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reviewID;

    private Integer studentID;
    private Integer propertyID;
    private Integer rating;
    private String comment;
    private LocalDateTime reviewDate;

    // Landlord's reply to this review (already existed in the DB before
    // this change — carried over here so this stays a complete, drop-in
    // replacement for the entity).
    private String landlordResponse;
    private LocalDateTime responseDate;

    // Flag a review as reported (e.g. abusive, spam, off-topic). Mirrors
    // the isReported/reportReason pattern already used on Property.
    // "Mark Resolved" on the landlord Reviews page clears this back to
    // false without deleting the underlying review.
    private Boolean isReported;
    private String reportReason;

    public Integer getReviewID() { return reviewID; }
    public void setReviewID(Integer reviewID) { this.reviewID = reviewID; }

    public Integer getStudentID() { return studentID; }
    public void setStudentID(Integer studentID) { this.studentID = studentID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }

    public String getLandlordResponse() { return landlordResponse; }
    public void setLandlordResponse(String landlordResponse) { this.landlordResponse = landlordResponse; }

    public LocalDateTime getResponseDate() { return responseDate; }
    public void setResponseDate(LocalDateTime responseDate) { this.responseDate = responseDate; }

    public Boolean getIsReported() { return isReported; }
    public void setIsReported(Boolean isReported) { this.isReported = isReported; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }
}