package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.Notification;
import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.NotificationRepository;
import com.ulee.ulee_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Backs the landlord notification bell (fragments/landlord-notifications.html).
 *
 * Deliberately self-contained: it reads the logged-in landlord off
 * Authentication rather than expecting the landlord dashboard's own
 * controller to have added anything to the Model. That means the bell
 * fragment can be dropped into ANY landlord page and it just works —
 * no existing landlord controller code needs to change.
 */
@RestController
@RequestMapping("/landlord/notifications")
public class NotificationController {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private Optional<Integer> currentLandlordId(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        return userOpt.map(User::getUserID);
    }

    // GET /landlord/notifications — list, newest first, plus unread count for the badge
    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        Optional<Integer> landlordId = currentLandlordId(authentication);
        if (landlordId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<Notification> notifications = notificationRepository
                .findByLandlordIDOrderByCreatedAtDesc(landlordId.get());

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Notification n : notifications) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", n.getNotificationID());
            row.put("title", n.getTitle());
            row.put("message", n.getMessage());
            row.put("propertyId", n.getPropertyID());
            row.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(DISPLAY_FORMAT) : "");
            row.put("isRead", Boolean.TRUE.equals(n.getIsRead()));
            payload.add(row);
        }

        long unreadCount = notificationRepository.countByLandlordIDAndIsReadFalse(landlordId.get());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notifications", payload);
        response.put("unreadCount", unreadCount);
        return ResponseEntity.ok(response);
    }

    // POST /landlord/notifications/{id}/read — mark one as read (called when the landlord opens it)
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Integer id, Authentication authentication) {
        Optional<Integer> landlordId = currentLandlordId(authentication);
        if (landlordId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isEmpty() || !Objects.equals(notificationOpt.get().getLandlordID(), landlordId.get())) {
            return ResponseEntity.status(404).body(Map.of("error", "Notification not found"));
        }

        Notification notification = notificationOpt.get();
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // POST /landlord/notifications/read-all — mark everything read (the "mark all as read" link)
    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        Optional<Integer> landlordId = currentLandlordId(authentication);
        if (landlordId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<Notification> notifications = notificationRepository
                .findByLandlordIDOrderByCreatedAtDesc(landlordId.get());
        for (Notification n : notifications) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}