package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import com.ulee.ulee_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    // A500 — show the review form for a specific property
    // Alternative flow: student not accepted for this property
    // Alternative flow: student already reviewed this property
    @GetMapping("/write-review/{propertyId}")
    public String showReviewForm(@PathVariable Integer propertyId, Model model,
                                 Authentication authentication, RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Could not identify your account. Please log in again.");
            return "redirect:/student-dashboard";
        }
        Integer studentId = userOpt.get().getUserID();

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

        boolean alreadyReviewed = reviewRepository
                .findByStudentIDAndPropertyID(studentId, propertyId)
                .isPresent();

        if (alreadyReviewed) {
            redirectAttributes.addFlashAttribute("actionError", "You've already submitted a review for this property.");
            return "redirect:/property/" + propertyId;
        }

        model.addAttribute("property", propertyOpt.get());
        return "student/write-review";
    }

    // A500 — submit the review
    // Alternative flow: rating out of range
    // Alternative flow: not accepted / duplicate (re-checked server-side, not just trusted from the form)
    @PostMapping("/submit-review")
    public String submitReview(@RequestParam Integer propertyId,
                               @RequestParam Integer rating,
                               @RequestParam String comment,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Could not identify your account. Please log in again.");
            return "redirect:/student-dashboard";
        }
        Integer studentId = userOpt.get().getUserID();

        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("actionError", "Rating must be between 1 and 5.");
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

        if (reviewRepository.findByStudentIDAndPropertyID(studentId, propertyId).isPresent()) {
            redirectAttributes.addFlashAttribute("actionError", "You've already submitted a review for this property.");
            return "redirect:/property/" + propertyId;
        }

        Review review = new Review();
        review.setStudentID(studentId);
        review.setPropertyID(propertyId);
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewDate(LocalDateTime.now());
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("actionMessage", "Your review has been submitted. Thank you!");
        return "redirect:/property/" + propertyId;
    }
}