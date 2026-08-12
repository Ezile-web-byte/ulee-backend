package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.PanoramaHotspot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PanoramaHotspotRepository extends JpaRepository<PanoramaHotspot,Integer> {
    List<PanoramaHotspot> findBySourcePanoramaID(Integer sourcePanoramaID);
}
