package com.ulee.ulee_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "amenity")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer amenityID;

    private String name;
    private String category;

    public Integer getAmenityID() { return amenityID; }
    public void setAmenityID(Integer amenityID) { this.amenityID = amenityID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}