package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.model.StudentNotification;
import com.ulee.ulee_backend.repository.StudentNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Call these from the places where the events already happen
 * (student applies, landlord accepts/rejects, student saves a favourite,
 * student submits a review/report, admin handles a report).
 * Each call just inserts one row, so a failure here should never break the main action:
 * wrap the call in try/catch if you want that guarantee.
 *
 * type values (drive the icon + heading in the bell):
 *   accepted | rejected | submitted | favourite | review | report | handled
 */
@Service
public class StudentNotificationService {

    @Autowired
    private StudentNotificationRepository repository;

    private static String nameOrDefault(String propertyTitle) {
        return (propertyTitle == null || propertyTitle.isBlank()) ? "a property" : propertyTitle;
    }

    /** Student's application was submitted. */
    public void applicationSubmitted(Integer studentId, String propertyTitle) {
        repository.save(new StudentNotification(
                studentId, "submitted",
                "Your application for " + nameOrDefault(propertyTitle) + " was submitted. The landlord will review it.",
                "/my-applications"));
    }

    /** Landlord accepted or rejected the student's application. */
    public void applicationDecision(Integer studentId, String propertyTitle, boolean accepted) {
        String title = nameOrDefault(propertyTitle);
        repository.save(new StudentNotification(
                studentId,
                accepted ? "accepted" : "rejected",
                accepted ? "Your application for " + title + " was accepted."
                        : "Your application for " + title + " was not successful.",
                "/my-applications"));
    }

    /** Student added a property to their favourites. Call on ADD only, not on remove. */
    public void favouriteAdded(Integer studentId, Integer propertyId, String propertyTitle) {
        repository.save(new StudentNotification(
                studentId, "favourite",
                nameOrDefault(propertyTitle) + " was added to your favourites.",
                propertyId != null ? "/property/" + propertyId : "/my-dashboard"));
    }

    /** Student's review was saved. */
    public void reviewSubmitted(Integer studentId, Integer propertyId, String propertyTitle) {
        repository.save(new StudentNotification(
                studentId, "review",
                "Your review for " + nameOrDefault(propertyTitle) + " was submitted successfully.",
                propertyId != null ? "/property/" + propertyId : "/my-dashboard"));
    }

    /** Student's report was saved. */
    public void reportSubmitted(Integer studentId) {
        repository.save(new StudentNotification(
                studentId, "report",
                "Your report was submitted successfully. We will look into it.",
                "/my-dashboard"));
    }

    /** An admin handled the student's report. */
    public void reportHandled(Integer studentId) {
        repository.save(new StudentNotification(
                studentId, "handled",
                "Your report has been handled by our team.",
                "/my-dashboard"));
    }
}