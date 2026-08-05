package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "property")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer propertyID;

    private Integer landlordID;
    private String title;
    private String description;
    private java.math.BigDecimal rent;
    private java.math.BigDecimal deposit;
    private String address;
    private String city;
    private String municipality;
    private String suburb;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private String type;
    private Integer bedrooms;
    private Integer bathrooms;
    private java.math.BigDecimal area;
    private Boolean furnished;
    private Boolean studyFriendly;
    private Boolean isAvailable;
    private LocalDate availableFrom;
    private java.math.BigDecimal distanceFromUniversity;
    private java.math.BigDecimal rating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private Boolean isReported;
    private String reportReason;

    // ── Images: read-only link to propertyimage.propertyID.
    //    Uses the existing plain "propertyID" column on PropertyImage directly,
    //    so PropertyImage.java does not need any changes.
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyID", referencedColumnName = "propertyID", insertable = false, updatable = false)
    private List<PropertyImage> images;

    @Transient
    public String getMainImageUrl() {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .findFirst()
                .map(PropertyImage::getUrl)
                .orElse(images.get(0).getUrl());
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsReported() { return isReported; }
    public void setIsReported(Boolean isReported) { this.isReported = isReported; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }
    // Getters and setters (Spring needs these to read/write each field)

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public Integer getLandlordID() { return landlordID; }
    public void setLandlordID(Integer landlordID) { this.landlordID = landlordID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public java.math.BigDecimal getRent() { return rent; }
    public void setRent(java.math.BigDecimal rent) { this.rent = rent; }

    public java.math.BigDecimal getDeposit() { return deposit; }
    public void setDeposit(java.math.BigDecimal deposit) { this.deposit = deposit; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getSuburb() { return suburb; }
    public void setSuburb(String suburb) { this.suburb = suburb; }

    public java.math.BigDecimal getLatitude() { return latitude; }
    public void setLatitude(java.math.BigDecimal latitude) { this.latitude = latitude; }

    public java.math.BigDecimal getLongitude() { return longitude; }
    public void setLongitude(java.math.BigDecimal longitude) { this.longitude = longitude; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public java.math.BigDecimal getArea() { return area; }
    public void setArea(java.math.BigDecimal area) { this.area = area; }

    public Boolean getFurnished() { return furnished; }
    public void setFurnished(Boolean furnished) { this.furnished = furnished; }

    public Boolean getStudyFriendly() { return studyFriendly; }
    public void setStudyFriendly(Boolean studyFriendly) { this.studyFriendly = studyFriendly; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }

    public java.math.BigDecimal getDistanceFromUniversity() { return distanceFromUniversity; }
    public void setDistanceFromUniversity(java.math.BigDecimal distanceFromUniversity) { this.distanceFromUniversity = distanceFromUniversity; }

    public java.math.BigDecimal getRating() { return rating; }
    public void setRating(java.math.BigDecimal rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}