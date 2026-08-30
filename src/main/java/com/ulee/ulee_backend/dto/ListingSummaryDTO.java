package com.ulee.ulee_backend.dto;

import java.math.BigDecimal;

/**
 * Lightweight view of a Property used only by the "Speak with AI" widget's
 * category-guided matcher (see /api/listings in SwaiApiController, and
 * speak-with-ai-logic.js on the frontend). Deliberately excludes amenities,
 * images (beyond one cover url), landlord info, applications, etc. — the
 * widget only ever needs enough to filter and render a result card.
 */
public class ListingSummaryDTO {

    private Integer id;
    private String title;
    private String address;
    private String city;
    private BigDecimal rent;
    private String type;         // e.g. "Single Room", "Sharing (2)" — property.type
    private String commuteType;  // e.g. "Walking distance", "Public transport" — property.commuteType
    private String imageUrl;     // first PropertyImage.url for this property, or null

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
}