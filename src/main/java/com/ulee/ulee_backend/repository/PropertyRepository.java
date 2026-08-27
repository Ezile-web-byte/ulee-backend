package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Integer> {
    List<Property> findByStatus(String status);
    List<Property> findByIsReportedTrue();
    List<Property> findByIsAvailableTrue();

    List<Property> findByIsAvailableTrueAndTitleContainingIgnoreCaseOrIsAvailableTrueAndCityContainingIgnoreCase(
            String titleKeyword, String cityKeyword);

    List<Property> findByIsAvailableTrueAndBedroomsGreaterThanEqualAndRentLessThanEqual(
            Integer minBedrooms, BigDecimal maxRent);

    List<Property> findByIsAvailableTrueAndRentLessThanEqual(java.math.BigDecimal rent);

    List<Property> findByLandlordID(Integer landlordID);

    // ── Public-visibility queries, keyed off status = "Approved" instead of
    //    isAvailable, so flipping the status column alone is enough to make
    //    a property go live for students. ──
    List<Property> findByStatusAndRentLessThanEqual(String status, BigDecimal rent);

    @Query("SELECT p FROM Property p WHERE p.status = :status AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.city) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Property> searchByStatusAndKeyword(@Param("status") String status, @Param("query") String query);

    // Convenience overload matching the controller's current call shape
    // (always searches "Approved" properties).
    default List<Property> searchApproved(String query) {
        return searchByStatusAndKeyword("Approved", query);
    }
}