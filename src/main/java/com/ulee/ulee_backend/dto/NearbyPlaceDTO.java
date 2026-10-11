package com.ulee.ulee_backend.dto;

/**
 * A single nearby place, normalized from the Geoapify Places API response
 * (see GeoapifyPlacesService, added in a later stage). This shape is
 * intentionally provider-agnostic — nothing outside GeoapifyPlacesService
 * should ever touch Geoapify's raw JSON, so swapping providers later only
 * requires a new PlacesService implementation, not changes to callers.
 */
public class NearbyPlaceDTO {

    private String name;
    private String category;       // normalized category, e.g. "restaurant", "supermarket", "gym"
    private Double distanceMeters; // straight-line distance from the property's coordinates
    private String address;

    public NearbyPlaceDTO() {
    }

    public NearbyPlaceDTO(String name, String category, Double distanceMeters, String address) {
        this.name = name;
        this.category = category;
        this.distanceMeters = distanceMeters;
        this.address = address;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
