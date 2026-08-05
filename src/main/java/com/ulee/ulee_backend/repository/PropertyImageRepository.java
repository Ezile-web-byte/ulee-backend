package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Integer> {

    List<PropertyImage> findByPropertyIDAndIsVRTrue(Integer propertyID);

    // Used by the landlord's property inventory grid to show one thumbnail per card
    List<PropertyImage> findByPropertyIDAndIsMainTrue(Integer propertyID);

    List<PropertyImage> findByPropertyIDIn(java.util.List<Integer> propertyIDs);

}