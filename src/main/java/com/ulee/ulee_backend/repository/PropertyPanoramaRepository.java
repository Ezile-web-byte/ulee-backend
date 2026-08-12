package com.ulee.ulee_backend.repository;
import com.ulee.ulee_backend.model.PropertyPanorama;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PropertyPanoramaRepository extends JpaRepository<PropertyPanorama,Integer> {
    List<PropertyPanorama> findByPropertyID(Integer propertyID);
}
