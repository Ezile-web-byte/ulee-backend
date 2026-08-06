package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRepository extends JpaRepository<Amenity, Integer> {

    // Used to render the checkbox list on the edit-property page, grouped by category
    List<Amenity> findAllByOrderByCategoryAscNameAsc();
}