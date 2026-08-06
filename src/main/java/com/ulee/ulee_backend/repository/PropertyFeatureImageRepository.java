package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.PropertyFeatureImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyFeatureImageRepository extends JpaRepository<PropertyFeatureImage, Integer> {

    List<PropertyFeatureImage> findByFeatureID(Integer featureID);
}