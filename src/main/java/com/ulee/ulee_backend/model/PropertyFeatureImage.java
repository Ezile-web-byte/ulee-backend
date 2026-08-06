package com.ulee.ulee_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "property_feature_image")
public class PropertyFeatureImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer imageID;

    private Integer featureID;
    private String url;
    private Integer displayOrder;

    public Integer getImageID() { return imageID; }
    public void setImageID(Integer imageID) { this.imageID = imageID; }

    public Integer getFeatureID() { return featureID; }
    public void setFeatureID(Integer featureID) { this.featureID = featureID; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}