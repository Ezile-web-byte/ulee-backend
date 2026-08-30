package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByLandlordIDOrderByCreatedAtDesc(Integer landlordID);

    long countByLandlordIDAndIsReadFalse(Integer landlordID);
}