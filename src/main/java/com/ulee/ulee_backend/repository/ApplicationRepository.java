package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    List<Application> findByStudentID(Integer studentID);

    List<Application> findByPropertyIDIn(List<Integer> propertyIDs);
    List<Application> findByStudentIDAndPropertyID(Integer studentID, Integer propertyID);

    // Backs the one-application-per-property rule in
    // PropertyController.applyToProperty(). Matches on studentID + propertyID
    // regardless of status, so a student can't apply again even if their
    // first application was Rejected.
    boolean existsByStudentIDAndPropertyID(Integer studentID, Integer propertyID);

    // NEW — how many applications across the given properties currently have
    // this status (e.g. "Pending"). Used for the pending-applications badge on
    // the landlord sidebar, so it's one COUNT query instead of loading every
    // application row. Callers must not pass an empty list (they return 0
    // themselves in that case).
    long countByPropertyIDInAndStatus(List<Integer> propertyIDs, String status);

}