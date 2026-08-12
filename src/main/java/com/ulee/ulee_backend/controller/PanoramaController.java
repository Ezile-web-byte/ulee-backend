package com.ulee.ulee_backend.controller;
import com.ulee.ulee_backend.model.*;
import com.ulee.ulee_backend.repository.PropertyPanoramaRepository;
import com.ulee.ulee_backend.repository.PanoramaHotspotRepository;
import com.ulee.ulee_backend.repository.PropertyImageRepository;
import com.ulee.ulee_backend.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class PanoramaController {
    @Autowired
    private PropertyPanoramaRepository propertyPanoramaRepository;
    @Autowired
    private PanoramaHotspotRepository panoramaHotspotRepository;
    @Autowired
    private PropertyImageRepository propertyImageRepository;
    @Autowired
    private PropertyRepository propertyRepository;

    @GetMapping("/property/{id}/vr-tour")
    public String viewVRTour(@PathVariable Integer id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));

        List<PropertyPanorama> panoramas = propertyPanoramaRepository.findByPropertyID(id);

        // Attach the actual image URL to each panorama for the view to use
        for (PropertyPanorama panorama : panoramas) {
            PropertyImage image = propertyImageRepository.findById(panorama.getImageID()).orElse(null);
            panorama.setImageID(panorama.getImageID()); // unchanged, just clarity
        }

        model.addAttribute("property", property);
        model.addAttribute("panoramas", panoramas);

        // Build a lookup of imageID -> url so the template can resolve it easily
        model.addAttribute("imageUrls", propertyImageRepository.findAllById(
                panoramas.stream().map(PropertyPanorama::getImageID).toList()
        ));

        // All hotspots across all panoramas for this property, grouped by source in the view
        List<Integer> panoramaIds = panoramas.stream().map(PropertyPanorama::getPanoramaID).toList();
        model.addAttribute("hotspots", panoramaHotspotRepository.findAll().stream()
                .filter(h -> panoramaIds.contains(h.getSourcePanoramaID()))
                .toList());

        return "vr-tour";
    }
}
