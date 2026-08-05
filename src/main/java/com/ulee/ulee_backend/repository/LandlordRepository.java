package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Landlord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandlordRepository extends JpaRepository<Landlord, Integer> {
}