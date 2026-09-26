package com.ulee.ulee_backend.dto;

import java.util.Collections;
import java.util.List;

/**
 * Result of a PlacesService lookup (added in a later stage). Deliberately
 * NOT just a List<NearbyPlaceDTO> — a bare empty list is ambiguous between
 * two very different situations that SwaiChatService must handle
 * differently:
 *
 *   - available=true,  places=[]      -> the lookup ran successfully and
 *                                         genuinely found no matching
 *                                         places nearby. This is a valid,
 *                                         truthful answer, NOT a failure.
 *
 *   - available=false, places=[]      -> the lookup could not be trusted:
 *                                         it failed, timed out, was
 *                                         rate-limited, or could not run at
 *                                         all (missing API key, missing/
 *                                         invalid coordinates, etc). For a
 *                                         neighbourhood-dependent question,
 *                                         SwaiChatService must return the
 *                                         deterministic fallback here and
 *                                         must NOT ask the LLM to answer,
 *                                         since it has no grounded facts to
 *                                         reason from and could otherwise
 *                                         be tempted to invent an answer.
 *
 * places is never null — unavailable() and success() both return an empty
 * (but non-null) list so callers never need a separate null check.
 */
public class PlacesResult {

    private final List<NearbyPlaceDTO> places;
    private final boolean available;

    private PlacesResult(List<NearbyPlaceDTO> places, boolean available) {
        this.places = places;
        this.available = available;
    }

    /** A successful lookup, whether or not it found any matching places. */
    public static PlacesResult success(List<NearbyPlaceDTO> places) {
        return new PlacesResult(
                places == null ? Collections.emptyList() : places,
                true
        );
    }

    /**
     * The lookup could not be trusted (failed, timed out, rate-limited, or
     * could not run — e.g. missing API key or missing/invalid coordinates).
     */
    public static PlacesResult unavailable() {
        return new PlacesResult(Collections.emptyList(), false);
    }

    public List<NearbyPlaceDTO> getPlaces() { return places; }

    public boolean isAvailable() { return available; }
}
