package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import org.springframework.transaction.annotation.Transactional;
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
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.ulee.ulee_backend.repository.NotificationRepository notificationRepository;

    @Autowired
    private com.ulee.ulee_backend.repository.AdminNotificationReadRepository adminNotificationReadRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.ulee.ulee_backend.repository.AdminActivityLogRepository adminActivityLogRepository;

    /**
     * Runs before every request handled by this controller and injects the
     * currently logged-in admin's display name + initials into the model, so
     * every template (sidebar footer, dashboard greeting, etc.) can use
     * ${adminName} / ${adminInitials} instead of a hardcoded "Sarah Admin".
     * principal.getName() is the Spring Security username, which
     * CustomUserDetailsService builds from the user's email — so we look the
     * User row up by email here.
     */
    @ModelAttribute
    public void addCurrentAdmin(Model model, java.security.Principal principal) {
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("adminName", safeName(user));
                model.addAttribute("adminInitials", initialsFor(user));
            });
        }
    }

    /**
     * Populates the counts every admin page's sidebar badges need
     * (Review Properties, Reported, Reviews) so they stay consistent
     * across pages instead of drifting per-controller-method.
     */
    /**
     * Resolves the display name of whoever is currently logged in, so a
     * logged activity can be attributed to the real admin who performed it
     * without threading a Principal parameter through every action method
     * below — pulled straight from Spring Security's context instead.
     */
    private String currentAdminName() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return userRepository.findByEmail(auth.getName()).map(this::safeName).orElse(auth.getName());
        }
        return "Admin";
    }

    /**
     * Records one real admin action for the Dashboard's Recent Activity
     * feed. This replaces the old approach (inferring "activity" from a
     * property's updatedAt timestamp), which showed unrelated data changes
     * — including ones from seed data — as if an admin had just acted on
     * them, and never reflected user/review actions at all.
     */
    private void logActivity(String action, String message) {
        com.ulee.ulee_backend.model.AdminActivityLog log = new com.ulee.ulee_backend.model.AdminActivityLog();
        log.setActorName(currentAdminName());
        log.setAction(action);
        log.setMessage(message);
        log.setTimestamp(LocalDateTime.now());
        adminActivityLogRepository.save(log);
    }

    /** Two-letter initials from an admin's name, for the activity feed avatar. */
    private String initialsFromName(String name) {
        if (name == null || name.isBlank()) return "AD";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() >= 2) break;
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        }
        return sb.length() > 0 ? sb.toString() : "AD";
    }

    private void addSidebarCounts(Model model) {
        model.addAttribute("totalPending", propertyRepository.findByStatus("Pending").size());
        model.addAttribute("totalReported", propertyRepository.findByIsReportedTrue().size());
        model.addAttribute("totalReviews", reviewRepository.findAll().size());
        // Was previously missing from here, so the sidebar's "Listings" badge
        // only showed the real count on pages whose own controller method
        // happened to add totalListings manually (Listings, Approved
        // Properties, Dashboard, Listing Detail) and silently fell back to 0
        // everywhere else (Review Properties, Reviews, Reported, Manage
        // Users) via the template's ${totalListings} ?: 0. Centralizing it
        // here makes it consistent on every page that calls this method.
        model.addAttribute("totalListings", propertyRepository.findAll().size());
    }

    private List<NotificationFeedItem> buildNotificationFeed() {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a");
        List<NotificationFeedItem> items = new ArrayList<>();

        // New listings submitted
        for (Property p : propertyRepository.findByStatus("Pending")) {
            if (p.getCreatedAt() == null) continue;
            items.add(new NotificationFeedItem(
                    "listing-" + p.getPropertyID(),
                    "listing",
                    "New listing submitted: \"" + p.getTitle() + "\"",
                    p.getCreatedAt(),
                    p.getCreatedAt().format(timeFormat),
                    "/admin/listing/" + p.getPropertyID() + "?from=pending"));
        }

        // Reports filed against listings.
        // ASSUMPTION: Report's own primary key getter is getReportID(),
        // matching this codebase's <Entity>ID convention (propertyID,
        // reviewID, userID). Nothing else in this controller looks Report
        // up by its own id, so if your entity names it differently, this is
        // the one line to fix.
        for (Report r : reportRepository.findAll()) {
            if (r.getReportedAt() == null || r.getPropertyID() == null) continue;
            String title = propertyRepository.findById(r.getPropertyID())
                    .map(Property::getTitle).orElse("a listing");
            String reasonSuffix = (r.getReason() != null && !r.getReason().isBlank()) ? " — " + r.getReason() : "";
            items.add(new NotificationFeedItem(
                    "report-" + r.getReportID(),
                    "report",
                    "New report filed on \"" + title + "\"" + reasonSuffix,
                    r.getReportedAt(),
                    r.getReportedAt().format(timeFormat),
                    "/admin/reported-listing/" + r.getPropertyID()));
        }

        // Official warnings sent (covers both the listing-specific warning in
        // warnLandlordForListing() and the account-level one in warnUser() —
        // both set a title containing "Warning").
        // ASSUMPTION: same as above, but Notification -> getNotificationID().
        for (com.ulee.ulee_backend.model.Notification n : notificationRepository.findAll()) {
            if (n.getCreatedAt() == null || n.getTitle() == null
                    || !n.getTitle().toLowerCase().contains("warning")) continue;
            items.add(new NotificationFeedItem(
                    "warning-" + n.getNotificationID(),
                    "warning",
                    n.getTitle(),
                    n.getCreatedAt(),
                    n.getCreatedAt().format(timeFormat),
                    n.getPropertyID() != null ? "/admin/reported-listing/" + n.getPropertyID() : null));
        }

        items.sort(Comparator.comparing(NotificationFeedItem::getTimestamp, Comparator.reverseOrder()));
        List<NotificationFeedItem> trimmed = items.size() > 10 ? items.subList(0, 10) : items;

        // Mark which of these this admin has already clicked/read
        List<String> ids = trimmed.stream().map(NotificationFeedItem::getId).collect(Collectors.toList());
        java.util.Set<String> readIds = adminNotificationReadRepository.findByEventKeyIn(ids).stream()
                .map(com.ulee.ulee_backend.model.AdminNotificationRead::getEventKey)
                .collect(Collectors.toSet());
        for (NotificationFeedItem item : trimmed) {
            item.setRead(readIds.contains(item.getId()));
        }

        return trimmed;
    }

    /** Read-only row for the notification bell dropdown (see buildNotificationFeed). */
    public static class NotificationFeedItem {
        private final String id;
        private final String type;
        private final String message;
        private final LocalDateTime timestamp;
        private final String timeDisplay;
        private final String link;
        private boolean read;

        public NotificationFeedItem(String id, String type, String message, LocalDateTime timestamp, String timeDisplay, String link) {
            this.id = id;
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
            this.timeDisplay = timeDisplay;
            this.link = link;
            this.read = false;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        @com.fasterxml.jackson.annotation.JsonIgnore
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getTimeDisplay() { return timeDisplay; }
        public String getLink() { return link; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
    }

    @GetMapping("/admin/notifications")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> getAdminNotifications() {
        List<NotificationFeedItem> feed = buildNotificationFeed();
        long unreadCount = feed.stream().filter(item -> !item.isRead()).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notifications", feed);
        response.put("unreadCount", unreadCount);
        return response;
    }

    @PostMapping("/admin/notifications/{eventKey}/read")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> markAdminNotificationRead(@PathVariable String eventKey) {
        if (!adminNotificationReadRepository.existsByEventKey(eventKey)) {
            com.ulee.ulee_backend.model.AdminNotificationRead read = new com.ulee.ulee_backend.model.AdminNotificationRead();
            read.setEventKey(eventKey);
            read.setReadAt(LocalDateTime.now());
            adminNotificationReadRepository.save(read);
        }
        return Map.of("success", true);
    }

    @PostMapping("/admin/notifications/read-all")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> markAllAdminNotificationsRead() {
        // Marks everything currently in the feed as read, not just the top 10
        // shown in buildNotificationFeed()'s trimmed result — recompute
        // untrimmed so "mark all as read" really means all.
        for (NotificationFeedItem item : buildNotificationFeed()) {
            if (!adminNotificationReadRepository.existsByEventKey(item.getId())) {
                com.ulee.ulee_backend.model.AdminNotificationRead read = new com.ulee.ulee_backend.model.AdminNotificationRead();
                read.setEventKey(item.getId());
                read.setReadAt(LocalDateTime.now());
                adminNotificationReadRepository.save(read);
            }
        }
        return Map.of("success", true);
    }

    @GetMapping("/admin/listings")
    public String viewAllListings(Model model,
                                  @RequestParam(required = false) String search,
                                  @RequestParam(required = false) String status) {
        List<Property> allProperties = propertyRepository.findAll();

        String searchLower = search != null ? search.trim().toLowerCase() : null;
        List<Property> filtered = allProperties.stream()
                .filter(p -> searchLower == null || searchLower.isBlank()
                        || (p.getCity() != null && p.getCity().toLowerCase().contains(searchLower))
                        || (p.getTitle() != null && p.getTitle().toLowerCase().contains(searchLower))
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(searchLower))
                        || (p.getSuburb() != null && p.getSuburb().toLowerCase().contains(searchLower)))
                // The template's status filter was previously sending
                // ?status=... but this method never read it at all, so
                // picking a status silently did nothing. "Approved" also
                // matches "Active" here, same as everywhere else in this
                // controller that groups the two together.
                .filter(p -> status == null || status.isBlank()
                        || (status.equalsIgnoreCase("Approved")
                        ? ("Approved".equalsIgnoreCase(p.getStatus()) || "Active".equalsIgnoreCase(p.getStatus()))
                        : status.equalsIgnoreCase(p.getStatus())))
                .collect(Collectors.toList());

        // Number of students who applied to each property, for the "Applicants" column
        Map<Integer, Long> applicationCounts = applicationRepository.findAll().stream()
                .filter(a -> a.getPropertyID() != null)
                .collect(Collectors.groupingBy(Application::getPropertyID, Collectors.counting()));

        model.addAttribute("properties", filtered);
        model.addAttribute("totalListings", allProperties.size());
        model.addAttribute("applicationCounts", applicationCounts);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
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
            return "redirect:/admin/pending-listings";
        }
        Property property = propertyOpt.get();
        // DB only allows Active / Pending / Rejected — there is no
        // "Approved" value. "Approve" is a UI concept only: approving a
        // pending listing means making it live, i.e. Active. The listings
        // page already displays Active as "Approved" and the status filter
        // already maps "Approved" back to Active, so this is the only place
        // that needed to change.
        property.setStatus("Active");
        propertyRepository.save(property);
        logActivity("Approved", "Approved \"" + property.getTitle() + "\"");
        redirectAttributes.addFlashAttribute("actionMessage", "Approved \"" + property.getTitle() + "\"");
        return "redirect:/admin/pending-listings";
    }

    @PostMapping("/admin/reject-listing/{id}")
    public String rejectListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin/pending-listings";
        }
        Property property = propertyOpt.get();
        property.setStatus("Rejected");
        propertyRepository.save(property);
        logActivity("Rejected", "Rejected \"" + property.getTitle() + "\"");
        redirectAttributes.addFlashAttribute("actionMessage", "Rejected \"" + property.getTitle() + "\"");
        return "redirect:/admin/pending-listings";
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
        logActivity("Resolved review", "Marked a review resolved");
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
        addSidebarCounts(model);
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

        // Real reporter names for the admin's own view of the timeline.
        // report.filedByLabel (built on the Report entity) stays anonymized
        // as "Student ID: X" — that's still used as a fallback below for the
        // legacy synthetic row (which has no studentID) and for any report
        // whose student account no longer exists. This lookup is ONLY added
        // to this admin-facing page; warnLandlordForListing() still builds
        // the landlord's notification from reasons/descriptions alone, so
        // complainers stay anonymous to the landlord as before.
        // ASSUMPTION: Report exposes getStudentID(), matching this
        // codebase's <Entity>ID convention (propertyID, reviewID, reportID)
        // — it's what filedByLabel is presumably built from.
        Map<Integer, String> reporterNames = new HashMap<>();
        for (Report r : reportTimeline) {
            if (r.getStudentID() != null && !reporterNames.containsKey(r.getStudentID())) {
                userRepository.findById(r.getStudentID()).ifPresent(u -> reporterNames.put(r.getStudentID(), safeName(u)));
            }
        }

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
        model.addAttribute("reporterNames", reporterNames);
        model.addAttribute("reportsLast30Days", reportsLast30Days);
        model.addAttribute("addressLine", formatAddressLine(property));
        model.addAttribute("reportedQueue", reportedQueue);
        model.addAttribute("currentQueueIndex", currentIndex);
        model.addAttribute("prevReportedId", prevId);
        model.addAttribute("nextReportedId", nextId);
        model.addAttribute("isFlagged", Boolean.TRUE.equals(property.getIsReported()));
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

        // Warning sent = this report is considered handled, so it drops off
        // the Reported queue (same as suspend/remove).
        property.setIsReported(false);
        property.setReportReason(null);
        propertyRepository.save(property);

        logActivity("Warned landlord", "Warned " + safeName(landlordUser) + " about \"" + property.getTitle() + "\"");

        redirectAttributes.addFlashAttribute("actionMessage",
                "Official warning sent to " + safeName(landlordUser) + "'s notifications (warning #" + (current + 1) + ")");
        return "redirect:/admin/reported-listings";
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
        property.setIsReported(false);
        property.setReportReason(null);
        propertyRepository.save(property);
        logActivity("Suspended", "Suspended \"" + property.getTitle() + "\"");
        redirectAttributes.addFlashAttribute("actionMessage", "Suspended \"" + property.getTitle() + "\"");
        return "redirect:/admin/reported-listings";
    }

    @PostMapping("/admin/unsuspend-listing/{id}")
    public String unsuspendListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin/listings";
        }
        Property property = propertyOpt.get();
        if (!"Suspended".equals(property.getStatus())) {
            redirectAttributes.addFlashAttribute("actionError", "\"" + property.getTitle() + "\" is not currently suspended.");
            return "redirect:/admin/listings";
        }
        // Back to Active — the DB has no "Approved" value, only
        // Active/Pending/Rejected. Active is the normal "live/moderated"
        // status, and it will re-appear on /admin/approved-properties
        // immediately (that page already treats Active and Approved the same).
        property.setStatus("Active");
        property.setIsAvailable(true);
        propertyRepository.save(property);
        logActivity("Unsuspended", "Unsuspended \"" + property.getTitle() + "\"");
        redirectAttributes.addFlashAttribute("actionMessage", "Unsuspended \"" + property.getTitle() + "\" — it's live again");
        return "redirect:/admin/listings";
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
            deletePropertyCascade(id);
            logActivity("Removed", "Removed listing \"" + title + "\"");
            redirectAttributes.addFlashAttribute("actionMessage", "Removed listing \"" + title + "\"");
        } catch (Exception e) {
            // NOTE: deliberately not calling setRollbackOnly() here. Doing so
            // while still returning a normal "redirect:" string (rather than
            // letting the exception propagate) makes Spring throw its own
            // UnexpectedRollbackException at the transaction boundary — which
            // happens AFTER this method returns, so it escapes this try/catch
            // entirely and crashes with a blank error page instead of showing
            // the friendly message below. Trade-off: whatever succeeded
            // before the failure stays committed rather than rolling back —
            // preferable to a hard crash with no explanation.
            redirectAttributes.addFlashAttribute("actionError", "Could not remove \"" + title + "\": " + e.getMessage());
        }

        return "redirect:/admin/reported-listings";
    }

    /**
     * Deletes a single property and everything that has an FK pointing at
     * propertyID, in the order the constraints require. Shared by
     * removeListing (single property) and deleteUser (a landlord's entire
     * portfolio), so both go through the exact same safe cascade instead of
     * two copies drifting apart.
     */
    private void deletePropertyCascade(Integer propertyId) {
        reportRepository.deleteAll(reportRepository.findByPropertyIDOrderByReportedAtAsc(propertyId));
        reviewRepository.deleteAll(reviewRepository.findByPropertyID(propertyId));
        propertyImageRepository.deleteAll(propertyImageRepository.findByPropertyID(propertyId));
        applicationRepository.deleteAll(applicationRepository.findByPropertyIDIn(List.of(propertyId)));
        // Remaining tables with an FK to property.propertyID that don't have
        // a Spring Data repository wired into this controller. NOTE: "report"
        // (singular) is a separate legacy table from the "reports" (plural)
        // table ReportRepository maps to — it isn't used anywhere in the app
        // anymore but its FK constraint still blocks deletion if a stray row
        // exists.
        jdbcTemplate.update("DELETE FROM property_amenity WHERE propertyID = ?", propertyId);
        jdbcTemplate.update("DELETE FROM property_feature WHERE propertyID = ?", propertyId);
        jdbcTemplate.update("DELETE FROM savedproperty WHERE propertyID = ?", propertyId);
        jdbcTemplate.update("DELETE FROM report WHERE propertyID = ?", propertyId);
        // Same situation as "report" above: "review" (singular) is a separate
        // legacy table from "reviews" (plural) that ReviewRepository actually
        // maps to. It isn't used anywhere in the app anymore but its FK
        // constraint (review_ibfk_2 on propertyID) still blocks deleting the
        // property if a stray row exists there.
        jdbcTemplate.update("DELETE FROM review WHERE propertyID = ?", propertyId);
        propertyRepository.deleteById(propertyId);
    }

    @GetMapping("/admin-index")
    public String viewAdminDashboard(Model model,
                                     @RequestParam(required = false) Integer academicYear,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) String section,
                                     @RequestParam(required = false) String city,
                                     @RequestParam(required = false) java.math.BigDecimal minPrice,
                                     @RequestParam(required = false) java.math.BigDecimal maxPrice,
                                     @RequestParam(required = false) String type) {
        List<Property> allProperties = propertyRepository.findAll();
        List<Review> allReviews = reviewRepository.findAll();
        List<Property> pendingProperties = propertyRepository.findByStatus("Pending");
        List<Property> approvedProperties = propertyRepository.findByStatusIn(List.of("Approved", "Active"));
        List<Property> reportedProperties = propertyRepository.findByIsReportedTrue();

        // Hero carousel slides 1 & 2: the 2 most recently submitted pending
        // listings, each with a real applicant count for its stat pill.
        List<Property> latestPendingForHero = pendingProperties.stream()
                .sorted(Comparator.comparing(
                        (Property p) -> p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .limit(2)
                .collect(Collectors.toList());
        Map<Integer, Long> heroApplicantCounts = applicationRepository.findAll().stream()
                .filter(a -> a.getPropertyID() != null)
                .collect(Collectors.groupingBy(Application::getPropertyID, Collectors.counting()));
        model.addAttribute("latestPendingForHero", latestPendingForHero);
        model.addAttribute("heroApplicantCounts", heroApplicantCounts);

        int currentYear = java.time.Year.now().getValue();
        List<Integer> academicYearOptions = java.util.Arrays.asList(currentYear - 1, currentYear, currentYear + 1);

        // Distinct cities across ALL pending listings (not the filtered set),
        // so the City dropdown always lists every option regardless of what's
        // currently selected — matches the pattern used on Approved Properties.
        List<String> cityOptions = pendingProperties.stream()
                .map(Property::getCity)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        String searchLower = search != null ? search.trim().toLowerCase() : null;
        List<Property> filteredPendingProperties = pendingProperties.stream()
                .filter(p -> academicYear == null
                        || (p.getAvailableFrom() != null && p.getAvailableFrom().getYear() == academicYear))
                .filter(p -> searchLower == null || searchLower.isBlank()
                        || (p.getTitle() != null && p.getTitle().toLowerCase().contains(searchLower))
                        || (p.getCity() != null && p.getCity().toLowerCase().contains(searchLower))
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(searchLower)))
                .filter(p -> city == null || city.isBlank() || city.equalsIgnoreCase(p.getCity()))
                .filter(p -> minPrice == null || (p.getRent() != null && p.getRent().compareTo(minPrice) >= 0))
                .filter(p -> maxPrice == null || (p.getRent() != null && p.getRent().compareTo(maxPrice) <= 0))
                .filter(p -> type == null || type.isBlank() || type.equalsIgnoreCase(p.getType()))
                .collect(Collectors.toList());

        Map<Integer, Property> propertyLookup = allProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

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
        model.addAttribute("cityOptions", cityOptions);
        model.addAttribute("selectedCity", city);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedType", type);

        // NEW: decide which tab to show server-side, instead of relying on the
        // URL's #hash. Show Review Properties if explicitly requested via
        // ?section=review-properties (sidebar link), OR if a filter was just
        // submitted (search/academicYear present) — both mean the admin was
        // just looking at that tab and expects to land back on it.
        boolean showReviewTab = "review-properties".equals(section)
                || (search != null && !search.isBlank())
                || academicYear != null;
        model.addAttribute("activeSection", showReviewTab ? "review-properties" : "dashboard");

        Map<YearMonth, Long> monthlyCounts = buildMonthlyListingCounts(allProperties);
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MMM");
        List<Long> monthlyCountsList = new ArrayList<>(monthlyCounts.values());
        model.addAttribute("monthlyLabels", monthlyCounts.keySet().stream().map(m -> m.format(labelFormat)).collect(Collectors.toList()));
        model.addAttribute("monthlyCounts", monthlyCountsList);
        model.addAttribute("recentActivity", buildRecentActivity());

        // Real listings-created-this-month count, for the "Total Listings"
        // stat card's "+X this month" delta — last entry in monthlyCounts is
        // always the current month (buildMonthlyListingCounts pre-fills the
        // last 6 months oldest-first).
        long listingsThisMonth = monthlyCountsList.isEmpty() ? 0 : monthlyCountsList.get(monthlyCountsList.size() - 1);
        model.addAttribute("listingsThisMonth", listingsThisMonth);

        // Same idea for the "Approved" stat card's delta — mirrors the exact
        // calculation already used on the Approved Properties page.
        YearMonth currentMonth = YearMonth.now();
        long approvedThisMonth = approvedProperties.stream()
                .filter(p -> p.getCreatedAt() != null && YearMonth.from(p.getCreatedAt()).equals(currentMonth))
                .count();
        model.addAttribute("approvedThisMonth", approvedThisMonth);

        // Greeting + today's date for the dashboard header. adminName comes
        // from addCurrentAdmin() (a @ModelAttribute method that runs before
        // every request in this controller) and reflects whoever is actually
        // logged in, instead of a hardcoded "Sarah".
        int hour = LocalDateTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");
        model.addAttribute("greeting", greeting);
        model.addAttribute("todayDisplay", LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM")));

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
     * Builds the Dashboard's Recent Activity feed from real logged admin
     * actions (see logActivity()), most recent first. Previously this
     * inferred "activity" from whichever properties had the newest
     * createdAt/updatedAt timestamp — which surfaced unrelated data changes
     * (including ones from seed data) as if an admin had just acted, and
     * never showed user- or review-related actions at all.
     */
    /**
     * Full Activity Log page — everything logActivity() has ever recorded,
     * paginated, newest first. This is what the Dashboard's Recent Activity
     * "View all" link goes to now, instead of the Listings page it pointed
     * at before (a leftover from when that feed was built from property
     * timestamps rather than real admin actions).
     */
    @GetMapping("/admin/activity-log")
    public String viewActivityLog(Model model, @RequestParam(required = false, defaultValue = "1") Integer page) {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a");
        List<com.ulee.ulee_backend.model.AdminActivityLog> all = adminActivityLogRepository.findAllByOrderByTimestampDesc();

        int pageSize = 20;
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<ActivityItem> rows = all.subList(fromIndex, toIndex).stream()
                .map(log -> new ActivityItem(
                        log.getMessage(),
                        log.getTimestamp().format(timeFormat),
                        log.getAction(),
                        initialsFromName(log.getActorName())))
                .collect(Collectors.toList());

        model.addAttribute("activityRows", rows);
        model.addAttribute("totalActivity", total);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("fromIndex", total == 0 ? 0 : fromIndex + 1);
        model.addAttribute("toIndex", toIndex);
        addSidebarCounts(model);
        return "admin/admin-activity-log";
    }

    private List<ActivityItem> buildRecentActivity() {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a");

        return adminActivityLogRepository.findTop5ByOrderByTimestampDesc().stream()
                .map(log -> new ActivityItem(
                        log.getMessage(),
                        log.getTimestamp().format(timeFormat),
                        log.getAction(),
                        initialsFromName(log.getActorName())))
                .collect(Collectors.toList());
    }

    /** Small holder for a single row in the Dashboard's recent-activity feed. */
    public static class ActivityItem {
        private final String message;
        private final String timestamp;
        private final String action;
        private final String initials;

        public ActivityItem(String message, String timestamp, String action, String initials) {
            this.message = message;
            this.timestamp = timestamp;
            this.action = action;
            this.initials = initials;
        }

        public String getAction() { return action; }
        public String getInitials() { return initials; }
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }

        /**
         * Buckets the raw action name into a badge color the template can
         * key off directly, instead of the template guessing at status
         * strings that no longer apply (this feed shows admin actions now,
         * not property statuses).
         */
        public String getTone() {
            if (action == null) return "neutral";
            switch (action) {
                case "Approved":
                case "Reactivated":
                case "Unsuspended":
                case "Resolved review":
                    return "positive";
                case "Rejected":
                case "Suspended":
                case "Removed":
                case "Deactivated":
                case "Deleted":
                    return "negative";
                case "Warned landlord":
                case "Warned user":
                    return "warning";
                default:
                    return "neutral";
            }
        }
    }

    @GetMapping("/admin/listing/{id}")
    public String viewListingDetail(@PathVariable Integer id,
                                    @RequestParam(required = false) String from,
                                    Model model) {
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
        model.addAttribute("backUrl", resolveBackUrl(from));
        model.addAttribute("activeSection", resolveActiveSection(from));
        addSidebarCounts(model);
        return "admin/admin-listing-details";
    }

    /**
     * Sends Back to wherever the listing was opened from — matches the `from`
     * param each list page passes.
     *   from=pending  -> Review Properties tab on the dashboard
     *   from=approved -> Approved Properties page
     *   (default)     -> Listings page
     */
    private String resolveBackUrl(String from) {
        if ("pending".equals(from)) {
            return "/admin-index#review-properties";
        }
        if ("approved".equals(from)) {
            return "/admin/approved-properties";
        }
        return "/admin/listings";
    }

    /**
     * Keeps the sidebar highlight on whichever section the listing was opened
     * from, instead of always showing "Listings".
     *   from=pending  -> review-properties
     *   from=approved -> approved-properties
     *   (default)     -> listings
     */
    private String resolveActiveSection(String from) {
        if ("pending".equals(from)) {
            return "review-properties";
        }
        if ("approved".equals(from)) {
            return "approved-properties";
        }
        return "listings";
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

    /** Two-letter initials from a user's first/last name, for the sidebar avatar and similar chips. */
    private String initialsFor(User user) {
        String first = user.getFirstName() != null && !user.getFirstName().isBlank() ? user.getFirstName().substring(0, 1) : "";
        String last = user.getLastName() != null && !user.getLastName().isBlank() ? user.getLastName().substring(0, 1) : "";
        String initials = (first + last).toUpperCase();
        return initials.isEmpty() ? "AD" : initials;
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

        List<Property> allProperties = propertyRepository.findAll();

        // Properties owned per landlord — powers the "Properties owned" panel
        // when a landlord's row is expanded.
        Map<Integer, List<Property>> propertiesByLandlord = allProperties.stream()
                .filter(p -> p.getLandlordID() != null)
                .collect(Collectors.groupingBy(Property::getLandlordID));

        // Reports across every property a landlord owns — powers the
        // "Reports" panel when a landlord's row is expanded. There's no
        // per-report open/resolved status in the data model (only
        // Property.isReported at the property level), so these are shown
        // as a plain reason/description/date list rather than inventing a
        // status that isn't actually tracked.
        Map<Integer, List<Report>> reportsByLandlord = new HashMap<>();
        for (Map.Entry<Integer, List<Property>> entry : propertiesByLandlord.entrySet()) {
            List<Report> landlordReports = new ArrayList<>();
            for (Property p : entry.getValue()) {
                landlordReports.addAll(reportRepository.findByPropertyIDOrderByReportedAtAsc(p.getPropertyID()));
            }
            reportsByLandlord.put(entry.getKey(), landlordReports);
        }

        // A student's confirmed accommodation, resolved through their
        // Application row — "Approved" mirrors the same status vocabulary
        // Property.status already uses elsewhere in this app (Pending /
        // Approved / Rejected), so an Approved application is treated as
        // where the student currently lives. Grouped by studentID so the
        // template can look up "does this student have one" directly.
        Map<Integer, List<Application>> approvedApplicationsByStudent = applicationRepository.findAll().stream()
                .filter(a -> a.getStudentID() != null && "Approved".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.groupingBy(Application::getStudentID));

        // Property lookup (for resolving an application's propertyID into a
        // title/city) and a landlord-user lookup (for resolving a property's
        // landlordID into a name) — both needed to show "X lives at Y,
        // landlord Z" from just the application/property IDs.
        Map<Integer, Property> propertyLookup = allProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));
        Map<Integer, User> landlordUserLookup = landlordUsers.stream()
                .collect(Collectors.toMap(User::getUserID, u -> u));

        model.addAttribute("studentUsers", studentUsers);
        model.addAttribute("landlordUsers", landlordUsers);
        model.addAttribute("totalUsers", studentUsers.size() + landlordUsers.size());
        model.addAttribute("totalStudents", studentUsers.size());
        model.addAttribute("totalLandlords", landlordUsers.size());
        model.addAttribute("totalProperties", allProperties.size());
        model.addAttribute("propertiesByLandlord", propertiesByLandlord);
        model.addAttribute("reportsByLandlord", reportsByLandlord);
        model.addAttribute("approvedApplicationsByStudent", approvedApplicationsByStudent);
        model.addAttribute("propertyLookup", propertyLookup);
        model.addAttribute("landlordUserLookup", landlordUserLookup);
        // Sidebar badges (Review Properties / Reported / Reviews counts) +
        // reuses totalReported here as the "Open reports" stat card too,
        // since that's the same isReported-flag count used everywhere else.
        addSidebarCounts(model);
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
        int newCount = current + 1;
        user.setWarningCount(newCount);
        userRepository.save(user);

        // Actually deliver the warning, not just increment a counter only the
        // admin can see. Targets studentID or landlordID on the Notification
        // depending on which role this user has — exactly one is set.
        com.ulee.ulee_backend.model.Notification notification = new com.ulee.ulee_backend.model.Notification();
        if (landlordRepository.existsById(id)) {
            notification.setLandlordID(id);
        } else if (studentRepository.existsById(id)) {
            notification.setStudentID(id);
        }
        notification.setTitle("Official Warning");
        notification.setMessage("Dear " + safeName(user) + ",\n\nThis serves as an official warning from the ULEE admin team regarding your account. "
                + "This is warning #" + newCount + ". Continued violations may result in your account being deactivated.");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);
        notificationRepository.save(notification);

        logActivity("Warned user", "Warned " + safeName(user) + " (warning #" + newCount + ")");

        redirectAttributes.addFlashAttribute("actionMessage", "Warned " + safeName(user) + " (warning #" + newCount + ")");
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

        // Deactivating a landlord hides their properties from view — same
        // isAvailable flag suspendListing already uses for the same purpose
        // elsewhere in this controller — rather than deleting or changing
        // their approval status, so reactivating can safely restore them.
        if (landlordRepository.existsById(id)) {
            List<Property> ownedProperties = propertyRepository.findAll().stream()
                    .filter(p -> id.equals(p.getLandlordID()))
                    .collect(Collectors.toList());
            for (Property p : ownedProperties) {
                p.setIsAvailable(false);
            }
            propertyRepository.saveAll(ownedProperties);
        }

        // Record why the account was disabled — CustomUserDetailsService
        // already blocks their next login via isActive, but nothing
        // previously told them why. This won't reach them before that
        // blocked attempt, but it's waiting for them once reactivated.
        com.ulee.ulee_backend.model.Notification notification = new com.ulee.ulee_backend.model.Notification();
        if (landlordRepository.existsById(id)) {
            notification.setLandlordID(id);
        } else if (studentRepository.existsById(id)) {
            notification.setStudentID(id);
        }
        notification.setTitle("Account Deactivated");
        notification.setMessage("Dear " + safeName(user) + ",\n\nYour ULEE account has been deactivated by an administrator. "
                + "If you believe this was done in error, please contact support.");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);
        notificationRepository.save(notification);

        logActivity("Deactivated", "Deactivated " + safeName(user));

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

        // Restore visibility for a reactivated landlord's properties. This
        // makes every one of their properties available again — there's no
        // separate flag distinguishing "was already hidden before
        // deactivation" from "hidden because of deactivation", so this is a
        // simple, honest restore-all rather than guessing which ones to skip.
        if (landlordRepository.existsById(id)) {
            List<Property> ownedProperties = propertyRepository.findAll().stream()
                    .filter(p -> id.equals(p.getLandlordID()))
                    .collect(Collectors.toList());
            for (Property p : ownedProperties) {
                p.setIsAvailable(true);
            }
            propertyRepository.saveAll(ownedProperties);
        }

        logActivity("Reactivated", "Reactivated " + safeName(user));

        redirectAttributes.addFlashAttribute("actionMessage", "Reactivated " + safeName(user));
        return "redirect:/admin/manage-users";
    }
    @Autowired
    private com.ulee.ulee_backend.repository.AdminSessionRepository adminSessionRepository;

    @Autowired
    private com.ulee.ulee_backend.repository.AdminLoginActivityRepository adminLoginActivityRepository;

    @GetMapping("/admin/settings")
    public String viewSettings(Model model, java.security.Principal principal,
                               jakarta.servlet.http.HttpServletRequest request) {
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("currentUser", user);

                String currentSessionId = request.getSession().getId();
                DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a");

                List<SessionView> sessions = adminSessionRepository
                        .findByUserIDAndActiveTrueOrderByLastActiveAtDesc(user.getUserID())
                        .stream()
                        .map(s -> new SessionView(
                                s.getId(),
                                s.getDeviceLabel() != null ? s.getDeviceLabel() : "Unknown device",
                                (s.getLocation() != null && !s.getLocation().isBlank()) ? s.getLocation() : "Unknown location",
                                s.getIpAddress() != null ? s.getIpAddress() : "Unknown IP",
                                currentSessionId.equals(s.getSessionId()),
                                s.getLastActiveAt() != null ? s.getLastActiveAt().format(timeFormat) : ""))
                        .collect(Collectors.toList());
                model.addAttribute("activeSessions", sessions);

                // NOTE: "location" here is left as a placeholder — resolving
                // an IP to a real city/country needs a geolocation lookup
                // (e.g. an IP-to-location API call), which isn't wired in
                // yet. The IP address itself IS real and stored.
                List<LoginActivityView> logins = adminLoginActivityRepository
                        .findTop10ByUserIDOrderByTimestampDesc(user.getUserID())
                        .stream()
                        .map(l -> new LoginActivityView(
                                Boolean.TRUE.equals(l.getSuccess()),
                                l.getDeviceLabel() != null ? l.getDeviceLabel() : "Unknown device",
                                l.getIpAddress() != null ? l.getIpAddress() : "Unknown location",
                                l.getTimestamp() != null ? l.getTimestamp().format(timeFormat) : ""))
                        .collect(Collectors.toList());
                model.addAttribute("loginActivity", logins);
            });
        }
        addSidebarCounts(model);
        return "admin/admin-settings";
    }

    /** Read-only row for Settings > Security > Active Sessions. */
    public static class SessionView {
        private final Integer id;
        private final String deviceLabel;
        private final String location;
        private final String ipAddress;
        private final boolean current;
        private final String lastActive;

        public SessionView(Integer id, String deviceLabel, String location, String ipAddress, boolean current, String lastActive) {
            this.id = id;
            this.deviceLabel = deviceLabel;
            this.location = location;
            this.ipAddress = ipAddress;
            this.current = current;
            this.lastActive = lastActive;
        }

        public Integer getId() { return id; }
        public String getDeviceLabel() { return deviceLabel; }
        public String getLocation() { return location; }
        public String getIpAddress() { return ipAddress; }
        public boolean isCurrent() { return current; }
        public String getLastActive() { return lastActive; }
    }

    /** Read-only row for Settings > Security > Recent Login Activity. */
    public static class LoginActivityView {
        private final boolean success;
        private final String deviceLabel;
        private final String location;
        private final String timeLabel;

        public LoginActivityView(boolean success, String deviceLabel, String location, String timeLabel) {
            this.success = success;
            this.deviceLabel = deviceLabel;
            this.location = location;
            this.timeLabel = timeLabel;
        }

        public boolean isSuccess() { return success; }
        public String getDeviceLabel() { return deviceLabel; }
        public String getLocation() { return location; }
        public String getTimeLabel() { return timeLabel; }
    }

    /**
     * Saves the logged-in admin's own profile fields (Settings > Account >
     * Profile > "Save changes"). Called via fetch() from admin-settings.html,
     * so this returns JSON rather than a redirect.
     */
    @PostMapping("/admin/settings/profile")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> updateAdminProfile(@RequestParam String firstName,
                                                  @RequestParam String lastName,
                                                  @RequestParam String email,
                                                  @RequestParam(required = false) String phone,
                                                  java.security.Principal principal) {
        if (principal == null) {
            return Map.of("success", false, "message", "You're not logged in.");
        }
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return Map.of("success", false, "message", "Your account could not be found.");
        }
        User user = userOpt.get();

        // If the email is changing, make sure no OTHER account already uses
        // it — principal.getName() is built from email (see
        // addCurrentAdmin()'s comment above), so if we saved a duplicate
        // email here the admin would be unable to log back in as
        // themselves afterwards.
        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            Optional<User> clash = userRepository.findByEmail(email);
            if (clash.isPresent() && !clash.get().getUserID().equals(user.getUserID())) {
                return Map.of("success", false, "message", "That email is already in use by another account.");
            }
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        userRepository.save(user);

        logActivity("Updated profile", "Updated their own profile details");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated.");
        return response;
    }

    /**
     * Updates the logged-in admin's own password (Settings > Security >
     * Password > "Update password"). The current password MUST match what's
     * stored in the database (checked via PasswordEncoder, never a plain
     * string compare) before the new one is saved.
     */
    @PostMapping("/admin/settings/password")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> updateAdminPassword(@RequestParam String currentPassword,
                                                   @RequestParam String newPassword,
                                                   @RequestParam String confirmPassword,
                                                   java.security.Principal principal) {
        if (principal == null) {
            return Map.of("success", false, "message", "You're not logged in.");
        }
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return Map.of("success", false, "message", "Your account could not be found.");
        }
        User user = userOpt.get();

        // The actual check the person asked for: current password must
        // match what's in the database. passwordEncoder.matches() hashes
        // the submitted value the same way it was hashed at signup/last
        // change and compares hashes — never compare raw strings against
        // user.getPassword() directly, since that column is a hash, not
        // plaintext.
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Map.of("success", false, "message", "Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            return Map.of("success", false, "message", "New password must be at least 8 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return Map.of("success", false, "message", "New passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logActivity("Changed password", "Changed their own password");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Password updated.");
        return response;
    }
    @PostMapping("/admin/delete-user/{id}")
    public String deleteUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "User #" + id + " could not be found.");
            return "redirect:/admin/manage-users";
        }
        String name = safeName(userOpt.get());

        try {
            // A landlord can't be deleted while their properties still exist
            // (property.landlordID FK), and each property can't be deleted
            // while ITS reviews/reports/images/applications still exist —
            // same cascade removeListing already uses, just run once per
            // property this landlord owns.
            if (landlordRepository.existsById(id)) {
                List<Property> ownedProperties = propertyRepository.findAll().stream()
                        .filter(p -> id.equals(p.getLandlordID()))
                        .collect(Collectors.toList());
                for (Property owned : ownedProperties) {
                    deletePropertyCascade(owned.getPropertyID());
                }
                jdbcTemplate.update("DELETE FROM notifications WHERE landlordID = ?", id);
                landlordRepository.deleteById(id);
            }

            // A student can't be deleted while rows referencing their
            // studentID still exist (reviews, reports, applications, saved
            // properties) — same idea, cleared before the student row goes.
            if (studentRepository.existsById(id)) {
                jdbcTemplate.update("DELETE FROM reviews WHERE studentID = ?", id);
                jdbcTemplate.update("DELETE FROM reports WHERE studentID = ?", id);
                jdbcTemplate.update("DELETE FROM notifications WHERE studentID = ?", id);
                // Same legacy-table trap as deletePropertyCascade: "review"
                // is a separate old table from "reviews" (plural) and does
                // carry a studentID column. "report" (singular) does NOT
                // have a studentID column — confirmed by testing — so it's
                // only ever cleaned up by propertyID (in deletePropertyCascade),
                // never here.
                jdbcTemplate.update("DELETE FROM review WHERE studentID = ?", id);
                jdbcTemplate.update("DELETE FROM application WHERE studentID = ?", id);
                jdbcTemplate.update("DELETE FROM savedproperty WHERE studentID = ?", id);
                studentRepository.deleteById(id);
            }

            userRepository.deleteById(id);
            logActivity("Deleted", "Deleted " + name);
            redirectAttributes.addFlashAttribute("actionMessage", "Deleted " + name);
        } catch (Exception e) {
            // NOTE: deliberately not calling setRollbackOnly() here — see the
            // matching note in removeListing for why. Trade-off: whatever
            // succeeded before the failure stays committed rather than
            // rolling back, but the friendly message below actually reaches
            // the admin instead of a blank crash page.
            redirectAttributes.addFlashAttribute("actionError", "Could not delete " + name + ": " + e.getMessage());
        }

        return "redirect:/admin/manage-users";
    }

}