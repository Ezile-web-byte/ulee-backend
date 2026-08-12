package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    List<Application> findByStudentID(Integer studentID);

    List<Application> findByPropertyIDIn(List<Integer> propertyIDs);
    List<Application> findByStudentIDAndPropertyID(Integer studentID, Integer propertyID);

}