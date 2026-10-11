package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.dto.PlacesResult;

import java.math.BigDecimal;

/**
 * Looks up real nearby points of interest for a given location. Isolated
 * behind this interface so the concrete provider (currently Geoapify, see
 * GeoapifyPlacesService) can be swapped later without changing any caller.
 *
 * Implementations must return PlacesResult.unavailable() — never throw and
 * never fabricate places — for any condition that makes the result
 * untrustworthy: missing/invalid coordinates, an unconfigured API key, a
 * provider error, timeout, or rate limit. A genuinely successful lookup
 * that simply found no matching places must return
 * PlacesResult.success(emptyList()), which callers (SwaiChatService, added
 * in a later stage) must treat very differently from "unavailable" — see
 * PlacesResult's Javadoc for why that distinction exists.
 */
public interface PlacesService {

    /**
     * @param latitude  property latitude, or null if unknown
     * @param longitude property longitude, or null if unknown
     * @return a successful result (possibly with zero places) if the
     *         lookup could be trusted, otherwise an unavailable result
     */
    PlacesResult findNearbyPlaces(BigDecimal latitude, BigDecimal longitude);
}
