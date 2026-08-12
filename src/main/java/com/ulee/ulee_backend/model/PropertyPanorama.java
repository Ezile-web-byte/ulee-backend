package com.ulee.ulee_backend.model;
import jakarta.persistence.*;

@Entity
@Table(name = "property_panorama")
public class PropertyPanorama {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer panoramaID;

    private Integer propertyID;
    private String roomName;
    private Integer imageID;
    private Boolean isEntryPoint;

    public Integer getPanoramaID() { return panoramaID; }
    public void setPanoramaID(Integer panoramaID) { this.panoramaID = panoramaID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public Integer getImageID() { return imageID; }
    public void setImageID(Integer imageID) { this.imageID = imageID; }

    public Boolean getIsEntryPoint() { return isEntryPoint; }
    public void setIsEntryPoint(Boolean isEntryPoint) { this.isEntryPoint = isEntryPoint; }
}

