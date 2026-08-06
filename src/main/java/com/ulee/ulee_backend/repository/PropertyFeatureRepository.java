package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.PropertyFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyFeatureRepository extends JpaRepository<PropertyFeature, Integer> {

    List<PropertyFeature> findByPropertyID(Integer propertyID);
}