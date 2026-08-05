package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.SavedProperty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Integer> {
}
