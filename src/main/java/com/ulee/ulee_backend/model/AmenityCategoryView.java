package com.ulee.ulee_backend.model;

import java.util.List;

/**
 * Display-ready view of one amenity category for the property-detail page
 * (e.g. "Kitchen & Bathroom" with its icon and the items inside it).
 * Built in the controller instead of computed inline in Thymeleaf — the
 * old inline ternary-chain approach hit attoparser's assignation-sequence
 * parsing limits once the icon-matching chain got long enough.
 */
public class AmenityCategoryView {

    private String categoryName;
    private String categoryIcon;
    private List<AmenityItemView> items;

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public List<AmenityItemView> getItems() { return items; }
    public void setItems(List<AmenityItemView> items) { this.items = items; }
}