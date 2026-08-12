package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Used by PropertyController.editPropertyForm() / viewPropertyDetail()
    List<Review> findByPropertyID(Integer propertyID);

    // Used by PropertyController.viewPropertyReviews()
    List<Review> findByPropertyIDIn(List<Integer> propertyIDs);

    // Used by PropertyController's A500 review gate (already-reviewed check)
    Optional<Review> findByStudentIDAndPropertyID(Integer studentID, Integer propertyID);

    // Added on main — kept in case a student-facing "my reviews" view needs it
    List<Review> findByStudentID(Integer studentID);
}