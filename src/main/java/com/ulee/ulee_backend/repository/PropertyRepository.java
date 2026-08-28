package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Integer> {
    List<Property> findByStatus(String status);
    List<Property> findByStatusIn(List<String> statuses);
    List<Property> findByIsReportedTrue();
    List<Property> findByIsAvailableTrue();

    List<Property> findByIsAvailableTrueAndTitleContainingIgnoreCaseOrIsAvailableTrueAndCityContainingIgnoreCase(
            String titleKeyword, String cityKeyword);

    List<Property> findByIsAvailableTrueAndBedroomsGreaterThanEqualAndRentLessThanEqual(
            Integer minBedrooms, BigDecimal maxRent);

    List<Property> findByIsAvailableTrueAndRentLessThanEqual(java.math.BigDecimal rent);

    List<Property> findByLandlordID(Integer landlordID);

}