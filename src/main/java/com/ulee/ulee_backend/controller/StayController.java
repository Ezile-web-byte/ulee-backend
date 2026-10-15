package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import com.ulee.ulee_backend.repository.*;
import com.ulee.ulee_backend.service.StayEligibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * "My Stay" for students: the hub page, plus reporting an issue.
 * Reports are saved with your existing Report entity, so the admin's
 * "Handle Reported Property" page sees them. (Reviews are in ReviewController.)
 *
 * STAY LOCK: once a student has reviewed or reported a property, we assume
 * they live there, so their OTHER accepted properties are locked (no review,
 * no report). See stayingPropertyIds() / isLockedOut() below.
 */
@Controller
public class StayController {

    // key, label (saved into Report.reason), icon, hint
    private static final List<String[]> REPORT_CATEGORIES = List.of(
            new String[]{"maintenance", "Maintenance", "build", "Broken fixtures, leaks, damp, appliances"},
            new String[]{"safety", "Safety", "shield", "Locks, lighting, fire safety, break-ins"},
            new String[]{"cleanliness", "Cleanliness & pests", "cleaning_services", "Dirty shared areas, pests, rubbish"},
            new String[]{"utilities", "Wi-Fi & utilities", "wifi", "Internet, water or electricity problems"},
            new String[]{"landlord", "Landlord conduct", "gavel", "Unfair charges, no response, privacy"},
            new String[]{"other", "Something else", "help", "Anything not listed here"});

    // Shown whenever a locked property is opened or submitted.
    private static final String LOCKED_MESSAGE =
            "You have already reviewed or reported another property, so we assume you live there. "
                    + "You can only review or report issues at that property.";

    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PropertyImageRepository propertyImageRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private SavedPropertyRepository savedPropertyRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    private String initialsOf(User u) {
        String last = u.getLastName();
        return ("" + u.getFirstName().charAt(0)
                + (last != null && !last.isBlank() ? last.charAt(0) : "")).toUpperCase();
    }

    private boolean isAccepted(Integer studentId, Integer propertyId) {
        return applicationRepository.findByStudentIDAndPropertyID(studentId, propertyId).stream()
                .anyMatch(a -> "Accepted".equalsIgnoreCase(a.getStatus()));
    }

    // ── STAY LOCK ──────────────────────────────────────────────────────
    // The properties this student has already reviewed or reported. Reviewing
    // or reporting a property means we assume they are staying there.
    // (If old data has activity on more than one property, all of those stay
    // open, so nothing the student already did is taken away.)
    private Set<Integer> stayingPropertyIds(Integer studentId) {
        Set<Integer> ids = new LinkedHashSet<>();
        reviewRepository.findByStudentID(studentId).forEach(r -> {
            if (r.getPropertyID() != null) ids.add(r.getPropertyID());
        });
        reportRepository.findByStudentID(studentId).forEach(r -> {
            if (r.getPropertyID() != null) ids.add(r.getPropertyID());
        });
        return ids;
    }

    // True when the student already has a "home" property and this is not it.
    private boolean isLockedOut(Integer studentId, Integer propertyId) {
        Set<Integer> staying = stayingPropertyIds(studentId);
        return !staying.isEmpty() && !staying.contains(propertyId);
    }

    // ---------- My Stay hub ----------
    @GetMapping("/my-stay")
    public String viewMyStay(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        Integer studentId = user.getUserID();

        List<Application> applications = applicationRepository.findByStudentID(studentId);
        Set<Integer> acceptedIds = applications.stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                .map(Application::getPropertyID)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Review> myReviews = reviewRepository.findByStudentID(studentId).stream()
                .sorted(Comparator.comparing(Review::getReviewDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        List<Report> myReports = reportRepository.findByStudentID(studentId).stream()
                .sorted(Comparator.comparing(Report::getReportedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // propertyID -> this student's review of it, so My Stay can show the
        // landlord's reply (review.landlordResponse / review.responseDate).
        // myReviews is newest first, so putIfAbsent keeps the newest review per property.
        Map<Integer, Review> reviewByProperty = new LinkedHashMap<>();
        for (Review r : myReviews) {
            if (r.getPropertyID() != null) {
                reviewByProperty.putIfAbsent(r.getPropertyID(), r);
            }
        }

        // STAY LOCK: where the student stays, and which accepted places are locked.
        Set<Integer> stayingIds = stayingPropertyIds(studentId);
        Set<Integer> lockedIds = new LinkedHashSet<>();
        if (!stayingIds.isEmpty()) {
            for (Integer id : acceptedIds) {
                if (!stayingIds.contains(id)) lockedIds.add(id);
            }
        }

        // One lookup of every property mentioned on the page
        Set<Integer> allIds = new LinkedHashSet<>(acceptedIds);
        myReviews.forEach(r -> allIds.add(r.getPropertyID()));
        myReports.forEach(r -> allIds.add(r.getPropertyID()));

        Map<Integer, Property> propertyLookup = propertyRepository.findAllById(allIds).stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p, (a, b) -> a));
        Map<Integer, String> imageLookup = propertyImageRepository.findByPropertyIDIn(new ArrayList<>(allIds)).stream()
                .collect(Collectors.toMap(PropertyImage::getPropertyID, PropertyImage::getUrl, (a, b) -> a));

        List<Property> stays = acceptedIds.stream()
                .map(propertyLookup::get).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Integer> reviewedIds = myReviews.stream().map(Review::getPropertyID).collect(Collectors.toSet());
        Set<Integer> reviewableIds = stays.stream()
                .filter(StayEligibility::hasStarted)
                .map(Property::getPropertyID)
                .collect(Collectors.toSet());
        model.addAttribute("reviewableIds", reviewableIds);

        model.addAttribute("stays", stays);
        model.addAttribute("myReviews", myReviews);
        model.addAttribute("myReports", myReports);
        model.addAttribute("propertyLookup", propertyLookup);
        model.addAttribute("imageLookup", imageLookup);
        model.addAttribute("reviewedIds", reviewedIds);
        model.addAttribute("reviewByProperty", reviewByProperty);
        model.addAttribute("stayingIds", stayingIds);   // NEW — properties the student stays at
        model.addAttribute("lockedIds", lockedIds);     // NEW — accepted properties that are locked

        // Sidebar / top bar
        model.addAttribute("userInitials", initialsOf(user));
        model.addAttribute("totalApplications", applications.size());
        model.addAttribute("acceptedCount", acceptedIds.size());
        model.addAttribute("savedCount", savedPropertyRepository.findByStudentID(studentId).size());
        return "student/my-stay";
    }

    // ---------- Report an issue: form ----------
    @GetMapping("/report-issue/{propertyId}")
    public String showReportForm(@PathVariable Integer propertyId, Model model,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);

        Optional<Property> propertyOpt = propertyRepository.findById(propertyId);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "That property could not be found.");
            return "redirect:/my-stay";
        }
        if (!isAccepted(user.getUserID(), propertyId)) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can only report issues at a property you've been accepted to.");
            return "redirect:/my-stay";
        }
        // STAY LOCK: already reviewed/reported somewhere else
        if (isLockedOut(user.getUserID(), propertyId)) {
            redirectAttributes.addFlashAttribute("actionError", LOCKED_MESSAGE);
            return "redirect:/my-stay";
        }

        model.addAttribute("property", propertyOpt.get());
        model.addAttribute("imageUrl", propertyImageRepository.findByPropertyID(propertyId).stream()
                .findFirst().map(PropertyImage::getUrl).orElse(null));
        model.addAttribute("categories", REPORT_CATEGORIES);
        model.addAttribute("userInitials", initialsOf(user));
        return "student/report-issue";
    }

    // ---------- Report an issue: save into the reports table ----------
    @PostMapping("/submit-report")
    public String submitReport(@RequestParam Integer propertyId,
                               @RequestParam String category,
                               @RequestParam String details,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        Integer studentId = getCurrentUser(principal).getUserID();

        String label = REPORT_CATEGORIES.stream()
                .filter(c -> c[0].equals(category)).map(c -> c[1]).findFirst().orElse(null);
        if (label == null) {
            redirectAttributes.addFlashAttribute("actionError", "Please choose what kind of issue it is.");
            return "redirect:/report-issue/" + propertyId;
        }

        String cleanDetails = details == null ? "" : details.trim();
        if (cleanDetails.length() < 10) {
            redirectAttributes.addFlashAttribute("actionError", "Please describe the problem in a bit more detail.");
            return "redirect:/report-issue/" + propertyId;
        }

        if (!isAccepted(studentId, propertyId)) {
            redirectAttributes.addFlashAttribute("actionError",
                    "You can only report issues at a property you've been accepted to.");
            return "redirect:/my-stay";
        }

        // STAY LOCK: also enforced here, so a hand-made POST can't get round it
        if (isLockedOut(studentId, propertyId)) {
            redirectAttributes.addFlashAttribute("actionError", LOCKED_MESSAGE);
            return "redirect:/my-stay";
        }

        Report report = new Report();
        report.setPropertyID(propertyId);
        report.setStudentID(studentId);
        report.setReason(label);
        report.setDescription(cleanDetails);
        report.setReportedAt(LocalDateTime.now());
        reportRepository.save(report);

        // The admin's Reported Listings page treats "Unhandled" as
        // property.isReported == true, so flag the property too. (Warn or
        // Suspend by an admin clears the flag again.)
        propertyRepository.findById(propertyId).ifPresent(p -> {
            String summary = label + ": " + cleanDetails;
            p.setIsReported(true);
            p.setReportReason(summary.length() > 240 ? summary.substring(0, 240) : summary);
            propertyRepository.save(p);
        });

        redirectAttributes.addFlashAttribute("actionMessage", "Your report has been sent. Thank you for letting us know.");
        return "redirect:/my-stay";
    }
}