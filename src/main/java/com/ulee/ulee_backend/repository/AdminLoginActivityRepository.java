package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.AdminLoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminLoginActivityRepository extends JpaRepository<AdminLoginActivity, Integer> {

    List<AdminLoginActivity> findTop10ByUserIDOrderByTimestampDesc(Integer userID);
}