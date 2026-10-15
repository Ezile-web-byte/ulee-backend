package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.model.Property;
import java.time.LocalDate;

/** One place that decides whether a student's stay has started, so reviews can't be written early. */
public final class StayEligibility {
    private StayEligibility() {}

    /** True once the property's available-from date has arrived. No date means we can't tell, so not yet. */
    public static boolean hasStarted(Property property) {
        LocalDate from = property == null ? null : property.getAvailableFrom();
        return from != null && !from.isAfter(LocalDate.now());
    }
}