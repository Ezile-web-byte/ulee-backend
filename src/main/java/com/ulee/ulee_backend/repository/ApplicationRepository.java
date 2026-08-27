package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    List<Application> findByStudentID(Integer studentID);

    List<Application> findByPropertyIDIn(List<Integer> propertyIDs);

    // Backs the one-application-per-property rule in
    // PropertyController.applyToProperty(). Matches on studentID + propertyID
    // regardless of status, so a student can't apply again even if their
    // first application was Rejected.
    boolean existsByStudentIDAndPropertyID(Integer studentID, Integer propertyID);

}