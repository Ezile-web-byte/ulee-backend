package com.ulee.ulee_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "propertyimage")
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer imageID;


    private Integer propertyID;
    private String url;
    private String category;
    private String caption;
    private Boolean isMain;
    private Integer displayOrder;

    public Integer getImageID() { return imageID; }
    public void setImageID(Integer imageID) { this.imageID = imageID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Boolean getIsMain() { return isMain; }
    public void setIsMain(Boolean isMain) { this.isMain = isMain; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }


}
