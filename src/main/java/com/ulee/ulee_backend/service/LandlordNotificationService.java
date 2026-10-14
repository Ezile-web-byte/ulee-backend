package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.model.Application;
import com.ulee.ulee_backend.model.Notification;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.NotificationRepository;
import com.ulee.ulee_backend.repository.PropertyRepository;
import com.ulee.ulee_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Creates in-app notifications for landlords. Call these from the places
 * where the events actually happen (admin removing a listing, a student
 * submitting an application). Uses only the existing Notification model
 * and NotificationRepository, so no schema changes are needed.
 */
@Service
public class LandlordNotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private void save(Integer landlordId, Integer propertyId, String title, String message) {
        if (landlordId == null) return;
        Notification n = new Notification();
        n.setLandlordID(landlordId);
        n.setPropertyID(propertyId);
        n.setTitle(title);
        n.setMessage(message);
        n.setCreatedAt(LocalDateTime.now());
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    /**
     * Tell the landlord a listing was removed and why.
     * reason examples: "Reported by users: Maintenance - cold water",
     * "Multiple unresolved reports", "Removed by admin".
     * The complainer's identity is never included.
     */
    public void notifyListingRemoved(Integer landlordId, Integer propertyId,
                                     String landlordName, String propertyName, String reason) {
        String why = (reason == null || reason.isBlank()) ? "Removed by an administrator." : reason.trim();
        String message = "Dear " + (landlordName == null ? "Landlord" : landlordName) + ",\n\n"
                + "Your listing \"" + propertyName + "\" has been removed from the platform.\n\n"
                + "Reason:\n" + why + "\n\n"
                + "If you believe this was a mistake, please contact support.";
        save(landlordId, propertyId, "Listing Removed: " + propertyName, message);
    }

    /** Tell the landlord a student has applied for one of their properties. */
    public void notifyStudentApplied(Integer landlordId, Integer propertyId,
                                     String studentName, String propertyName) {
        String name = (studentName == null || studentName.isBlank()) ? "A student" : studentName.trim();
        String message = name + " has applied for \"" + propertyName + "\".\n\n"
                + "Review the application from your dashboard.";
        save(landlordId, propertyId, "New Application: " + propertyName, message);
    }

    /**
     * One-line version for the place where an application is saved:
     *
     *     Application saved = applicationRepository.save(application);
     *     landlordNotificationService.notifyStudentApplied(saved);
     *
     * Looks up the student's name, the property and its landlord from the
     * application's own IDs. Never throws: a notification problem must not
     * make the student's application fail.
     */
    public void notifyStudentApplied(Application application) {
        try {
            if (application == null || application.getPropertyID() == null) return;
            Property property = propertyRepository.findById(application.getPropertyID()).orElse(null);
            if (property == null) return;

            String studentName = null;
            if (application.getStudentID() != null) {
                User student = userRepository.findById(application.getStudentID()).orElse(null);
                if (student != null) {
                    String first = student.getFirstName() != null ? student.getFirstName() : "";
                    String last = student.getLastName() != null ? student.getLastName() : "";
                    studentName = (first + " " + last).trim();
                }
            }
            notifyStudentApplied(property.getLandlordID(), property.getPropertyID(), studentName, property.getTitle());
        } catch (Exception ignored) {
            // intentionally swallowed, see javadoc
        }
    }
}