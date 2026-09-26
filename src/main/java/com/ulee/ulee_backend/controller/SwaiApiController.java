package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.dto.ChatRequestDTO;
import com.ulee.ulee_backend.dto.ChatResponseDTO;
import com.ulee.ulee_backend.dto.ListingSummaryDTO;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.model.PropertyImage;
import com.ulee.ulee_backend.repository.PropertyImageRepository;
import com.ulee.ulee_backend.repository.PropertyRepository;
import com.ulee.ulee_backend.service.SwaiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backs the "Speak with AI" widget: the category-guided listing matcher
 * (speak-with-ai.js fetches GET /api/listings once, client-side, and
 * filters in-browser using the question tree in speak-with-ai-logic.js)
 * and the free-text chat endpoint (POST /api/swai/chat). A separate
 * controller — rather than another method on PropertyController — so the
 * widget's data needs stay isolated from the landlord/student
 * page-rendering flows.
 *
 * This controller is deliberately thin: all intent detection, property
 * lookup, Geoapify/Groq calls, prompt grounding, and fallback decisions
 * live in SwaiChatService (and the services it composes). This class only
 * adapts an HTTP request/response to that service call.
 */
@RestController
public class SwaiApiController {

    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private PropertyImageRepository propertyImageRepository;
    @Autowired
    private SwaiChatService swaiChatService;

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

    // Free-text chat endpoint for the SWAI widget (speak-with-ai.js).
    // propertyId is optional — General_Context pages send none. This
    // method does no intent detection, property lookup, or LLM/Places
    // logic itself; it only adapts the HTTP request/response to
    // SwaiChatService.handle(...), which owns all of that behavior and
    // already returns a safe, user-facing message for every failure mode
    // (unconfigured provider, provider error, missing coordinates, no
    // matching places, etc). required = false so a missing/empty body
    // becomes a null ChatRequestDTO here rather than a 400 — the service
    // treats a null request the same as a null/blank message.
    @PostMapping("/api/swai/chat")
    public ChatResponseDTO chat(@RequestBody(required = false) ChatRequestDTO request) {
        return swaiChatService.handle(request);
    }
}