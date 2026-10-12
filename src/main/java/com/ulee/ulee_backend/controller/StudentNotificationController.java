package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.StudentNotification;
import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.StudentNotificationRepository;
import com.ulee.ulee_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Backs the student notification bell (fragments/student-notifications.html).
 * Mirrors NotificationController (landlord): reads the student off
 * Authentication, so the bell works on any student page.
 */
@RestController
@RequestMapping("/student/notifications")
public class StudentNotificationController {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    @Autowired
    private StudentNotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private Optional<Integer> currentStudentId(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        return userOpt.map(User::getUserID);
    }

    // GET /student/notifications
    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        Optional<Integer> studentId = currentStudentId(authentication);
        if (studentId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        List<StudentNotification> notifications = notificationRepository
                .findByStudentIDOrderByCreatedAtDesc(studentId.get());

        List<Map<String, Object>> payload = new ArrayList<>();
        for (StudentNotification n : notifications) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", n.getNotificationID());
            row.put("type", n.getType());
            row.put("message", n.getMessage());
            row.put("link", n.getLink());
            row.put("timeDisplay", n.getCreatedAt() != null ? n.getCreatedAt().format(DISPLAY_FORMAT) : "");
            row.put("isRead", Boolean.TRUE.equals(n.getIsRead()));
            payload.add(row);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notifications", payload);
        response.put("unreadCount", notificationRepository.countByStudentIDAndIsReadFalse(studentId.get()));
        return ResponseEntity.ok(response);
    }

    // POST /student/notifications/{id}/read
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Integer id, Authentication authentication) {
        Optional<Integer> studentId = currentStudentId(authentication);
        if (studentId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        Optional<StudentNotification> opt = notificationRepository.findById(id);
        if (opt.isEmpty() || !Objects.equals(opt.get().getStudentID(), studentId.get())) {
            return ResponseEntity.status(404).body(Map.of("error", "Notification not found"));
        }

        StudentNotification n = opt.get();
        n.setIsRead(true);
        notificationRepository.save(n);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // POST /student/notifications/read-all
    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        Optional<Integer> studentId = currentStudentId(authentication);
        if (studentId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        for (StudentNotification n : notificationRepository.findByStudentIDOrderByCreatedAtDesc(studentId.get())) {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}