package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.ulee.ulee_backend.repository.PropertyRepository;
import com.ulee.ulee_backend.repository.PropertyImageRepository;
import com.ulee.ulee_backend.repository.ReviewRepository;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ulee.ulee_backend.repository.UserRepository;
import com.ulee.ulee_backend.repository.StudentRepository;
import com.ulee.ulee_backend.repository.LandlordRepository;
import com.ulee.ulee_backend.repository.ApplicationRepository;
import com.ulee.ulee_backend.repository.ReportRepository;


@Controller
public class AdminController {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LandlordRepository landlordRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private com.ulee.ulee_backend.repository.NotificationRepository notificationRepository;

    /**
     * Populates the counts every admin page's sidebar badges need
     * (Review Properties, Reported, Reviews) so they stay consistent
     * across pages instead of drifting per-controller-method.
     */
    private void addSidebarCounts(Model model) {
        model.addAttribute("totalPending", propertyRepository.findByStatus("Pending").size());
        model.addAttribute("totalReported", propertyRepository.findByIsReportedTrue().size());
        model.addAttribute("totalReviews", reviewRepository.findAll().size());
    }

    @GetMapping("/admin/listings")
    public String viewAllListings(Model model, @RequestParam(required = false) String search) {
        List<Property> allProperties = propertyRepository.findAll();

        String searchLower = search != null ? search.trim().toLowerCase() : null;
        List<Property> filtered = allProperties.stream()
                .filter(p -> searchLower == null || searchLower.isBlank()
                        || (p.getCity() != null && p.getCity().toLowerCase().contains(searchLower))
                        || (p.getTitle() != null && p.getTitle().toLowerCase().contains(searchLower))
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(searchLower))
                        || (p.getSuburb() != null && p.getSuburb().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());

        // Number of students who applied to each property, for the "Applicants" column
        Map<Integer, Long> applicationCounts = applicationRepository.findAll().stream()
                .filter(a -> a.getPropertyID() != null)
                .collect(Collectors.groupingBy(Application::getPropertyID, Collectors.counting()));

        model.addAttribute("properties", filtered);
        model.addAttribute("totalListings", allProperties.size());
        model.addAttribute("applicationCounts", applicationCounts);
        model.addAttribute("search", search);
        addSidebarCounts(model);
        return "admin/admin-listings";
    }

    @GetMapping("/admin/pending-listings")
    public String viewPendingListings(Model model,
                                      @RequestParam(required = false) Integer academicYear,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false, defaultValue = "1") Integer page,
                                      jakarta.servlet.http.HttpServletResponse response) {
        // Force the browser to never cache this page. We hit a case where
        // navigating here via the sidebar link showed a stale, older version
        // of this page while typing the URL directly showed the current one
        // — classic symptom of the browser serving a cached response instead
        // of re-fetching. This header makes that impossible regardless of
        // how the page was navigated to.
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        List<Property> allPending = propertyRepository.findByStatus("Pending");

        String searchLower = search != null ? search.trim().toLowerCase() : null;
        List<Property> filtered = allPending.stream()
                .filter(p -> academicYear == null
                        || (p.getAvailableFrom() != null && p.getAvailableFrom().getYear() == academicYear))
                .filter(p -> searchLower == null || searchLower.isBlank()
                        || (p.getTitle() != null && p.getTitle().toLowerCase().contains(searchLower))
                        || (p.getCity() != null && p.getCity().toLowerCase().contains(searchLower))
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(searchLower)))
                .sorted(Comparator.comparing(
                        (Property p) -> p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        long flaggedCount = propertyRepository.findByIsReportedTrue().size();

        int currentYear = java.time.Year.now().getValue();
        List<Integer> academicYearOptions = java.util.Arrays.asList(currentYear - 1, currentYear, currentYear + 1);

        // Pagination
        int pageSize = 10;
        int totalPending = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalPending / (double) pageSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalPending);
        int toIndex = Math.min(fromIndex + pageSize, totalPending);
        List<Property> pageProperties = filtered.subList(fromIndex, toIndex);

        // Build row view models: landlord name + a synthetic reference number
        // + a trust label derived from Landlord.verified (this project has no
        // separate "corporate/individual" distinction, so it's binary).
        List<PendingRowView> rows = new ArrayList<>();
        for (Property property : pageProperties) {
            Optional<User> landlordUserOpt = userRepository.findById(property.getLandlordID());
            Optional<Landlord> landlordRecordOpt = landlordRepository.findById(property.getLandlordID());
            boolean verified = landlordRecordOpt.isPresent() && Boolean.TRUE.equals(landlordRecordOpt.get().getVerified());
            String landlordName = landlordUserOpt.map(this::safeName).orElse("Landlord #" + property.getLandlordID());
            String trustLabel = verified ? "Verified Partner" : "Individual Owner";
            int refYear = property.getCreatedAt() != null ? property.getCreatedAt().getYear() : currentYear;
            String ref = "Ref: NMU " + refYear + "-" + String.format("%03d", property.getPropertyID());
            rows.add(new PendingRowView(property, landlordName, trustLabel, ref));
        }

        model.addAttribute("rows", rows);
        model.addAttribute("totalPendingAll", allPending.size());
        model.addAttribute("flaggedCount", flaggedCount);
        model.addAttribute("academicYearOptions", academicYearOptions);
        model.addAttribute("selectedAcademicYear", academicYear);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("fromIndex", totalPending == 0 ? 0 : fromIndex + 1);
        model.addAttribute("toIndex", toIndex);
        model.addAttribute("totalPending", totalPending);
        addSidebarCounts(model);
        return "admin/admin-pending-listings";
    }

    /** Read-only row view model for the Review Properties submissions table. */
    public static class PendingRowView {
        private final Property property;
        private final String landlordName;
        private final String trustLabel;
        private final String ref;

        public PendingRowView(Property property, String landlordName, String trustLabel, String ref) {
            this.property = property;
            this.landlordName = landlordName;
            this.trustLabel = trustLabel;
            this.ref = ref;
        }

        public Property getProperty() { return property; }
        public String getLandlordName() { return landlordName; }
        public String getTrustLabel() { return trustLabel; }
        public String getRef() { return ref; }
    }

    @GetMapping("/admin/approved-properties")
    public String viewApprovedProperties(Model model,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(required = false) String city,
                                         @RequestParam(required = false) java.math.BigDecimal minPrice,
                                         @RequestParam(required = false) java.math.BigDecimal maxPrice,
                                         @RequestParam(required = false) Integer bedrooms,
                                         @RequestParam(required = false) Integer academicYear) {

        List<Property> allApproved = propertyRepository.findByStatusIn(List.of("Approved", "Active"));
        // Distinct cities from the full approved set, for the city filter dropdown
        List<String> cityOptions = allApproved.stream()
                .map(Property::getCity)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        String searchLower = search != null ? search.trim().toLowerCase() : null;

        List<Property> filtered = allApproved.stream()
                .filter(p -> searchLower == null || searchLower.isBlank()
                        || (p.getTitle() != null && p.getTitle().toLowerCase().contains(searchLower))
                        || (p.getCity() != null && p.getCity().toLowerCase().contains(searchLower))
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(searchLower)))
                .filter(p -> city == null || city.isBlank() || city.equalsIgnoreCase(p.getCity()))
                .filter(p -> minPrice == null || (p.getRent() != null && p.getRent().compareTo(minPrice) >= 0))
                .filter(p -> maxPrice == null || (p.getRent() != null && p.getRent().compareTo(maxPrice) <= 0))
                .filter(p -> bedrooms == null || (p.getBedrooms() != null && p.getBedrooms().intValue() == bedrooms))
                .collect(Collectors.toList());

        // Selecting an academic year bumps properties available that year to the
        // top, rather than hiding the rest — sort is stable so order within each
        // group (matches / non-matches) is otherwise unchanged.
        final Integer selectedYear = academicYear;
        if (selectedYear != null) {
            filtered.sort(Comparator.comparing(
                    (Property p) -> !(p.getAvailableFrom() != null && p.getAvailableFrom().getYear() == selectedYear)));
        }

        // Headline stats always reflect ALL approved properties, not just the filtered view
        java.util.Set<Integer> approvedIds = allApproved.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toSet());
        long applicantsForApproved = applicationRepository.findAll().stream()
                .filter(a -> a.getPropertyID() != null && approvedIds.contains(a.getPropertyID()))
                .count();
        long approvedCapacity = allApproved.stream()
                .mapToLong(p -> p.getCapacity() != null ? p.getCapacity() : 0)
                .sum();
        double occupancyRate = approvedCapacity > 0 ? (applicantsForApproved * 100.0 / approvedCapacity) : 0;

        YearMonth currentMonth = YearMonth.now();
        long approvedThisMonth = allApproved.stream()
                .filter(p -> p.getCreatedAt() != null && YearMonth.from(p.getCreatedAt()).equals(currentMonth))
                .count();

        // A few selectable academic years around the current one, for the dropdown
        int currentYear = java.time.Year.now().getValue();
        List<Integer> academicYearOptions = java.util.Arrays.asList(currentYear - 1, currentYear, currentYear + 1);

        model.addAttribute("approvedProperties", filtered);
        model.addAttribute("totalApproved", allApproved.size());
        model.addAttribute("occupancyRate", occupancyRate);
        model.addAttribute("approvedThisMonth", approvedThisMonth);
        model.addAttribute("cityOptions", cityOptions);
        model.addAttribute("search", search);
        model.addAttribute("selectedCity", city);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("bedrooms", bedrooms);
        model.addAttribute("totalListings", propertyRepository.findAll().size());
        model.addAttribute("academicYearOptions", academicYearOptions);
        model.addAttribute("selectedAcademicYear", selectedYear != null ? selectedYear : currentYear);
        addSidebarCounts(model);
        return "admin/admin-approved-properties";
    }

    @PostMapping("/admin/approve-listing/{id}")
    public String approveListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin-index";
        }
        Property property = propertyOpt.get();
        property.setStatus("Approved");
        propertyRepository.save(property);
        redirectAttributes.addFlashAttribute("actionMessage", "Approved \"" + property.getTitle() + "\"");
        return "redirect:/admin-index";
    }

    @PostMapping("/admin/reject-listing/{id}")
    public String rejectListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin-index";
        }
        Property property = propertyOpt.get();
        property.setStatus("Rejected");
        propertyRepository.save(property);
        redirectAttributes.addFlashAttribute("actionMessage", "Rejected \"" + property.getTitle() + "\"");
        return "redirect:/admin-index";
    }

    @GetMapping("/admin/reviews")
    public String viewReviews(Model model, @RequestParam(required = false, defaultValue = "1") Integer page) {
        List<Review> allReviews = reviewRepository.findAll();
        List<Property> allProperties = propertyRepository.findAll();

        Map<Integer, Property> propertyLookup = allProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

        // Newest first
        List<Review> sortedReviews = allReviews.stream()
                .sorted(Comparator.comparing(
                        (Review r) -> r.getReviewDate() != null ? r.getReviewDate() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        long reportedCount = allReviews.stream().filter(r -> Boolean.TRUE.equals(r.getIsReported())).count();
        double avgRating = allReviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);

        long totalLandlords = landlordRepository.findAll().size();
        long verifiedLandlords = landlordRepository.countByVerifiedTrue();
        double accreditedPercent = totalLandlords > 0 ? (verifiedLandlords * 100.0 / totalLandlords) : 0;

        // Pagination
        int pageSize = 10;
        int totalReviews = sortedReviews.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalReviews / (double) pageSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalReviews);
        int toIndex = Math.min(fromIndex + pageSize, totalReviews);
        List<Review> pageReviews = sortedReviews.subList(fromIndex, toIndex);

        // Build display-ready cards: anonymized name + stable pseudo-initials
        // derived from studentID (never the student's real name), plus the
        // property this review is for.
        List<ReviewCardView> reviewCards = new ArrayList<>();
        for (Review review : pageReviews) {
            int overallIndex = sortedReviews.indexOf(review) + 1;
            String anonName = "anonymous" + overallIndex;
            String initials = pseudoInitials(review.getStudentID());
            Property property = review.getPropertyID() != null ? propertyLookup.get(review.getPropertyID()) : null;
            String propertyTitle = property != null ? property.getTitle() : "Unknown property";
            reviewCards.add(new ReviewCardView(
                    review.getReviewID(), propertyTitle, anonName, initials,
                    review.getRating() != null ? review.getRating() : 0,
                    review.getComment(), review.getReviewDate(),
                    Boolean.TRUE.equals(review.getIsReported())));
        }

        model.addAttribute("reviewCards", reviewCards);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("reportedCount", reportedCount);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("accreditedPercent", accreditedPercent);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("fromIndex", totalReviews == 0 ? 0 : fromIndex + 1);
        model.addAttribute("toIndex", toIndex);
        addSidebarCounts(model);
        return "admin/admin-reviews";
    }

    /** Two-letter pseudo-initials derived from an ID — visually distinct per reviewer without revealing identity. */
    private String pseudoInitials(Integer id) {
        if (id == null) return "??";
        char a = (char) ('A' + Math.floorMod(id, 26));
        char b = (char) ('A' + Math.floorMod(id * 7 + 3, 26));
        return "" + a + b;
    }

    @PostMapping("/admin/reviews/{id}/mark-resolved")
    public String markReviewResolved(@PathVariable Integer id,
                                     @RequestParam(required = false, defaultValue = "1") Integer page,
                                     RedirectAttributes redirectAttributes) {
        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Review #" + id + " could not be found.");
            return "redirect:/admin/reviews?page=" + page;
        }
        Review review = reviewOpt.get();
        review.setIsReported(false);
        reviewRepository.save(review);
        redirectAttributes.addFlashAttribute("actionMessage", "Review marked resolved.");
        return "redirect:/admin/reviews?page=" + page;
    }

    /** Read-only view model for a single review card on the admin Reviews page. */
    public static class ReviewCardView {
        private final Integer reviewId;
        private final String propertyTitle;
        private final String anonName;
        private final String initials;
        private final int rating;
        private final String comment;
        private final String dateDisplay;
        private final boolean reported;

        public ReviewCardView(Integer reviewId, String propertyTitle, String anonName, String initials,
                              int rating, String comment, LocalDateTime reviewDate, boolean reported) {
            this.reviewId = reviewId;
            this.propertyTitle = propertyTitle;
            this.anonName = anonName;
            this.initials = initials;
            this.rating = rating;
            this.comment = comment;
            this.dateDisplay = reviewDate != null
                    ? reviewDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    : "";
            this.reported = reported;
        }

        public Integer getReviewId() { return reviewId; }
        public String getPropertyTitle() { return propertyTitle; }
        public String getAnonName() { return anonName; }
        public String getInitials() { return initials; }
        public int getRating() { return rating; }
        public String getComment() { return comment; }
        public String getDateDisplay() { return dateDisplay; }
        public boolean isReported() { return reported; }
    }

    @GetMapping("/admin/reported-listings")
    public String viewReportedListings(Model model) {
        List<Property> reportedProperties = propertyRepository.findByIsReportedTrue();
        model.addAttribute("reportedProperties", reportedProperties);
        return "admin/admin-reported-listings";
    }

    @GetMapping("/admin/reported-listing/{id}")
    public String viewReportedListingDetail(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin/reported-listings";
        }
        Property property = propertyOpt.get();
        Optional<User> landlordUserOpt = userRepository.findById(property.getLandlordID());
        Optional<Landlord> landlordRecordOpt = landlordRepository.findById(property.getLandlordID());

        List<Report> reportTimeline = reportRepository.findByPropertyIDOrderByReportedAtAsc(id);
        // Fall back to the legacy single reportReason string if no Report
        // rows exist yet for this property (e.g. it was flagged before the
        // Report table existed).
        if (reportTimeline.isEmpty() && property.getReportReason() != null && !property.getReportReason().isBlank()) {
            Report legacy = new Report();
            legacy.setReason("Reported");
            legacy.setDescription(property.getReportReason());
            legacy.setReportedAt(property.getUpdatedAt() != null ? property.getUpdatedAt() : property.getCreatedAt());
            reportTimeline = List.of(legacy);
        }

        long reportsLast30Days = reportRepository.countByPropertyIDAndReportedAtAfter(id, LocalDateTime.now().minusDays(30));
        boolean landlordVerified = landlordRecordOpt.isPresent() && Boolean.TRUE.equals(landlordRecordOpt.get().getVerified());

        // Pagination across the reported queue (issue #3): lets the admin page
        // "1 2 3 …" straight through every reported property without bouncing
        // back to the list each time. Order matches the Reported list page.
        List<Property> reportedQueue = propertyRepository.findByIsReportedTrue();
        int currentIndex = -1;
        for (int i = 0; i < reportedQueue.size(); i++) {
            if (reportedQueue.get(i).getPropertyID().equals(id)) {
                currentIndex = i;
                break;
            }
        }
        Integer prevId = currentIndex > 0 ? reportedQueue.get(currentIndex - 1).getPropertyID() : null;
        Integer nextId = (currentIndex >= 0 && currentIndex < reportedQueue.size() - 1)
                ? reportedQueue.get(currentIndex + 1).getPropertyID() : null;

        model.addAttribute("property", property);
        model.addAttribute("landlord", landlordUserOpt.orElse(null));
        model.addAttribute("landlordVerified", landlordVerified);
        model.addAttribute("reportTimeline", reportTimeline);
        model.addAttribute("reportsLast30Days", reportsLast30Days);
        model.addAttribute("addressLine", formatAddressLine(property));
        model.addAttribute("reportedQueue", reportedQueue);
        model.addAttribute("currentQueueIndex", currentIndex);
        model.addAttribute("prevReportedId", prevId);
        model.addAttribute("nextReportedId", nextId);
        addSidebarCounts(model);
        return "admin/admin-reported-listing-detail";
    }

    @PostMapping("/admin/warn-landlord-for-listing/{propertyId}")
    public String warnLandlordForListing(@PathVariable Integer propertyId, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(propertyId);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + propertyId + " could not be found.");
            return "redirect:/admin/reported-listings";
        }
        Property property = propertyOpt.get();
        Optional<User> landlordUserOpt = userRepository.findById(property.getLandlordID());
        if (landlordUserOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Landlord for this listing could not be found.");
            return "redirect:/admin/reported-listing/" + propertyId;
        }
        User landlordUser = landlordUserOpt.get();

        // Build the email body from the report reasons/descriptions ONLY —
        // never the reporting student's ID or name — so complainers stay
        // anonymous to the landlord.
        List<Report> reports = reportRepository.findByPropertyIDOrderByReportedAtAsc(propertyId);
        List<String> anonymizedSummaries;
        if (!reports.isEmpty()) {
            anonymizedSummaries = reports.stream()
                    .map(r -> (r.getReason() != null ? r.getReason() + ": " : "") +
                            (r.getDescription() != null ? r.getDescription() : "No details provided"))
                    .collect(Collectors.toList());
        } else {
            anonymizedSummaries = property.getReportReason() != null && !property.getReportReason().isBlank()
                    ? List.of(property.getReportReason())
                    : List.of();
        }

        // Build the notification body: "Dear <landlord>, this serves as an
        // official warning…" followed by the complaints, complainers left out.
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Dear ").append(safeName(landlordUser)).append(",\n\n");
        messageBuilder.append("This serves as an official warning regarding \"").append(property.getTitle()).append("\". ");
        messageBuilder.append("The following complaint(s) have been raised:\n\n");
        if (anonymizedSummaries.isEmpty()) {
            messageBuilder.append("• A general concern was reported for this listing.\n");
        } else {
            for (String summary : anonymizedSummaries) {
                messageBuilder.append("• ").append(summary).append("\n");
            }
        }
        messageBuilder.append("\nPlease address these issues promptly. Continued unresolved reports may result in suspension of this listing.");

        // Delivery: an in-app notification on the landlord's dashboard. This
        // project has no email service configured (no SMTP settings, and no
        // MailService class exists), so the notification is the sole,
        // reliable delivery mechanism — not a fallback.
        com.ulee.ulee_backend.model.Notification notification = new com.ulee.ulee_backend.model.Notification();
        notification.setLandlordID(landlordUser.getUserID());
        notification.setPropertyID(propertyId);
        notification.setTitle("Official Warning: " + property.getTitle());
        notification.setMessage(messageBuilder.toString());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);
        notificationRepository.save(notification);

        int current = landlordUser.getWarningCount() != null ? landlordUser.getWarningCount() : 0;
        landlordUser.setWarningCount(current + 1);
        userRepository.save(landlordUser);
        redirectAttributes.addFlashAttribute("actionMessage",
                "Official warning sent to " + safeName(landlordUser) + "'s notifications (warning #" + (current + 1) + ")");
        return "redirect:/admin/reported-listing/" + propertyId;
    }

    @PostMapping("/admin/suspend-listing/{id}")
    public String suspendListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin/reported-listings";
        }
        Property property = propertyOpt.get();
        property.setStatus("Suspended");
        property.setIsAvailable(false);
        propertyRepository.save(property);
        redirectAttributes.addFlashAttribute("actionMessage", "Suspended \"" + property.getTitle() + "\"");
        return "redirect:/admin/reported-listings";
    }

    @PostMapping("/admin/remove-listing/{id}")
    public String removeListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin/reported-listings";
        }
        String title = propertyOpt.get().getTitle();

        try {
            reportRepository.deleteAll(reportRepository.findByPropertyIDOrderByReportedAtAsc(id));
            reviewRepository.deleteAll(reviewRepository.findByPropertyID(id));
            propertyImageRepository.deleteAll(propertyImageRepository.findByPropertyID(id));
            // add applicationRepository / savedPropertyRepository cleanup here too if those repos exist

            propertyRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("actionMessage", "Removed listing \"" + title + "\"");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("actionError", "Could not remove \"" + title + "\": it has related records (reports, reviews, or applications) that must be cleared first.");
        }

        return "redirect:/admin/reported-listings";
    }

    @GetMapping("/admin-index")
    public String viewAdminDashboard(Model model,
                                     @RequestParam(required = false) Integer academicYear,
                                     @RequestParam(required = false) String search) {
        List<Property> allProperties = propertyRepository.findAll();
        List<Review> allReviews = reviewRepository.findAll();
        List<Property> pendingProperties = propertyRepository.findByStatus("Pending");
        // "Approved" and "Active" both count as approved, same as the
        // Approved Properties page — otherwise listings that have moved to
        // "Active" get silently dropped from this count.
        List<Property> approvedProperties = propertyRepository.findByStatusIn(List.of("Approved", "Active"));
        List<Property> reportedProperties = propertyRepository.findByIsReportedTrue();

        // "Filter All" on the Review Properties header narrows the pending
        // queue down to listings available in the selected academic year,
        // and/or matching the search box (property name, city, or address).
        int currentYear = java.time.Year.now().getValue();
        List<Integer> academicYearOptions = java.util.Arrays.asList(currentYear - 1, currentYear, currentYear + 1);
        String searchLower = search != null ? search.trim().toLowerCase() : null;
        List<Property> filteredPendingProperties = pendingProperties.stream()
                .filter(p -> academicYear == null
                        || (p.getAvailableFrom() != null && p.getAvailableFrom().getYear() == academicYear))
                .filter(p -> searchLower == null || searchLower.isBlank()
                        || (p.getTitle() != null && p.getTitle().toLowerCase().contains(searchLower))
                        || (p.getCity() != null && p.getCity().toLowerCase().contains(searchLower))
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());

        Map<Integer, Property> propertyLookup = allProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

        // Build a landlord lookup so the pending-listings table can show names
        Map<Integer, User> landlordLookup = new HashMap<>();
        for (Property p : pendingProperties) {
            if (p.getLandlordID() != null && !landlordLookup.containsKey(p.getLandlordID())) {
                userRepository.findById(p.getLandlordID()).ifPresent(u -> landlordLookup.put(p.getLandlordID(), u));
            }
        }

        model.addAttribute("properties", allProperties);
        model.addAttribute("reviews", allReviews);
        model.addAttribute("propertyLookup", propertyLookup);
        model.addAttribute("pendingProperties", filteredPendingProperties);
        model.addAttribute("reportedProperties", reportedProperties);
        model.addAttribute("landlordLookup", landlordLookup);
        model.addAttribute("totalListings", allProperties.size());
        model.addAttribute("totalReviews", allReviews.size());
        model.addAttribute("totalPending", pendingProperties.size());
        model.addAttribute("totalReported", reportedProperties.size());
        model.addAttribute("totalApproved", approvedProperties.size());
        model.addAttribute("totalVerifiedLandlords", landlordRepository.countByVerifiedTrue());
        model.addAttribute("academicYearOptions", academicYearOptions);
        model.addAttribute("selectedAcademicYear", academicYear);
        model.addAttribute("reviewSearch", search);
        Map<YearMonth, Long> monthlyCounts = buildMonthlyListingCounts(allProperties);
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MMM");
        model.addAttribute("monthlyLabels", monthlyCounts.keySet().stream().map(m -> m.format(labelFormat)).collect(Collectors.toList()));
        model.addAttribute("monthlyCounts", new ArrayList<>(monthlyCounts.values()));
        model.addAttribute("recentActivity", buildRecentActivity(allProperties));

        return "admin/admin-index";
    }

    /**
     * Counts listings created per month for the last 6 months (oldest first),
     * for the Dashboard's "Listings Trend" chart. Returns a LinkedHashMap so
     * insertion order (oldest -> newest) is preserved for the caller.
     */
    private Map<YearMonth, Long> buildMonthlyListingCounts(List<Property> allProperties) {
        YearMonth currentMonth = YearMonth.now();

        // Pre-fill the last 6 months (oldest -> newest) with zero counts
        Map<YearMonth, Long> counts = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            counts.put(currentMonth.minusMonths(i), 0L);
        }

        for (Property p : allProperties) {
            LocalDateTime createdAt = p.getCreatedAt();
            if (createdAt == null) continue;
            YearMonth month = YearMonth.from(createdAt);
            if (counts.containsKey(month)) {
                counts.merge(month, 1L, Long::sum);
            }
        }

        return counts;
    }

    /**
     * Builds a simple "recent activity" feed from the most recently
     * created/updated listings, for the Dashboard.
     */
    private List<ActivityItem> buildRecentActivity(List<Property> allProperties) {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a");

        return allProperties.stream()
                .filter(p -> p.getCreatedAt() != null || p.getUpdatedAt() != null)
                .sorted(Comparator.comparing(
                        (Property p) -> p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt(),
                        Comparator.reverseOrder()))
                .limit(5)
                .map(p -> {
                    LocalDateTime when = p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt();
                    String status = p.getStatus() != null ? p.getStatus() : "Pending";
                    String message = "\"" + p.getTitle() + "\" — " + status;
                    return new ActivityItem(message, when.format(timeFormat));
                })
                .collect(Collectors.toList());
    }

    /** Small holder for a single row in the Dashboard's recent-activity feed. */
    public static class ActivityItem {
        private final String message;
        private final String timestamp;

        public ActivityItem(String message, String timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }
    }

    @GetMapping("/admin/listing/{id}")
    public String viewListingDetail(@PathVariable Integer id, Model model) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);

        if (propertyOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Listing #" + id + " could not be found. It may have been removed.");
            return "admin/admin-listing-not-found";
        }

        Property property = propertyOpt.get();
        List<String> validationIssues = validateListing(property);
        Optional<User> landlordUserOpt = userRepository.findById(property.getLandlordID());

        model.addAttribute("property", property);
        model.addAttribute("images", propertyImageRepository.findByPropertyID(id));
        model.addAttribute("landlord", landlordUserOpt.orElse(null));
        model.addAttribute("reviews", reviewRepository.findByPropertyID(id));
        model.addAttribute("validationIssues", validationIssues);
        model.addAttribute("isValid", validationIssues.isEmpty());
        model.addAttribute("totalListings", propertyRepository.findAll().size());
        addSidebarCounts(model);
        return "admin/admin-listing-details";
    }

    /** Joins address, suburb, city into one display line, skipping any that are blank. */
    private String formatAddressLine(Property property) {
        List<String> parts = new ArrayList<>();
        if (property.getAddress() != null && !property.getAddress().isBlank()) parts.add(property.getAddress());
        if (property.getSuburb() != null && !property.getSuburb().isBlank()) parts.add(property.getSuburb());
        if (property.getCity() != null && !property.getCity().isBlank()) parts.add(property.getCity());
        return String.join(", ", parts);
    }

    private List<String> validateListing(Property property) {
        List<String> issues = new ArrayList<>();

        if (property.getTitle() == null || property.getTitle().isBlank()) issues.add("Missing title");
        if (property.getAddress() == null || property.getAddress().isBlank()) issues.add("Missing address");
        if (property.getCity() == null || property.getCity().isBlank()) issues.add("Missing city");
        if (property.getRent() == null) issues.add("Missing rent amount");
        if (property.getBedrooms() == null) issues.add("Missing bedroom count");
        if (property.getBathrooms() == null) issues.add("Missing bathroom count");
        if (property.getDescription() == null || property.getDescription().isBlank()) issues.add("Missing description");
        if (propertyImageRepository.findByPropertyID(property.getPropertyID()).isEmpty()) issues.add("No property images uploaded");

        return issues;
    }

    private String safeName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "user #" + user.getUserID() : full;
    }

    @GetMapping("/admin/manage-users")
    public String manageUsers(Model model) {
        List<Student> students = studentRepository.findAll();
        List<Landlord> landlords = landlordRepository.findAll();

        List<User> studentUsers = new ArrayList<>();
        for (Student s : students) {
            userRepository.findById(s.getStudentID()).ifPresent(studentUsers::add);
        }

        List<User> landlordUsers = new ArrayList<>();
        for (Landlord l : landlords) {
            userRepository.findById(l.getLandlordID()).ifPresent(landlordUsers::add);
        }

        model.addAttribute("studentUsers", studentUsers);
        model.addAttribute("landlordUsers", landlordUsers);
        model.addAttribute("totalUsers", studentUsers.size() + landlordUsers.size());
        return "admin/admin-manage-users";
    }

    @PostMapping("/admin/warn-user/{id}")
    public String warnUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "User #" + id + " could not be found.");
            return "redirect:/admin/manage-users";
        }
        User user = userOpt.get();
        int current = user.getWarningCount() != null ? user.getWarningCount() : 0;
        user.setWarningCount(current + 1);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("actionMessage", "Warned " + safeName(user) + " (warning #" + (current + 1) + ")");
        return "redirect:/admin/manage-users";
    }

    @GetMapping("/admin/edit-user/{id}")
    public String editUserForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "User #" + id + " could not be found.");
            return "redirect:/admin/manage-users";
        }
        model.addAttribute("user", userOpt.get());
        return "admin/admin-edit-user";
    }

    @PostMapping("/admin/edit-user/{id}")
    public String editUserSave(@PathVariable Integer id,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam String phone,
                               RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "User #" + id + " could not be found.");
            return "redirect:/admin/manage-users";
        }
        User user = userOpt.get();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("actionMessage", "Updated " + safeName(user));
        return "redirect:/admin/manage-users";
    }

    @PostMapping("/admin/deactivate-user/{id}")
    public String deactivateUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "User #" + id + " could not be found.");
            return "redirect:/admin/manage-users";
        }
        User user = userOpt.get();
        user.setIsActive(false);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("actionMessage", "Deactivated " + safeName(user));
        return "redirect:/admin/manage-users";
    }

    @PostMapping("/admin/reactivate-user/{id}")
    public String reactivateUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "User #" + id + " could not be found.");
            return "redirect:/admin/manage-users";
        }
        User user = userOpt.get();
        user.setIsActive(true);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("actionMessage", "Reactivated " + safeName(user));
        return "redirect:/admin/manage-users";
    }

    @PostMapping("/admin/delete-user/{id}")
    public String deleteUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "User #" + id + " could not be found.");
            return "redirect:/admin/manage-users";
        }
        String name = safeName(userOpt.get());
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
        }
        if (landlordRepository.existsById(id)) {
            landlordRepository.deleteById(id);
        }
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("actionMessage", "Deleted " + name);
        return "redirect:/admin/manage-users";
    }

}



