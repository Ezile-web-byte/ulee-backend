package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.AdminNotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminNotificationReadRepository extends JpaRepository<AdminNotificationRead, Integer> {
    boolean existsByEventKey(String eventKey);
    Optional<AdminNotificationRead> findByEventKey(String eventKey);
    List<AdminNotificationRead> findByEventKeyIn(List<String> eventKeys);
}