package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.model.StudentNotification;
import com.ulee.ulee_backend.repository.StudentNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Call these from the places where the events already happen
 * (landlord accepts/rejects, student submits a review/report, admin handles a report).
 * Each call just inserts one row, so a failure here should never break the main action:
 * wrap the call in try/catch if you want that guarantee.
 */
@Service
public class StudentNotificationService {

    @Autowired
    private StudentNotificationRepository repository;

    /** Landlord accepted or rejected the student's application. */
    public void applicationDecision(Integer studentId, String propertyTitle, boolean accepted) {
        String title = (propertyTitle == null || propertyTitle.isBlank()) ? "a property" : propertyTitle;
        repository.save(new StudentNotification(
                studentId,
                accepted ? "accepted" : "rejected",
                accepted ? "Your application for " + title + " was accepted."
                        : "Your application for " + title + " was not successful.",
                "/my-applications"));
    }

    /** Student's review was saved. */
    public void reviewSubmitted(Integer studentId, Integer propertyId, String propertyTitle) {
        String title = (propertyTitle == null || propertyTitle.isBlank()) ? "a property" : propertyTitle;
        repository.save(new StudentNotification(
                studentId, "review",
                "Your review for " + title + " was submitted successfully.",
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