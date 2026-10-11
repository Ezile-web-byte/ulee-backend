package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.SavedProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Integer> {

    // Used by PropertyController.addFavorite() to toggle save/unsave without
    // creating duplicate rows for the same student+property pair.
    //
    // Returns a List, NOT an Optional, on purpose: the old addFavorite()
    // always inserted a new row with no duplicate check, so existing data
    // can already contain several rows for the same student+property. An
    // Optional-returning query throws IncorrectResultSizeDataAccessException
    // the moment it matches more than one row, which would make unsaving
    // fail outright for exactly those properties. Returning a List lets
    // addFavorite() delete every matching row and self-heal that old data.
    List<SavedProperty> findByStudentIDAndPropertyID(Integer studentID, Integer propertyID);

    // Used by PropertyController.viewStudentDashboard() (heart icon state)
    // and viewSavedProperties() (the Saved Properties listing page).
    List<SavedProperty> findByStudentID(Integer studentID);
}
