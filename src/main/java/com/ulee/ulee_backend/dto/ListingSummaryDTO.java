package com.ulee.ulee_backend.dto;

import java.math.BigDecimal;

/**
 * Lightweight view of a Property used by the "Speak with AI" widget: the
 * category-guided matcher on the front end, and the AI assistant (which reads
 * these details so it can answer questions about ANY listing, not only the one
 * the student is currently viewing). Deliberately excludes landlord info,
 * applications, coordinates, etc.
 */
public class ListingSummaryDTO {

    private Integer id;
    private String title;
    private String address;
    private String city;
    private BigDecimal rent;
    private String type;         // e.g. "Single", "Sharing", "Commune" — property.type
    private String commuteType;  // e.g. "Walking distance", "Public transport" — property.commuteType
    private String imageUrl;     // first PropertyImage.url for this property, or null

    // ---- Extra details so the AI can answer questions about any listing ----
    private String suburb;
    private BigDecimal deposit;
    private Integer capacity;
    private BigDecimal rating;
    private Integer reviewCount;
    private String availableFrom;   // ISO date text, e.g. "2026-10-01"
    private String features;        // amenities + special features, comma-separated
    private String description;     // shortened to keep the AI prompt small

    public ListingSummaryDTO() {
    }

    public ListingSummaryDTO(Integer id, String title, String address, String city,
                             BigDecimal rent, String type, String commuteType, String imageUrl) {
        this.id = id;
        this.title = title;
        this.address = address;
        this.city = city;
        this.rent = rent;
        this.type = type;
        this.commuteType = commuteType;
        this.imageUrl = imageUrl;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public BigDecimal getRent() { return rent; }
    public void setRent(BigDecimal rent) { this.rent = rent; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCommuteType() { return commuteType; }
    public void setCommuteType(String commuteType) { this.commuteType = commuteType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSuburb() { return suburb; }
    public void setSuburb(String suburb) { this.suburb = suburb; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public String getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(String availableFrom) { this.availableFrom = availableFrom; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}