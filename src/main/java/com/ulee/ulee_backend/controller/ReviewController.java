package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import com.ulee.ulee_backend.repository.*;
import com.ulee.ulee_backend.service.StayEligibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Student review flow (A500). Saves into the `reviews` table.
 * Same checks as before (accepted + not already reviewed), now also saving the
 * four sub-ratings, residency status and year of study from the new form.
 */
@Controller
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    // key, label, icon, hint: drives the four optional sub-rating rows on the form
    private static final List<String[]> SUB_RATINGS = List.of(
            new String[]{"cleanlinessRating", "Cleanliness", "cleaning_services", "Rooms and shared areas"},
            new String[]{"safetyRating", "Safety & security", "shield", "Locks, lighting, neighbourhood"},
            new String[]{"wifiRating", "Wi-Fi", "wifi", "Speed and reliability"},
            new String[]{"studyAreaRating", "Study space", "menu_book", "Quiet places to study"});

    private boolean validOptional(Integer r) {
        return r == null || (r >= 1 && r <= 5);
    }

    // The one property a student may review: their newest accepted application
    // whose stay has started. Any other accepted place is read-only for them.
    private Integer currentStayPropertyId(Integer studentId) {
        return applicationRepository.findByStudentID(studentId).stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()) && a.getPropertyID() != null)
                .filter(a -> propertyRepository.findById(a.getPropertyID()).map(StayEligibility::hasStarted).orElse(false))
                .max(java.util.Comparator.comparing(com.ulee.ulee_backend.model.Application::getApplicationID))
                .map(com.ulee.ulee_backend.model.Application::getPropertyID)
                .orElse(null);
    }

    // A500: show the review form for a specific property
    @GetMapping("/write-review/{propertyId}")
    public String showReviewForm(@PathVariable Integer propertyId, Model model,
                                 Authentication authentication, RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Could not identify your account. Please log in again.");
            return "redirect:/student-dashboard";
        }
        User user = userOpt.get();
        Integer studentId = user.getUserID();

        Optional<Property> propertyOpt = propertyRepository.findById(propertyId);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "That property could not be found.");
            return "redirect:/student-dashboard";
        }

        boolean isAccepted = applicationRepository
                .findByStudentIDAndPropertyID(studentId, propertyId)
                .stream()
                .anyMatch(a -> "Accepted".equalsIgnoreCase(a.getStatus()));

        if (!isAccepted) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can only review properties you've been accepted to stay at.");
            return "redirect:/property/" + propertyId;
        }
        if (!StayEligibility.hasStarted(propertyOpt.get())) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can review this property once your stay has started.");
            return "redirect:/property/" + propertyId;
        }

        if (reviewRepository.findByStudentIDAndPropertyID(studentId, propertyId).isPresent()) {
            redirectAttributes.addFlashAttribute("actionError", "You've already submitted a review for this property.");
            return "redirect:/property/" + propertyId;
        }

        if (!propertyId.equals(currentStayPropertyId(studentId))) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can only review the place you're currently staying at.");
            return "redirect:/property/" + propertyId;
        }
        String imageUrl = propertyImageRepository.findByPropertyID(propertyId).stream()
                .findFirst().map(PropertyImage::getUrl).orElse(null);

        String lastName = user.getLastName();
        String initials = ("" + user.getFirstName().charAt(0)
                + (lastName != null && !lastName.isBlank() ? lastName.charAt(0) : "")).toUpperCase();

        model.addAttribute("property", propertyOpt.get());
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("subRatings", SUB_RATINGS);
        model.addAttribute("userInitials", initials);
        return "student/write-review";
    }

    // A500: submit the review (saved into `reviews`)
    @PostMapping("/submit-review")
    public String submitReview(@RequestParam Integer propertyId,
                               @RequestParam Integer rating,
                               @RequestParam String comment,
                               @RequestParam(required = false) Integer cleanlinessRating,
                               @RequestParam(required = false) Integer safetyRating,
                               @RequestParam(required = false) Integer wifiRating,
                               @RequestParam(required = false) Integer studyAreaRating,
                               @RequestParam(required = false) String residencyStatus,
                               @RequestParam(required = false) Integer yearOfStudy,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Could not identify your account. Please log in again.");
            return "redirect:/student-dashboard";
        }
        Integer studentId = userOpt.get().getUserID();

        if (rating < 1 || rating > 5
                || !validOptional(cleanlinessRating) || !validOptional(safetyRating)
                || !validOptional(wifiRating) || !validOptional(studyAreaRating)) {
            redirectAttributes.addFlashAttribute("actionError", "Ratings must be between 1 and 5.");
            return "redirect:/write-review/" + propertyId;
        }

        String cleanComment = comment == null ? "" : comment.trim();
        if (cleanComment.length() < 10) {
            redirectAttributes.addFlashAttribute("actionError", "Please write at least a short sentence about your stay.");
            return "redirect:/write-review/" + propertyId;
        }

        boolean isAccepted = applicationRepository
                .findByStudentIDAndPropertyID(studentId, propertyId)
                .stream()
                .anyMatch(a -> "Accepted".equalsIgnoreCase(a.getStatus()));

        if (!isAccepted) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can only review properties you've been accepted to stay at.");
            return "redirect:/property/" + propertyId;
        }

        Optional<Property> stayProperty = propertyRepository.findById(propertyId);
        if (stayProperty.isEmpty() || !StayEligibility.hasStarted(stayProperty.get())) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can review this property once your stay has started.");
            return "redirect:/property/" + propertyId;
        }

        if (reviewRepository.findByStudentIDAndPropertyID(studentId, propertyId).isPresent()) {
            redirectAttributes.addFlashAttribute("actionError", "You've already submitted a review for this property.");
            return "redirect:/property/" + propertyId;
        }

        if (!propertyId.equals(currentStayPropertyId(studentId))) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can only review the place you're currently staying at.");
            return "redirect:/property/" + propertyId;
        }
        Review review = new Review();
        review.setStudentID(studentId);
        review.setPropertyID(propertyId);
        review.setRating(rating);
        review.setComment(cleanComment);
        review.setReviewDate(LocalDateTime.now());
        review.setCleanlinessRating(cleanlinessRating);
        review.setSafetyRating(safetyRating);
        review.setWifiRating(wifiRating);
        review.setStudyAreaRating(studyAreaRating);
        review.setResidencyStatus(
                "Current".equals(residencyStatus) || "Past".equals(residencyStatus) ? residencyStatus : null);
        review.setYearOfStudy(yearOfStudy != null && yearOfStudy >= 1 && yearOfStudy <= 6 ? yearOfStudy : null);
        review.setIsReported(false);
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("actionMessage", "Your review has been submitted. Thank you!");
        return "redirect:/my-stay";
    }
}