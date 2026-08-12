package com.ulee.ulee_backend.model;

import java.util.List;

/**
 * Simple view-model: one property + all of its reviews, plus a rolled-up
 * average rating. Used to feed the "property name, then its comments
 * underneath" layout on the Reviews page.
 */
public class PropertyReviewGroup {

    private final Property property;
    private final List<Review> reviews;
    private final double averageRating;

    public PropertyReviewGroup(Property property, List<Review> reviews) {
        this.property = property;
        this.reviews = reviews;
        this.averageRating = reviews.isEmpty() ? 0.0 :
                reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    public Property getProperty() { return property; }
    public List<Review> getReviews() { return reviews; }
    public double getAverageRating() { return averageRating; }
    public int getReviewCount() { return reviews.size(); }
}