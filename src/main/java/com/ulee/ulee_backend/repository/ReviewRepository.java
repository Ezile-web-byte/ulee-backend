package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Used by PropertyController.editPropertyForm() / viewPropertyDetail()
    List<Review> findByPropertyID(Integer propertyID);

    // Used by PropertyController.viewPropertyReviews()
    List<Review> findByPropertyIDIn(List<Integer> propertyIDs);

    // Used if a student-facing "my reviews" view ever needs it — harmless to
    // include now since it follows the same pattern as ApplicationRepository
    List<Review> findByStudentID(Integer studentID);
}