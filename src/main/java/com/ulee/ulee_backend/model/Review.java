package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reviewID;
    private Boolean isReported;
    private String reportReason;
    private Integer studentID;
    private Integer propertyID;
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime reviewDate;

    // ── New fields for the redesigned review form ──
    private Integer cleanlinessRating;
    private Integer safetyRating;
    private Integer wifiRating;
    private Integer studyAreaRating;
    private Integer yearOfStudy;
    private String residencyStatus; // "Current" or "Past"

    // NEW — has the landlord opened this review yet? Every freshly written
    // review starts as false ("new"). It flips to true when the landlord
    // opens that review on the property's reviews page. Reviews that existed
    // before this column was added have NULL here and are treated as already
    // viewed, so they never show up in the sidebar count.
    private Boolean landlordViewed = false;

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

    public Integer getCleanlinessRating() { return cleanlinessRating; }
    public void setCleanlinessRating(Integer cleanlinessRating) { this.cleanlinessRating = cleanlinessRating; }

    public Integer getSafetyRating() { return safetyRating; }
    public void setSafetyRating(Integer safetyRating) { this.safetyRating = safetyRating; }

    public Integer getWifiRating() { return wifiRating; }
    public void setWifiRating(Integer wifiRating) { this.wifiRating = wifiRating; }

    public Integer getStudyAreaRating() { return studyAreaRating; }
    public void setStudyAreaRating(Integer studyAreaRating) { this.studyAreaRating = studyAreaRating; }

    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getResidencyStatus() { return residencyStatus; }
    public void setResidencyStatus(String residencyStatus) { this.residencyStatus = residencyStatus; }


    public Boolean getIsReported() { return isReported; }
    public void setIsReported(Boolean isReported) { this.isReported = isReported; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }

    public Boolean getLandlordViewed() { return landlordViewed; }
    public void setLandlordViewed(Boolean landlordViewed) { this.landlordViewed = landlordViewed; }
}
