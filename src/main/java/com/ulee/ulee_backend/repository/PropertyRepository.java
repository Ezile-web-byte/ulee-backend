package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Integer> {
    List<Property> findByStatus(String status);
    List<Property> findByStatusIn(List<String> statuses);
    List<Property> findByIsReportedTrue();
    List<Property> findByIsAvailableTrue();

    // CHANGED: searches suburb instead of city (city is always Gqeberha now).
    // If anything still calls the old ...AndCityContainingIgnoreCase name,
    // update that call to this name.
    List<Property> findByIsAvailableTrueAndTitleContainingIgnoreCaseOrIsAvailableTrueAndSuburbContainingIgnoreCase(
            String titleKeyword, String suburbKeyword);

    List<Property> findByIsAvailableTrueAndRentLessThanEqual(java.math.BigDecimal rent);

    List<Property> findByLandlordID(Integer landlordID);

    // ── Public-visibility queries, keyed off status = "Active" instead of
    //    isAvailable, so flipping the status column alone is enough to make
    //    a property go live for students. ──
    List<Property> findByStatusAndRentLessThanEqual(String status, BigDecimal rent);

    // CHANGED: keyword search now matches title OR suburb (was city).
    @Query("SELECT p FROM Property p WHERE p.status = :status AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.suburb) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Property> searchByStatusAndKeyword(@Param("status") String status, @Param("query") String query);

    // Convenience overload matching the controller's current call shape
    // (always searches "Active" properties).
    default List<Property> searchActive(String query) {
        return searchByStatusAndKeyword("Active", query);
    }

    // ── Unique property names ──
    // How many properties already use this title (case and surrounding
    // spaces ignored). Used when CREATING a property.
    @Query("SELECT COUNT(p) FROM Property p WHERE LOWER(TRIM(p.title)) = LOWER(:title)")
    long countTitleMatches(@Param("title") String title);

    // Same, but skips one property, so a landlord can re-save their own
    // listing without clashing with itself. Used when EDITING.
    @Query("SELECT COUNT(p) FROM Property p WHERE LOWER(TRIM(p.title)) = LOWER(:title) AND p.propertyID <> :excludeId")
    long countTitleMatchesExcluding(@Param("title") String title, @Param("excludeId") Integer excludeId);
}