package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByPropertyID(Integer propertyID);

    List<Review> findByPropertyIDIn(java.util.List<Integer> propertyIDs);

}