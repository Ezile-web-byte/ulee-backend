package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.StudentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentNotificationRepository extends JpaRepository<StudentNotification, Integer> {

    List<StudentNotification> findByStudentIDOrderByCreatedAtDesc(Integer studentID);

    long countByStudentIDAndIsReadFalse(Integer studentID);
}