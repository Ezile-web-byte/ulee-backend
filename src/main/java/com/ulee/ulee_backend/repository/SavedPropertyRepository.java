package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.SavedProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Integer> {

    // Used by PropertyController.addFavorite() to toggle save/unsave without
    // creating duplicate rows for the same student+property pair.
    Optional<SavedProperty> findByStudentIDAndPropertyID(Integer studentID, Integer propertyID);

    // Used by PropertyController.viewStudentDashboard() (heart icon state)
    // and viewSavedProperties() (the Saved Properties listing page).
    List<SavedProperty> findByStudentID(Integer studentID);
}
