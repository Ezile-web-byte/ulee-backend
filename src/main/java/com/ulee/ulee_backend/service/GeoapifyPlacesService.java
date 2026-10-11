package com.ulee.ulee_backend.service;

import tools.jackson.databind.JsonNode;
import com.ulee.ulee_backend.config.GeoapifyProperties;
import com.ulee.ulee_backend.dto.NearbyPlaceDTO;
import com.ulee.ulee_backend.dto.PlacesResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PlacesService implementation backed by the Geoapify Places API
 * (https://api.geoapify.com/v2/places). All Geoapify-specific request
 * building and GeoJSON response parsing lives in this class only — every
 * other part of the app sees just the normalized NearbyPlaceDTO/
 * PlacesResult types from the dto package, so swapping providers later
 * only means writing a new PlacesService implementation.
 *
 * Reliability contract (see PlacesService/PlacesResult Javadoc): this
 * class NEVER throws out of findNearbyPlaces, and NEVER fabricates places.
 * Any condition that would make the result untrustworthy — missing
 * coordinates, a blank/unconfigured API key, an HTTP error, a timeout, or
 * a rate limit — returns PlacesResult.unavailable() instead.
 */
@Service
public class GeoapifyPlacesService implements PlacesService {

    private static final String PLACES_PATH = "/v2/places";

    // Search radius around the property. Wide enough to cover "what's
    // around here" / "nearest mall" style questions in a typical suburb,
    // without pulling in places from a genuinely different neighbourhood.
    private static final int SEARCH_RADIUS_METERS = 5000;

    // Capped well under Geoapify's 500-per-request ceiling — this is a
    // chat assistant summarizing a neighbourhood, not a places browser, so
    // a large, mostly-unused result set would just waste API credits.
    private static final int RESULT_LIMIT = 20;

    // The student-relevant category groups approved for this feature:
    // shopping/malls, groceries, restaurants/cafes, recreation
    // (parks/beaches), gyms, worship, nightlife/entertainment,
    // healthcare/pharmacies, and public transport. Sent to Geoapify as a
    // single comma-separated "categories" query parameter.
    private static final List<String> QUERY_CATEGORIES = List.of(
            "commercial.shopping_mall",
            "commercial.supermarket",
            "commercial.convenience",
            "catering.restaurant",
            "catering.cafe",
            "leisure.park",
            "beach",
            "sport.fitness.gym",
            "religion.place_of_worship",
            "entertainment",
            "catering.bar",
            "catering.pub",
            "adult.nightclub",
            "healthcare",
            "healthcare.pharmacy",
            "public_transport"
    );

    // Maps a Geoapify category prefix to the normalized label stored on
    // NearbyPlaceDTO.category. Order matters: checked top-to-bottom, so
    // more specific prefixes (e.g. "healthcare.pharmacy") must appear
    // before broader ones they'd otherwise be swallowed by (e.g.
    // "healthcare") — see categorize(JsonNode) below.
    private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();
    static {
        CATEGORY_LABELS.put("commercial.shopping_mall", "shopping_mall");
        CATEGORY_LABELS.put("commercial.supermarket", "supermarket");
        CATEGORY_LABELS.put("commercial.convenience", "supermarket");
        CATEGORY_LABELS.put("catering.restaurant", "restaurant");
        CATEGORY_LABELS.put("catering.cafe", "cafe");
        CATEGORY_LABELS.put("leisure.park", "park");
        CATEGORY_LABELS.put("beach", "beach");
        CATEGORY_LABELS.put("sport.fitness.gym", "gym");
        CATEGORY_LABELS.put("religion.place_of_worship", "place_of_worship");
        CATEGORY_LABELS.put("healthcare.pharmacy", "pharmacy");
        CATEGORY_LABELS.put("healthcare", "healthcare");
        CATEGORY_LABELS.put("adult.nightclub", "nightlife");
        CATEGORY_LABELS.put("catering.bar", "nightlife");
        CATEGORY_LABELS.put("catering.pub", "nightlife");
        CATEGORY_LABELS.put("entertainment", "entertainment");
        CATEGORY_LABELS.put("public_transport", "public_transport");
    }

    private final RestClient geoapifyRestClient;
    private final GeoapifyProperties geoapifyProperties;

    @Autowired
    public GeoapifyPlacesService(@Qualifier("geoapifyRestClient") RestClient geoapifyRestClient,
                                  GeoapifyProperties geoapifyProperties) {
        this.geoapifyRestClient = geoapifyRestClient;
        this.geoapifyProperties = geoapifyProperties;
    }

    @Override
    public PlacesResult findNearbyPlaces(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            // No usable coordinates — do not call the API at all.
            return PlacesResult.unavailable();
        }

        String apiKey = geoapifyProperties.getKey();
        if (apiKey == null || apiKey.isBlank()) {
            // Not configured — do not call the API at all, and never send
            // a request with a missing/blank key.
            return PlacesResult.unavailable();
        }

        String categories = String.join(",", QUERY_CATEGORIES);
        // Geoapify's filter/bias circle format is "lon,lat,radiusMeters" —
        // longitude first, matching the API's coordinate order (not the
        // more common lat,lon order used elsewhere in this codebase).
        String circle = longitude + "," + latitude + "," + SEARCH_RADIUS_METERS;
        String proximity = longitude + "," + latitude;

        try {
            JsonNode response = geoapifyRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PLACES_PATH)
                            .queryParam("categories", categories)
                            .queryParam("filter", "circle:" + circle)
                            .queryParam("bias", "proximity:" + proximity)
                            .queryParam("limit", RESULT_LIMIT)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            return PlacesResult.success(toNearbyPlaces(response));
        } catch (RestClientException e) {
            // Covers non-2xx responses (including 401/403 for a bad key
            // and 429 for rate limiting), timeouts, and connectivity
            // failures. Deliberately does not include the raw exception
            // (which could echo the request URI containing the API key)
            // in any log line — only a generic, key-free message.
            System.err.println("Geoapify places lookup failed: " + e.getClass().getSimpleName());
            return PlacesResult.unavailable();
        } catch (Exception e) {
            // Defensive catch-all (e.g. unexpected response shape while
            // parsing) — still must never throw out of this method.
            System.err.println("Geoapify places lookup failed unexpectedly: " + e.getClass().getSimpleName());
            return PlacesResult.unavailable();
        }
    }

    /** Converts a Geoapify /v2/places GeoJSON FeatureCollection into our normalized DTO list. */
    private List<NearbyPlaceDTO> toNearbyPlaces(JsonNode response) {
        List<NearbyPlaceDTO> places = new ArrayList<>();
        if (response == null) {
            return places;
        }

        JsonNode features = response.path("features");
        if (!features.isArray()) {
            return places;
        }

        for (JsonNode feature : features) {
            JsonNode properties = feature.path("properties");
            if (properties.isMissingNode()) {
                continue;
            }

            String name = textOrNull(properties.path("name"));
            if (name == null) {
                // Skip unnamed features (e.g. some generic OSM nodes) — a
                // nameless "place" is not useful to surface to a student.
                continue;
            }

            String address = textOrNull(properties.path("formatted"));
            Double distanceMeters = properties.path("distance").isNumber()
                    ? properties.path("distance").asDouble()
                    : null;
            String category = categorize(properties.path("categories"));

            places.add(new NearbyPlaceDTO(name, category, distanceMeters, address));
        }

        return places;
    }

    /** Picks the normalized label for the first matching known category prefix, or "other" if none match. */
    private String categorize(JsonNode categoriesNode) {
        if (!categoriesNode.isArray()) {
            return "other";
        }

        List<String> rawCategories = new ArrayList<>();
        for (JsonNode c : categoriesNode) {
            if (c.isTextual()) {
                rawCategories.add(c.asText());
            }
        }

        for (Map.Entry<String, String> entry : CATEGORY_LABELS.entrySet()) {
            String prefix = entry.getKey();
            for (String raw : rawCategories) {
                if (raw.equals(prefix) || raw.startsWith(prefix + ".")) {
                    return entry.getValue();
                }
            }
        }

        return "other";
    }

    private String textOrNull(JsonNode node) {
        return node.isTextual() ? node.asText() : null;
    }
}
