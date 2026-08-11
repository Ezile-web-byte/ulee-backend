package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reviewID;

    private Integer studentID;
    private Integer propertyID;
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime reviewDate;

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
}