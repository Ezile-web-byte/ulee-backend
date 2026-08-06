package com.ulee.ulee_backend.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "property_feature")
public class PropertyFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer featureID;

    private Integer propertyID;
    private String name;
    private String description;
    private Integer displayOrder;

    // Read-only link to property_feature_image.featureID, same pattern as Property -> PropertyImage
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "featureID", referencedColumnName = "featureID", insertable = false, updatable = false)
    private List<PropertyFeatureImage> images;

    public Integer getFeatureID() { return featureID; }
    public void setFeatureID(Integer featureID) { this.featureID = featureID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public List<PropertyFeatureImage> getImages() { return images; }
    public void setImages(List<PropertyFeatureImage> images) { this.images = images; }
}