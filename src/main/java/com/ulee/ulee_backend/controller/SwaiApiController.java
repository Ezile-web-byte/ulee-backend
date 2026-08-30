package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.dto.ListingSummaryDTO;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.model.PropertyImage;
import com.ulee.ulee_backend.repository.PropertyImageRepository;
import com.ulee.ulee_backend.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backs the "Speak with AI" widget's category-guided listing matcher
 * (speak-with-ai.js fetches this once, client-side, and filters in-browser
 * using the question tree in speak-with-ai-logic.js). A separate controller
 * — rather than another method on PropertyController — so the widget's data
 * needs stay isolated from the landlord/student page-rendering flows.
 */
@RestController
public class SwaiApiController {

    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private PropertyImageRepository propertyImageRepository;

    // Same findByIsAvailableTrue() query the student dashboard uses, and the
    // same "first image per property" lookup pattern from
    // PropertyController.viewLandlordDashboard — so "approved listings" here
    // means exactly what it means everywhere else in the app.
    @GetMapping("/api/listings")
    public List<ListingSummaryDTO> listings() {
        List<Property> available = propertyRepository.findByIsAvailableTrue();
        List<Integer> propertyIds = available.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        Map<Integer, String> imageLookup = propertyImageRepository.findByPropertyIDIn(propertyIds).stream()
                .collect(Collectors.toMap(
                        PropertyImage::getPropertyID,
                        PropertyImage::getUrl,
                        (existing, replacement) -> existing));

        return available.stream()
                .map(p -> new ListingSummaryDTO(
                        p.getPropertyID(),
                        p.getTitle(),
                        p.getAddress(),
                        p.getCity(),
                        p.getRent(),
                        p.getType(),
                        p.getCommuteType(),
                        imageLookup.get(p.getPropertyID())))
                .collect(Collectors.toList());
    }
}