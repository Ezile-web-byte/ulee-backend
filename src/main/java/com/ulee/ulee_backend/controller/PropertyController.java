package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import com.ulee.ulee_backend.service.StayEligibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import com.ulee.ulee_backend.repository.PropertyRepository;
import com.ulee.ulee_backend.repository.SavedPropertyRepository;
import com.ulee.ulee_backend.repository.ApplicationRepository;
import com.ulee.ulee_backend.repository.ReviewRepository;
import com.ulee.ulee_backend.repository.ApplicationDocumentRepository;
import com.ulee.ulee_backend.repository.AmenityRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

import com.ulee.ulee_backend.repository.PropertyImageRepository;

@Controller
public class PropertyController {

    // ── Repositories: each one is Spring Data's auto-generated data-access
    //    layer for one table. Autowired means Spring creates and injects
    //    these for us — we never call "new XyzRepository()" ourselves. ──
    @Autowired
    private PropertyImageRepository propertyImageRepository;
    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;
        @Autowired
    private AmenityRepository amenityRepository;


    // Where uploaded photos get saved on disk — configured in
    // application.properties as app.upload.dir
    @Value("${app.upload.dir}")
    private String uploadDir;

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private SavedPropertyRepository savedPropertyRepository;
    @Autowired
    private com.ulee.ulee_backend.repository.UserRepository userRepository;
    @Autowired
    private com.ulee.ulee_backend.repository.StudentRepository studentRepository;

    // In-app notification bells: one for landlords, one for students.
    @Autowired
    private com.ulee.ulee_backend.service.LandlordNotificationService landlordNotificationService;
    @Autowired
    private com.ulee.ulee_backend.service.StudentNotificationService studentNotificationService;

    // The one exact status string that means "live and visible to students".
    // Every query/branch that decides visibility should use this constant
    // instead of typing "Active" by hand, so a typo can't silently break
    // visibility somewhere.
    private static final String LIVE_STATUS = "Active";

    private static final double HIGHLY_RATED_MIN = 4.0;

    // The one exact status string that means "deactivated / soft-deleted".
    // Any list of a landlord's properties that's meant to represent what
    // still "exists" (dashboards, switchers, dropdowns) should exclude
    // this, the same way LIVE_STATUS marks what's visible to students.
    private static final String DEACTIVATED_STATUS = "Inactive";

    // NEW — the status every freshly submitted application starts with (see
    // applyToProperty below). "New application" in the UI means exactly this:
    // an application the landlord has not accepted or rejected yet.
    private static final String PENDING_STATUS = "Pending";

    // NEW — every property is in the same city (this site is for NMU students
    // only), so landlords never enter it. It is stored on every property so
    // the existing city column stays filled in; the landlord only chooses a
    // SUBURB. Don't show this on cards — cards show the suburb only.
    private static final String CITY = "Gqeberha";

    private boolean isVisibleToStudents(Property property) {
        // Status is the single source of truth for "can a student see this".
        // isAvailable only HIDES a listing when it is explicitly false — a null
        // value (e.g. older/merged rows that never had it set) is treated as
        // available, otherwise every such Active listing silently disappears
        // from the dashboard.
        return LIVE_STATUS.equals(property.getStatus()) && !Boolean.FALSE.equals(property.getIsAvailable());
    }

    // The lowest monthly rent a listing is allowed to go live with.

    private static final java.math.BigDecimal MIN_RENT = new java.math.BigDecimal("1000");

    // ── Small reusable helpers, used by many endpoints below ──

    // Server-side mirror of the wizard/edit-form's client-side validation.
    // This is the enforcement that actually matters — client-side JS can
    // always be bypassed (dev tools, a raw POST, Postman), so a listing
    // can only ever go "live" (Pending/Active) through this backend check,
    // never on the client's say-so alone. Called from BOTH /listProperty
    // (create) and /update-property/{id} (edit), so the rule is identical
    // and can't drift between the two.
    //
    // Rent must be >= MIN_RENT (R1 000), not merely > 0.
    private boolean meetsPublishRequirements(String title, java.math.BigDecimal rent, String address, Integer capacity) {
        boolean titleOk = title != null && !title.isBlank();
        boolean rentOk = rent != null && rent.compareTo(MIN_RENT) >= 0;
        boolean addressOk = address != null && !address.isBlank();
        boolean capacityOk = capacity != null && capacity >= 1;
        return titleOk && rentOk && addressOk && capacityOk;
    }

    // Names which requirement failed, so the edit page can send the landlord
    // straight to the right step with the right message instead of a generic
    // "something's missing". Returns null when everything passes.
    private String firstUnmetRequirement(String title, java.math.BigDecimal rent, String address, Integer capacity) {
        if (title == null || title.isBlank()) return "title";
        if (capacity == null || capacity < 1) return "capacity";
        if (rent == null) return "rent-missing";
        if (rent.compareTo(MIN_RENT) < 0) return "rent-too-low";
        if (address == null || address.isBlank()) return "address";
        return null;
    }

    // Photos needed to publish: one cover photo plus at least this many
    // supporting photos. Keep in step with MIN_SUPPORTING_PHOTOS in listProperty.js.
    private static final int MIN_SUPPORTING_PHOTOS = 1;

    private static int countFiles(MultipartFile[] files) {
        int n = 0;
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) n++;
            }
        }
        return n;
    }

    private boolean hasRequiredPhotos(MultipartFile cover, MultipartFile[] gallery) {
        return cover != null && !cover.isEmpty() && countFiles(gallery) >= MIN_SUPPORTING_PHOTOS;
    }

    // A property name must be unique across ALL properties (any landlord,
    // any status, including deactivated ones). Case and surrounding spaces
    // are ignored, so "Sunset House" and " sunset house " count as the same
    // name. A blank title is never "taken" (several empty drafts are fine —
    // the publish rules already stop a blank title from going live).
    // excludePropertyId lets a landlord re-save their own listing without
    // clashing with itself; pass null when creating.
    private boolean isTitleTaken(String title, Integer excludePropertyId) {
        if (title == null || title.isBlank()) return false;
        String clean = title.trim();
        long matches = (excludePropertyId == null)
                ? propertyRepository.countTitleMatches(clean)
                : propertyRepository.countTitleMatchesExcluding(clean, excludePropertyId);
        return matches > 0;
    }

    // Resolves the logged-in user's account from their email (the Principal
    // Spring Security gives us). Every landlord-only endpoint calls this
    // first to find out WHO is making the request.
    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    // Fetches a property AND checks that the logged-in landlord actually
    // owns it. Throws if either the property doesn't exist or belongs to
    // someone else — this is what stops Landlord A from editing/deleting
    // Landlord B's listings just by guessing a propertyID in the URL.
    private Property getOwnedProperty(Integer propertyId, Integer landlordID) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + propertyId));
        if (!property.getLandlordID().equals(landlordID)) {
            throw new RuntimeException("You do not have permission to manage this property");
        }
        return property;
    }

    // Every "still exists" list of a landlord's own properties (My
    // Properties dashboard) should go through this instead of calling
    // findByLandlordID directly — keeps the Inactive-exclusion rule in one
    // place instead of re-typed in every endpoint. Draft/Pending/Rejected
    // properties ARE included here, since the landlord is still actively
    // managing those from their dashboard.
    private List<Property> findActiveOwnedProperties(Integer landlordID) {
        return propertyRepository.findByLandlordID(landlordID).stream()
                .filter(p -> !DEACTIVATED_STATUS.equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    // Stricter than findActiveOwnedProperties: only status == LIVE_STATUS.
    // Applications can only meaningfully exist against a property once it's
    // live to students, so anywhere that lists properties a landlord could
    // be managing APPLICATIONS for (the Applications switcher, its
    // entry-point redirect) should use this instead — otherwise
    // Draft/Pending properties with zero real applicants show up in the
    // list too.
    private List<Property> findLiveOwnedProperties(Integer landlordID) {
        return propertyRepository.findByLandlordID(landlordID).stream()
                .filter(p -> LIVE_STATUS.equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    // ── NEW: helpers for the re-review rules and the Vacant Rooms page ──

    // BigDecimal equality that ignores scale (1500 == 1500.00) and handles null.
    private boolean sameMoney(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null || b == null) return a == b;
        return a.compareTo(b) == 0;
    }

    // Text equality that ignores surrounding spaces and treats null as "".
    private boolean sameText(String a, String b) {
        return (a == null ? "" : a.trim()).equals(b == null ? "" : b.trim());
    }

    // Accepted applications per property — the "filled" part of capacity.
    private Map<Integer, Long> acceptedCountByProperty(List<Integer> propertyIds) {
        if (propertyIds.isEmpty()) return new LinkedHashMap<>();
        return applicationRepository.findByPropertyIDIn(propertyIds).stream()
                .filter(a -> a.getPropertyID() != null && "Accepted".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.groupingBy(Application::getPropertyID, Collectors.counting()));
    }

    // Vacant rooms for one property = capacity minus accepted students (never below 0).
    private long vacancyOf(Property p, Map<Integer, Long> acceptedByProperty) {
        int capacity = p.getCapacity() != null ? p.getCapacity() : 0;
        return Math.max(0, capacity - acceptedByProperty.getOrDefault(p.getPropertyID(), 0L));
    }


    // ── NEW: helpers for the Applications pages ──

    private boolean isPending(String status) {
        return PENDING_STATUS.equalsIgnoreCase(status);
    }

    // NEW — total number of applications still waiting for a decision across
    // every LIVE property this landlord owns. Powers the badge on the
    // "Applications" sidebar item (see addLandlordShellData below).
    private long countPendingApplications(Integer landlordID) {
        List<Integer> liveIds = findLiveOwnedProperties(landlordID).stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());
        if (liveIds.isEmpty()) return 0;
        return applicationRepository.countByPropertyIDInAndStatus(liveIds, PENDING_STATUS);
    }
    // NEW — number of reviews on this landlord's LIVE properties that the
    // landlord has not opened yet. Powers the badge on the "Reviews" sidebar item.
    private long countUnviewedReviews(Integer landlordID) {
        List<Integer> liveIds = findLiveOwnedProperties(landlordID).stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());
        if (liveIds.isEmpty()) return 0;
        return reviewRepository.countByPropertyIDInAndLandlordViewedFalse(liveIds);
    }

    // NEW — FIRST COME, FIRST SERVED. The student who applied earliest comes
    // first. Applications with the same timestamp (or no timestamp at all)
    // fall back to the applicationID, which always grows over time, so the
    // order is the same every time. Used by "Accept up to capacity" and
    // "Accept all" so the earliest applicants always get the places.
    private int compareFirstComeFirstServed(Application a, Application b) {
        boolean aHasDate = a.getApplicationDate() != null;
        boolean bHasDate = b.getApplicationDate() != null;
        if (aHasDate && !bHasDate) return -1;
        if (!aHasDate && bHasDate) return 1;
        if (aHasDate) {
            int byDate = a.getApplicationDate().compareTo(b.getApplicationDate());
            if (byDate != 0) return byDate;
        }
        return Integer.compare(a.getApplicationID(), b.getApplicationID());
    }

    // NEW — the order applications are LISTED in on the landlord's screen.
    // Pending ones come first, oldest first, so the list reads top to bottom
    // exactly like the queue "Accept up to capacity" works through. Decided
    // ones (Accepted / Rejected) sit below, newest first.
    private int compareQueueOrder(ApplicationRowView a, ApplicationRowView b) {
        boolean aPending = isPending(a.getStatus());
        boolean bPending = isPending(b.getStatus());
        if (aPending != bPending) return aPending ? -1 : 1;

        boolean aHasDate = a.getApplicationDate() != null;
        boolean bHasDate = b.getApplicationDate() != null;
        if (aHasDate && !bHasDate) return -1;
        if (!aHasDate && bHasDate) return 1;

        int result = 0;
        if (aHasDate) {
            result = a.getApplicationDate().compareTo(b.getApplicationDate());
        }
        if (result == 0) {
            result = Integer.compare(a.getApplicationID(), b.getApplicationID());
        }
        return aPending ? result : -result;
    }

    // NEW — two-letter initials for the landlord avatar in the top bar.
    private String initialsOf(User user) {
        String first = user.getFirstName() != null && !user.getFirstName().isBlank()
                ? user.getFirstName().trim().substring(0, 1) : "";
        String last = user.getLastName() != null && !user.getLastName().isBlank()
                ? user.getLastName().trim().substring(0, 1) : "";
        String initials = (first + last).toUpperCase();
        return initials.isEmpty() ? "?" : initials;
    }

    // NEW — display name taken from the database account (never typed into a
    // template). Falls back to the email if both names are blank.
    private String displayNameOf(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }

    // NEW — runs before EVERY request handled by this controller and adds the
    // values the landlord pages' shared shell needs: who is logged in (name +
    // initials for the top bar avatar) and how many applications are waiting
    // (the sidebar badge). Doing it here means every landlord page gets the
    // same correct numbers without each method adding them by hand. For
    // visitors who are not logged in, or are not landlords, it adds nothing.
    @ModelAttribute
    public void addLandlordShellData(Model model, Principal principal) {
        if (principal == null) return;
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null || !"LANDLORD".equalsIgnoreCase(user.getRole())) return;

        model.addAttribute("landlordName", displayNameOf(user));
        model.addAttribute("landlordInitials", initialsOf(user));
        model.addAttribute("pendingApplicationsCount", countPendingApplications(user.getUserID()));
        model.addAttribute("unviewedReviewsCount", countUnviewedReviews(user.getUserID()));
    }

    // Generates the semester-start options for the "Available From"
    // dropdown: Jan and July of each year, one year back through three
    // years ahead.
    private List<LocalDate> generateSemesterOptions() {
        List<LocalDate> options = new ArrayList<>();
        options.add(LocalDate.of(2027, 1, 1));
        options.add(LocalDate.of(2027, 7, 1));
        options.add(LocalDate.of(2028, 1, 1));
        return options;
    }

    // Turns a raw Review entity into a display-ready ReviewRowView (name,
    // initials, "Resident • 2nd Year Student" role text, etc). Used by both
    // the Reviews page and the Manage Property page so review cards look
    // identical everywhere.
    private ReviewRowView toReviewRowView(Review review) {
        User user = userRepository.findById(review.getStudentID()).orElse(null);
        Student studentProfile = studentRepository.findById(review.getStudentID()).orElse(null);

        ReviewRowView view = new ReviewRowView();
        view.setReviewID(review.getReviewID());
        view.setPropertyID(review.getPropertyID());
        view.setRating(review.getRating());
        view.setComment(review.getComment());
        view.setReviewDate(review.getReviewDate());
        view.setLandlordResponse(review.getLandlordResponse());
        view.setResponseDate(review.getResponseDate());
        view.setIsReported(review.getIsReported());
        view.setReportReason(review.getReportReason());

        if (user != null) {
            String lastInitial = (user.getLastName() != null && !user.getLastName().isBlank())
                    ? user.getLastName().charAt(0) + "." : "";
            view.setReviewerName((user.getFirstName() + " " + lastInitial).trim());
            view.setInitials(("" + user.getFirstName().charAt(0)
                    + (user.getLastName() != null && !user.getLastName().isBlank() ? user.getLastName().charAt(0) : ""))
                    .toUpperCase());
        } else {
            view.setReviewerName("Former student");
            view.setInitials("?");
        }

        if (studentProfile != null && studentProfile.getYearOfStudy() != null) {
            view.setReviewerRole("Resident • " + studentProfile.getYearOfStudy() + " Year Student");
        } else {
            view.setReviewerRole("Resident");
        }

        return view;
    }

    // Homepage — browse-first entry point, no login wall
    @GetMapping("/")
    public String home() {
        return "redirect:/student-dashboard";
    }

    // NEW — used by the list-property wizard and the edit-property page to
    // ask "is this name already taken?" BEFORE the landlord saves, so they
    // get a friendly popup instead of losing what they typed. The same
    // rule is enforced again on the server in listProperty() and
    // updateProperty(), so this endpoint is a convenience, not the guard.
    // excludeId = the property being edited (so it doesn't clash with itself).
    @GetMapping("/check-property-title")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Boolean> checkPropertyTitle(
            @RequestParam String title,
            @RequestParam(required = false) Integer excludeId,
            Principal principal) {
        getCurrentUser(principal); // must be logged in
        return Map.of("taken", isTitleTaken(title, excludeId));
    }

    // Public-facing: only properties whose status is exactly LIVE_STATUS
    // show up here. isAvailable is NOT checked on this endpoint — status is
    // the single source of truth for whether students can see a listing at
    // all.
    // Public-facing: only properties whose status is exactly LIVE_STATUS
    // show up here, grouped into named location categories in a fixed
    // display order so the dashboard reads like "On Campus → Summerstrand
    // → Humewood → Off Campus (Town → North End → Central → Pier 14)".
    // Any property whose suburb doesn't match a known category still shows
    // up, just grouped under "Other Areas" at the end, so nothing silently
    // disappears from the dashboard.

    @GetMapping("/student-dashboard")
    public String viewStudentDashboard(Model model, Principal principal) {
        List<Property> visibleProperties = propertyRepository.findByStatus(LIVE_STATUS).stream()
                .filter(this::isVisibleToStudents)
                .collect(Collectors.toList());

        // ── NEW: average rating per property, straight from the reviews ──
        List<Integer> visibleIds = visibleProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        Map<Integer, Double> averageRatings = new LinkedHashMap<>();
        if (!visibleIds.isEmpty()) {
            reviewRepository.findByPropertyIDIn(visibleIds).stream()
                    .filter(r -> r.getPropertyID() != null && r.getRating() != null)
                    .collect(Collectors.groupingBy(Review::getPropertyID,
                            Collectors.averagingInt(Review::getRating)))
                    .forEach(averageRatings::put);
        }

        // Properties with an average of 4.0 or above get the "Highly rated" badge.
        Set<Integer> highlyRatedIds = averageRatings.entrySet().stream()
                .filter(e -> e.getValue() >= HIGHLY_RATED_MIN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // ── NEW: display order inside every suburb ──
        // Highly rated first, then best average rating, then A-Z. The page shows
        // the first 3 (or every highly rated one, if there are more than 3) and
        // puts the rest behind "View all", so a highly rated property is always
        // on show and never ends up in the "View all" part.
        Comparator<Property> byPreference = Comparator
                .comparing((Property p) -> {
                    boolean rated = highlyRatedIds.contains(p.getPropertyID());
                    java.time.LocalDate from = p.getAvailableFrom();
                    boolean availableNow = from == null || !from.isAfter(java.time.LocalDate.now());
                    if (rated && availableNow) return 0;
                    if (rated) return 1;
                    if (availableNow) return 2;
                    return 3;
                })
                .thenComparing((Property p) -> highlyRatedIds.contains(p.getPropertyID()) ? 0 : 1)
                .thenComparing(Comparator.comparingDouble(
                        (Property p) -> averageRatings.getOrDefault(p.getPropertyID(), 0.0)).reversed())
                .thenComparing(p -> p.getTitle() == null ? "" : p.getTitle().toLowerCase());

        List<Property> allProperties = visibleProperties.stream()
                .sorted(byPreference)
                .collect(Collectors.toList());

        // "On Campus" is its own group, then the suburbs in a fixed order.
        // Anything with an unknown suburb goes under "Other Areas" at the end.
        List<String> categoryOrder = List.of(
                "On Campus", "Summerstrand", "Humewood", "Town", "North End", "Central", "Pier 14");

        Map<String, List<Property>> categorizedProperties = new LinkedHashMap<>();
        for (String category : categoryOrder) {
            List<Property> inCategory = allProperties.stream()
                    .filter(p -> category.equals(p.getSuburb()))
                    .collect(Collectors.toList());
            if (!inCategory.isEmpty()) {
                categorizedProperties.put(category, inCategory);
            }
        }

        List<Property> uncategorized = allProperties.stream()
                .filter(p -> p.getSuburb() == null || !categoryOrder.contains(p.getSuburb()))
                .collect(Collectors.toList());
        if (!uncategorized.isEmpty()) {
            categorizedProperties.put("Other Areas", uncategorized);
        }

        model.addAttribute("categorizedProperties", categorizedProperties);
        model.addAttribute("properties", allProperties);
        model.addAttribute("highlyRatedIds", highlyRatedIds); // NEW

        boolean isLoggedIn = principal != null;
        model.addAttribute("isLoggedIn", isLoggedIn);

        if (isLoggedIn) {
            User currentUser = getCurrentUser(principal);
            model.addAttribute("currentUserFirstName", currentUser.getFirstName());

            // Null-safe: accounts can have no first/last name, and the raw
            // charAt(0) threw a NullPointerException (500) on those. initialsOf()
            // returns "?" when both are blank.
            String initials = initialsOf(currentUser);
            model.addAttribute("userInitials", initials);

            Integer studentID = currentUser.getUserID();
            Set<Integer> savedPropertyIds = savedPropertyRepository.findByStudentID(studentID).stream()
                    .map(SavedProperty::getPropertyID)
                    .collect(Collectors.toSet());
            model.addAttribute("savedPropertyIds", savedPropertyIds);

            // Which properties this student has applied to, and the status of each
            Map<Integer, String> appliedStatusByProperty = new LinkedHashMap<>();
            for (Application a : applicationRepository.findByStudentID(studentID)) {
                if (a.getPropertyID() != null) {
                    appliedStatusByProperty.put(a.getPropertyID(), a.getStatus() == null ? "Pending" : a.getStatus());
                }
            }
            model.addAttribute("appliedStatusByProperty", appliedStatusByProperty);
        }

        return "student/student-dashboard";
    }


    // ============================================================
    // C100 support — Landlord Dashboard ("My Properties" inventory page)
    // ============================================================
    //
    // CHANGED — three stat cards now: Total Properties, Awaiting Review and
    // Vacant Rooms. The Avg. Occupancy card is gone.
    //
    // Vacant Rooms used to be "total properties minus available properties",
    // which counts PROPERTIES, not rooms. It is now the real thing: for every
    // LIVE property, capacity minus accepted students, added up.
    @GetMapping("/landlord-index")
    public String viewLandlordDashboard(Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        // Deactivated (soft-deleted) properties never show anywhere on this
        // page — dashboard, stat cards, or filter tabs — regardless of what
        // status they were in before being deactivated.
        List<Property> myProperties = findActiveOwnedProperties(landlordID);

        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        long totalProperties = myProperties.size();

        // "Awaiting Review" counts PROPERTIES this landlord owns that are
        // sitting in "Pending" status — submitted, but not yet approved by an
        // Admin. Deliberately separate from student applications.
        long awaitingApprovalCount = myProperties.stream()
                .filter(p -> "Pending".equals(p.getStatus()))
                .count();

        // Vacant rooms = capacity minus accepted students, LIVE properties only.
        List<Property> liveProperties = myProperties.stream()
                .filter(p -> LIVE_STATUS.equals(p.getStatus()))
                .collect(Collectors.toList());
        Map<Integer, Long> accepted = acceptedCountByProperty(
                liveProperties.stream().map(Property::getPropertyID).collect(Collectors.toList()));
        long vacantRooms = liveProperties.stream().mapToLong(p -> vacancyOf(p, accepted)).sum();

        // Builds a lookup so the property cards can show a cover image
        // without a separate query per card. If a property has more than
        // one image, whichever one Hibernate returns first for that ID wins
        // (see the merge function below) — display order isn't guaranteed
        // here, just "some" image.
        Map<Integer, String> imageLookup = propertyImageRepository.findByPropertyIDIn(propertyIds).stream()
                .collect(Collectors.toMap(
                        PropertyImage::getPropertyID,
                        PropertyImage::getUrl,
                        (existing, replacement) -> existing
                ));

        model.addAttribute("properties", myProperties);
        model.addAttribute("imageLookup", imageLookup);
        model.addAttribute("totalProperties", totalProperties);
        model.addAttribute("vacantRooms", vacantRooms);
        model.addAttribute("awaitingApprovalCount", awaitingApprovalCount);
        return "landlord/landlord-index";
    }


    // ============================================================
    // C101 — Manage Property (edit form)
    // ============================================================

    // Shows the edit form pre-filled with the property's existing data.
    @GetMapping("/edit-property/{id}")
    public String editPropertyForm(@PathVariable Integer id, Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);

        List<Amenity> allAmenities = amenityRepository.findAllByOrderByCategoryAscNameAsc();
        Map<String, List<Amenity>> amenityCategories = allAmenities.stream()
                .collect(Collectors.groupingBy(Amenity::getCategory, LinkedHashMap::new, Collectors.toList()));

        Set<Integer> selectedAmenityIds = property.getAmenities() == null
                ? Set.of()
                : property.getAmenities().stream().map(Amenity::getAmenityID).collect(Collectors.toSet());

        List<ReviewRowView> propertyReviews = reviewRepository.findByPropertyID(id).stream()
                .map(this::toReviewRowView)
                .collect(Collectors.toList());

        model.addAttribute("property", property);
        model.addAttribute("images", propertyImageRepository.findByPropertyID(id));
        model.addAttribute("amenityCategories", amenityCategories);
        model.addAttribute("selectedAmenityIds", selectedAmenityIds);
        model.addAttribute("availableFromOptions", generateSemesterOptions());
        model.addAttribute("reviews", propertyReviews);
        model.addAttribute("reviewCount", propertyReviews.size());
        model.addAttribute("averageRating", propertyReviews.isEmpty() ? 0.0 :
                propertyReviews.stream().mapToInt(ReviewRowView::getRating).average().orElse(0.0));
        return "landlord/edit-property";
    }

    // Saves the edited fields, amenity selections, and optionally appends
    // new photos. Also doubles as "Save Draft": when action=draft, required
    // fields are skipped (the form uses formnovalidate) and whatever WAS
    // filled in is saved with status="Draft" so the landlord can finish
    // later without losing progress.
    //
    // A "Save Changes" (action=update) that would leave the listing below
    // R1 000 rent, or with any starred field blank, is REJECTED outright
    // and bounced back to the edit page with an error flag — it is never
    // silently downgraded to Draft.
    //
    // A title that another property already uses is rejected for BOTH
    // Save Changes and Save Draft (?error=duplicate-title).
    //
    // NEW — STATUS RULES ON SAVE (always the SAME property row, never a new one):
    //   Draft    + Save Changes  -> Pending (first submission)
    //   Rejected + Save Changes  -> Pending (resubmission; admin comment cleared)
    //   Rejected + Save Draft    -> stays Rejected (the admin's comment stays visible)
    //   Active   + Save Changes  -> Pending ONLY IF rent, address, capacity or
    //                               photos changed (hidden from students until the
    //                               admin approves again); any other edit stays Active
    //   Pending  + anything      -> stays Pending
    //
    // CRITICAL ORDERING NOTE: all validation happens BEFORE the Property
    // entity is mutated. With spring.jpa.open-in-view left at its default
    // (true), the entity returned by getOwnedProperty stays managed for the
    // whole request, so Hibernate's dirty checking can flush field changes
    // at request end even on a path that never calls save(). Mutating first
    // and returning early would therefore still persist the invalid rent.
    // Do not reorder these blocks.
    @PostMapping("/update-property/{id}")
    public String updateProperty(
            @PathVariable Integer id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String address,
            // CHANGED — was "city". The form field is now "suburb" (a dropdown).
            // City is no longer sent by the forms; it is always set to CITY below.
            @RequestParam(required = false) String suburb,
            @RequestParam(required = false) String rent,
            @RequestParam(required = false) String deposit,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String commuteType,
            @RequestParam(required = false) String occupantRestriction,
            @RequestParam(required = false) String acceptedFunding,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(value = "amenityIds", required = false) List<Integer> amenityIds,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "action", defaultValue = "update") String action,
            Principal principal) throws IOException {

        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);
        boolean isDraft = "draft".equals(action);

        // ── STEP 1: work out what the values WOULD become, without
        //    writing anything to the entity yet. Same fallback rules the
        //    mutation block below uses, so the two can't disagree. ──
        String effectiveTitle = (title != null) ? title : property.getTitle();
        String effectiveAddress = (address != null) ? address : property.getAddress();
        Integer effectiveCapacity = (capacity != null) ? capacity : property.getCapacity();

        java.math.BigDecimal effectiveRent;
        boolean rentUnparseable = false;
        if (rent != null && !rent.isBlank()) {
            try {
                effectiveRent = new java.math.BigDecimal(rent);
            } catch (NumberFormatException ex) {
                effectiveRent = null;
                rentUnparseable = true;
            }
        } else {
            effectiveRent = property.getRent();
        }

        // ── NEW: property names must be unique. Applies to Save Draft too,
        //    otherwise a draft could create the duplicate. Nothing has been
        //    mutated yet, so returning here leaves the stored property alone. ──
        if (title != null && isTitleTaken(title, id)) {
            return "redirect:/edit-property/" + id + "?error=duplicate-title";
        }

        // ── STEP 2: gate the save. Only "Save Changes" is gated — "Save
        //    Draft" still saves whatever's been filled in, exactly as
        //    before, so a landlord is never trapped and never loses work. ──
        if (!isDraft) {
            if (rentUnparseable) {
                return "redirect:/edit-property/" + id + "?error=rent-invalid";
            }
            String unmet = firstUnmetRequirement(effectiveTitle, effectiveRent, effectiveAddress, effectiveCapacity);
            if (unmet != null) {
                // Nothing has been mutated at this point, so returning here
                // leaves the stored property completely untouched.
                return "redirect:/edit-property/" + id + "?error=" + unmet;
            }
        }

        // ── NEW STEP 2b: did the landlord change something the admin must
        //    look at again? Only rent, address, capacity and photos count.
        //    Everything else (title, description, amenities, deposit, suburb,
        //    room type, ...) saves freely. This compares OLD vs NEW values, so
        //    it must run before STEP 3 changes the entity. ──
        boolean rentChanged = effectiveRent != null && !sameMoney(effectiveRent, property.getRent());
        boolean addressChanged = address != null && !sameText(address, property.getAddress());
        boolean capacityChanged = capacity != null && !capacity.equals(property.getCapacity());
        boolean newPhotosUploaded = coverImage != null && !coverImage.isEmpty();
        if (images != null) {
            for (MultipartFile f : images) {
                if (f != null && !f.isEmpty()) { newPhotosUploaded = true; break; }
            }
        }
        boolean needsReReview = rentChanged || addressChanged || capacityChanged || newPhotosUploaded;
        String previousStatus = property.getStatus();

        // ── STEP 3: only now is it safe to mutate. ──
        if (title != null) property.setTitle(title.trim());
        if (type != null) property.setType(type);
        if (address != null) property.setAddress(address);
        // CHANGED — the landlord picks a suburb; the city is always the same.
        // If the request has no suburb param at all, the stored suburb is left alone.
        if (suburb != null) property.setSuburb(suburb.isBlank() ? null : suburb.trim());
        property.setCity(CITY);
        property.setRent(effectiveRent != null ? effectiveRent : property.getRent());
        property.setDeposit((deposit != null && !deposit.isBlank()) ? new java.math.BigDecimal(deposit) : null);
        property.setDescription(description);
        property.setCommuteType(commuteType);
        // The two selects always post a value from the edit form, so a
        // non-null param is the landlord's real choice.
        // If a request doesn't include the param at all, the stored value
        // is left alone.
        if (occupantRestriction != null) property.setOccupantRestriction(occupantRestriction);
        if (acceptedFunding != null) property.setAcceptedFunding(acceptedFunding.isBlank() ? "Any" : acceptedFunding);
        if (capacity != null) property.setCapacity(capacity);

        if (availableFrom != null && !availableFrom.isBlank()) {
            property.setAvailableFrom(LocalDate.parse(availableFrom));
        }

        // Amenity checkboxes are replaced wholesale each save — whatever
        // came through in the request IS the new full set.
        property.setAmenities(amenityIds != null
                ? amenityRepository.findAllById(amenityIds)
                : new ArrayList<>());

        // Which message the landlord sees after saving.
        boolean submittedForApproval = false;      // Draft -> Pending
        boolean resubmittedAfterRejection = false; // Rejected -> Pending
        boolean sentBackForReview = false;         // Active -> Pending (sensitive edit)

        if (isDraft) {
            // A rejected listing stays "Rejected" when only a draft is saved,
            // so the admin's comment stays visible until the landlord really
            // resubmits. Anything else becomes a Draft, as before.
            if (!"Rejected".equals(previousStatus)) {
                property.setStatus("Draft");
            }
        } else if ("Draft".equals(previousStatus)) {
            submittedForApproval = true;
            // Finishing a draft via "Save Changes" submits it for Admin
            // approval — same as a brand-new listing going Pending. By
            // this point requirements are guaranteed met (STEP 2 returned
            // early otherwise), so no re-check is needed here.
            property.setStatus("Pending");
        } else if ("Rejected".equals(previousStatus)) {
            // The "second chance": same row, same property, back to the admin.
            resubmittedAfterRejection = true;
            property.setStatus("Pending");
            property.setRejectionReason(null);
        } else if (LIVE_STATUS.equals(previousStatus) && needsReReview) {
            // A live listing with a sensitive change is hidden until the admin
            // approves it again. Students only ever see status == "Active",
            // so "Pending" hides it. Approval sets Active + isAvailable again.
            sentBackForReview = true;
            property.setStatus("Pending");
        }

        propertyRepository.save(property);

        // Append any newly uploaded photos after the ones already on file
        // (existingCount + 1 onward), rather than replacing them.
        // Handle cover photo and/or additional photos. Both share one
        // upload directory check, done once up front.
        boolean hasCoverImage = coverImage != null && !coverImage.isEmpty();
        boolean hasAdditionalImages = images != null && images.length > 0;

        if (hasCoverImage || hasAdditionalImages) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Replacing the cover photo: un-mark whatever was main before,
            // then save the new one as the main image.
            if (hasCoverImage) {
                List<PropertyImage> existingImages = propertyImageRepository.findByPropertyID(id);
                for (PropertyImage existing : existingImages) {
                    if (Boolean.TRUE.equals(existing.getIsMain())) {
                        existing.setIsMain(false);
                        propertyImageRepository.save(existing);
                    }
                }

                String uniqueFileName = id + "_" + System.currentTimeMillis() + "_" + coverImage.getOriginalFilename();
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(coverImage.getInputStream(), filePath);

                PropertyImage coverPropImage = new PropertyImage();
                coverPropImage.setPropertyID(id);
                coverPropImage.setUrl("/uploads/" + uniqueFileName);
                coverPropImage.setCategory("exterior");
                coverPropImage.setIsMain(true);
                coverPropImage.setDisplayOrder(0);
                propertyImageRepository.save(coverPropImage);
            }

            // Append any newly uploaded gallery photos after the ones
            // already on file, rather than replacing them.
            //
            // FIX: this used to hardcode isMain=false for every appended
            // photo. Combined with the same bug in listProperty(), a
            // property that ended up with zero main images could NEVER
            // get one assigned through this endpoint either — permanently
            // broken. Now it checks whether the property already has a
            // main image (re-queried AFTER the coverImage block above, so
            // it correctly sees a cover photo just uploaded in this same
            // request) and, if not, promotes the first newly-appended
            // photo to main instead.
            if (hasAdditionalImages) {
                List<PropertyImage> currentImages = propertyImageRepository.findByPropertyID(id);
                int existingCount = currentImages.size();
                int order = existingCount + 1;

                boolean propertyHasMainImage = currentImages.stream()
                        .anyMatch(img -> Boolean.TRUE.equals(img.getIsMain()));
                boolean firstNewImageHandled = false;

                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        String uniqueFileName = id + "_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
                        Path filePath = uploadPath.resolve(uniqueFileName);
                        Files.copy(image.getInputStream(), filePath);

                        boolean isMain = !propertyHasMainImage && !firstNewImageHandled;

                        PropertyImage propImage = new PropertyImage();
                        propImage.setPropertyID(id);
                        propImage.setUrl("/uploads/" + uniqueFileName);
                        propImage.setCategory("exterior");
                        propImage.setIsMain(isMain);
                        propImage.setDisplayOrder(order);
                        propertyImageRepository.save(propImage);
                        order++;

                        if (isMain) {
                            firstNewImageHandled = true;
                        }
                    }
                }
            }
        }

        // All branches return to the dashboard (not back to the edit
        // form), with a query flag so the dashboard's popup knows which
        // message to show.
        if (isDraft) return "redirect:/landlord-index?toast=draft-saved";
        if (resubmittedAfterRejection) return "redirect:/landlord-index?toast=resubmitted";
        if (sentBackForReview) return "redirect:/landlord-index?toast=review-needed";
        return submittedForApproval
                ? "redirect:/landlord-index?toast=submitted"
                : "redirect:/landlord-index?toast=updated";
    }

    // Removes a single photo from a property.
    //
    // NEW — photos are one of the four things the admin re-checks, so deleting
    // a photo from a LIVE listing sends it back to Pending (hidden until
    // re-approved), exactly like uploading one does. Remove this block if you
    // decide deleting a photo should not need re-approval.
    @PostMapping("/delete-property-image/{imageId}")
    public String deletePropertyImage(@PathVariable Integer imageId, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        Property property = getOwnedProperty(image.getPropertyID(), landlordID);

        propertyImageRepository.deleteById(imageId);

        if (LIVE_STATUS.equals(property.getStatus())) {
            property.setStatus("Pending");
            propertyRepository.save(property);
        }

        return "redirect:/edit-property/" + property.getPropertyID();
    }


    // Deactivate or reactivate a listing. This is a SOFT delete — the row
    // is never removed from the database.
    //   isAvailable=false → status becomes "Inactive" regardless of what it
    //                        was before (Draft, Pending, or Active). This
    //                        is what lets viewLandlordDashboard's filter
    //                        hide it no matter which state it was in.
    //   isAvailable=true  → if it was Inactive, restores status to
    //                        LIVE_STATUS ("Active"), making it visible to
    //                        students again immediately (no re-approval).
    @PostMapping("/toggle-property-status/{id}")
    public String togglePropertyStatus(@PathVariable Integer id, @RequestParam Boolean isAvailable, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);

        boolean wasReactivated = Boolean.TRUE.equals(isAvailable) && DEACTIVATED_STATUS.equals(property.getStatus());

        if (Boolean.FALSE.equals(isAvailable)) {
            property.setStatus(DEACTIVATED_STATUS);
        } else if (DEACTIVATED_STATUS.equals(property.getStatus())) {
            property.setStatus(LIVE_STATUS);
        }
        property.setIsAvailable(isAvailable);
        propertyRepository.save(property);

        return Boolean.FALSE.equals(isAvailable)
                ? "redirect:/landlord-index?toast=deactivated"
                : wasReactivated
                ? "redirect:/landlord-index?toast=reactivated"
                : "redirect:/landlord-index";
    }

    // ============================================================
    // Delete a property permanently (the Delete button on the edit page).
    // Unlike togglePropertyStatus above (a soft "Inactive" flag), this
    // removes the row for good, along with everything that points at it.
    // ============================================================
    @PostMapping("/delete-property/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteProperty(@PathVariable Integer id, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID); // ownership check

        List<Application> applications = applicationRepository.findByPropertyIDIn(List.of(id));
        // Accepted students: close the property and ask them to move instead of blocking.
        List<Application> acceptedHere = applications.stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).collect(Collectors.toList());
        List<String> openedTitles = new java.util.ArrayList<>();
        if (!acceptedHere.isEmpty()) {
            if (!"Closing".equalsIgnoreCase(property.getStatus())) {
                int needed = acceptedHere.size();
                java.time.LocalDate today = java.time.LocalDate.now();
                List<Property> others = propertyRepository.findByLandlordID(landlordID).stream()
                        .filter(p -> !p.getPropertyID().equals(id) && isLiveProperty(p))
                        .collect(Collectors.toList());
                int available = others.stream()
                        .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                        .mapToInt(this::freeSpaces).sum();
                // Not enough open rooms: open the landlord's not-yet-open properties, nearest date first.
                List<Property> toOpen = new java.util.ArrayList<>();
                if (available < needed) {
                    List<Property> notOpen = others.stream()
                            .filter(p -> !Boolean.TRUE.equals(p.getIsAvailable()))
                            .filter(p -> freeSpaces(p) > 0)
                            .sorted(java.util.Comparator.comparing(Property::getAvailableFrom,
                                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                            .collect(Collectors.toList());
                    for (Property p : notOpen) {
                        if (available >= needed) break;
                        toOpen.add(p);
                        available += freeSpaces(p);
                    }
                }
                if (available < needed) {
                    return "redirect:/edit-property/" + id + "?error=has-accepted-students";
                }
                if (!toOpen.isEmpty()) {
                    return "redirect:/delete-property/" + id + "/choose-rooms";
                }
                for (Property p : toOpen) {
                    p.setIsAvailable(true);
                    p.setAvailableFrom(today);
                    openedTitles.add(p.getTitle());
                    propertyRepository.save(p);
                }
                property.setStatus("Closing");
                property.setIsAvailable(false);
                propertyRepository.save(property);
                for (Application a : acceptedHere) {
                    try { studentNotificationService.relocationRequested(a.getStudentID(), id, property.getTitle()); }
                    catch (Exception ignored) { }
                }
            }
            String openedParam = openedTitles.isEmpty() ? "" : "&opened=" + java.net.URLEncoder.encode(String.join("|", openedTitles), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/landlord-index?toast=closing" + openedParam;
        }

        purgeProperty(id, property, applications);
        return "redirect:/landlord-index?toast=deleted";
    }

    // Step 2 of deleting a property that has accepted students: the landlord picks which
    // of their other properties to open early. Nothing is opened automatically.
    @GetMapping("/delete-property/{id}/choose-rooms")
    public String chooseRoomsPage(@PathVariable Integer id, Principal principal, org.springframework.ui.Model model) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);
        List<Application> accepted = applicationRepository.findByPropertyIDIn(List.of(id)).stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).collect(Collectors.toList());
        if (accepted.isEmpty() || "Closing".equalsIgnoreCase(property.getStatus())) {
            return "redirect:/edit-property/" + id;
        }
        List<Property> others = propertyRepository.findByLandlordID(landlordID).stream()
                .filter(p -> !p.getPropertyID().equals(id) && isLiveProperty(p))
                .collect(Collectors.toList());
        int alreadyOpen = others.stream().filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                .mapToInt(this::freeSpaces).sum();
        List<Property> candidates = others.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsAvailable()))
                .filter(p -> freeSpaces(p) > 0)
                .sorted(java.util.Comparator.comparing(Property::getAvailableFrom,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .collect(Collectors.toList());
        List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (Property p : candidates) {
            java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("id", p.getPropertyID());
            r.put("title", p.getTitle());
            r.put("free", freeSpaces(p));
            r.put("from", p.getAvailableFrom() != null ? p.getAvailableFrom().toString() : "not set");
            rows.add(r);
        }
        model.addAttribute("property", property);
        model.addAttribute("needed", accepted.size());
        model.addAttribute("alreadyOpen", alreadyOpen);
        model.addAttribute("rows", rows);
        return "landlord/choose-rooms";
    }

    @PostMapping("/delete-property/{id}/open-rooms")
    @org.springframework.transaction.annotation.Transactional
    public String openChosenRooms(@PathVariable Integer id,
                                  @RequestParam(required = false) List<Integer> openIds,
                                  Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);
        List<Application> acceptedHere = applicationRepository.findByPropertyIDIn(List.of(id)).stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).collect(Collectors.toList());
        if (acceptedHere.isEmpty() || "Closing".equalsIgnoreCase(property.getStatus())) {
            return "redirect:/edit-property/" + id;
        }
        List<Property> others = propertyRepository.findByLandlordID(landlordID).stream()
                .filter(p -> !p.getPropertyID().equals(id) && isLiveProperty(p))
                .collect(Collectors.toList());
        int available = others.stream().filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                .mapToInt(this::freeSpaces).sum();
        List<Property> chosen = others.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsAvailable()))
                .filter(p -> freeSpaces(p) > 0)
                .filter(p -> openIds != null && openIds.contains(p.getPropertyID()))
                .collect(Collectors.toList());
        for (Property p : chosen) available += freeSpaces(p);
        if (available < acceptedHere.size()) {
            return "redirect:/delete-property/" + id + "/choose-rooms?error=not-enough";
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        List<String> openedTitles = new java.util.ArrayList<>();
        for (Property p : chosen) {
            p.setIsAvailable(true);
            p.setAvailableFrom(today);
            propertyRepository.save(p);
            openedTitles.add(p.getTitle());
        }
        property.setStatus("Closing");
        property.setIsAvailable(false);
        propertyRepository.save(property);
        for (Application a : acceptedHere) {
            try { studentNotificationService.relocationRequested(a.getStudentID(), id, property.getTitle()); }
            catch (Exception ignored) { }
        }
        String openedParam = openedTitles.isEmpty() ? "" : "&opened=" + java.net.URLEncoder.encode(String.join("|", openedTitles), java.nio.charset.StandardCharsets.UTF_8);
        return "redirect:/landlord-index?toast=closing" + openedParam;
    }

    // Removes the property and everything that points at it (used by delete and by the last student's move).
    private void purgeProperty(Integer id, Property property, List<Application> applications) {
        // 1. Application documents (rows + files on disk), then the applications
        for (Application app : applications) {
            List<ApplicationDocument> docs = applicationDocumentRepository.findByApplicationID(app.getApplicationID());
            for (ApplicationDocument d : docs) {
                deleteFileQuietly(d.getFilePath());
            }
            applicationDocumentRepository.deleteAll(docs);
        }
        applicationRepository.deleteAll(applications);

        // 2. Reviews and saved-property hearts
        reviewRepository.deleteAll(reviewRepository.findByPropertyID(id));
        savedPropertyRepository.deleteAll(savedPropertyRepository.findAll().stream()
                .filter(s -> id.equals(s.getPropertyID()))
                .collect(Collectors.toList()));

        // 3. Property photos (rows + files)
        List<PropertyImage> images = propertyImageRepository.findByPropertyID(id);
        for (PropertyImage img : images) {
            deleteUploadedFileQuietly(img.getUrl());
        }
        propertyImageRepository.deleteAll(images);

        // 4. The property itself (its amenity links go with it)
        propertyRepository.delete(property);
    }

    // ---------- Relocation of accepted students before a property is deleted ----------
    private boolean isLiveProperty(Property p) {
        return "Active".equalsIgnoreCase(p.getStatus()) || "Approved".equalsIgnoreCase(p.getStatus());
    }

    private int freeSpaces(Property p) {
        int cap = p.getCapacity() == null ? 0 : p.getCapacity();
        long taken = applicationRepository.findByPropertyIDIn(List.of(p.getPropertyID())).stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).count();
        return Math.max(0, cap - (int) taken);
    }

    private Application acceptedApplication(Integer studentID, Integer propertyID) {
        return applicationRepository.findByStudentIDAndPropertyID(studentID, propertyID).stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).findFirst().orElse(null);
    }

    private boolean allowedBy(Property p, String gender, Integer year) {
        String r = p.getOccupantRestriction() == null ? "" : p.getOccupantRestriction().trim().toLowerCase();
        String g = gender == null ? "" : gender.trim();
        if (r.equals("female only") && !g.equalsIgnoreCase("Female")) return false;
        if (r.equals("male only") && !g.equalsIgnoreCase("Male")) return false;
        if (r.startsWith("senior") && (year == null || year < 2)) return false;
        if (r.startsWith("first year") && (year == null || year != 1)) return false;
        return true;
    }

    private boolean fundingOk(Property p, String funding) {
        String req = p.getAcceptedFunding();
        if (req == null || req.isBlank() || "Any".equalsIgnoreCase(req)) return true;
        return funding != null && req.equalsIgnoreCase(funding);
    }

    private List<Property> relocationOptions(User me, Property old, Application mine) {
        Integer year = studentRepository.findById(me.getUserID()).map(Student::getYearOfStudy).orElse(null);
        return propertyRepository.findByLandlordID(old.getLandlordID()).stream()
                .filter(p -> !p.getPropertyID().equals(old.getPropertyID()))
                .filter(p -> isLiveProperty(p) && Boolean.TRUE.equals(p.getIsAvailable()))
                .filter(p -> freeSpaces(p) > 0)
                .filter(p -> !applicationRepository.existsByStudentIDAndPropertyID(me.getUserID(), p.getPropertyID()))
                .collect(Collectors.toList());
    }

    @GetMapping("/relocate/{propertyId}/options")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> relocateOptions(@PathVariable Integer propertyId, Principal principal) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        User me = getCurrentUser(principal);
        Application mine = acceptedApplication(me.getUserID(), propertyId);
        Property old = propertyRepository.findById(propertyId).orElse(null);
        if (mine == null || old == null) {
            result.put("moved", true);
            result.put("options", new java.util.ArrayList<>());
            return result;
        }
        java.util.List<java.util.Map<String, Object>> opts = new java.util.ArrayList<>();
        for (Property p : relocationOptions(me, old, mine)) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", p.getPropertyID());
            m.put("title", p.getTitle());
            m.put("suburb", p.getSuburb());
            m.put("rent", p.getRent());
            m.put("type", p.getType());
            opts.add(m);
        }
        result.put("moved", false);
        result.put("options", opts);
        return result;
    }
    @GetMapping("/relocate/{propertyId}")
    public String relocatePage(@PathVariable Integer propertyId, Principal principal, org.springframework.ui.Model model) {
        User me = getCurrentUser(principal);
        Application mine = acceptedApplication(me.getUserID(), propertyId);
        Property old = propertyRepository.findById(propertyId).orElse(null);
        if (mine == null || old == null) return "redirect:/my-applications";
        model.addAttribute("oldProperty", old);
        model.addAttribute("options", relocationOptions(me, old, mine));
        return "student/relocate";
    }

    @PostMapping("/relocate/{oldId}/to/{newId}")
    @org.springframework.transaction.annotation.Transactional
    public String relocate(@PathVariable Integer oldId, @PathVariable Integer newId, Principal principal) {
        User me = getCurrentUser(principal);
        Application mine = acceptedApplication(me.getUserID(), oldId);
        Property old = propertyRepository.findById(oldId).orElse(null);
        if (mine == null || old == null) return "redirect:/my-applications";
        boolean allowed = relocationOptions(me, old, mine).stream().anyMatch(p -> p.getPropertyID().equals(newId));
        if (!allowed) return "redirect:/relocate/" + oldId;
        mine.setPropertyID(newId);
        // Emergency move: the new place counts as a current stay, so reviews open straight away.
        propertyRepository.findById(newId).ifPresent(np -> {
            if (np.getAvailableFrom() == null || np.getAvailableFrom().isAfter(java.time.LocalDate.now())) {
                np.setAvailableFrom(java.time.LocalDate.now());
                propertyRepository.save(np);
            }
        });
        applicationRepository.save(mine);
        // She has chosen where she stays, so her other Pending applications are removed.
        List<Application> otherPending = applicationRepository.findAll().stream()
                .filter(a -> java.util.Objects.equals(a.getStudentID(), me.getUserID()))
                .filter(a -> !java.util.Objects.equals(a.getApplicationID(), mine.getApplicationID()))
                .filter(a -> "Pending".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());
        for (Application a : otherPending) {
            List<ApplicationDocument> docs = applicationDocumentRepository.findByApplicationID(a.getApplicationID());
            for (ApplicationDocument d : docs) { deleteFileQuietly(d.getFilePath()); }
            applicationDocumentRepository.deleteAll(docs);
        }
        applicationRepository.deleteAll(otherPending);

        List<Application> left = applicationRepository.findByPropertyIDIn(List.of(oldId));
        boolean anyAccepted = left.stream().anyMatch(a -> "Accepted".equalsIgnoreCase(a.getStatus()));
        if (!anyAccepted && "Closing".equalsIgnoreCase(old.getStatus())) {
            purgeProperty(oldId, old, left);
        }
        return "redirect:/my-applications";
    }
    // Best-effort file cleanup: a missing file must never block the delete.
    private void deleteFileQuietly(String path) {
        if (path == null || path.isBlank()) return;
        try { Files.deleteIfExists(Paths.get(path)); } catch (Exception ignored) { }
    }

    // Photo URLs are stored as "/uploads/<filename>"; the file lives in uploadDir.
    private void deleteUploadedFileQuietly(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(url.substring("/uploads/".length())));
        } catch (Exception ignored) { }
    }


    // FIX: this previously only passed "property", "reviews", and
    // "vrImages" to the template — meaning the image gallery, amenities,
    // and special features sections all rendered blank regardless of what
    // was actually on file, since the template variables they depend on
    // ("images", "amenitiesByCategory", "features") were never populated.
    @GetMapping("/property/{id}")
    public String viewPropertyDetail(@PathVariable Integer id, Model model, Principal principal) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));

        // NEW — this page never checked status/isAvailable at all, so a
        // direct link to a suspended listing, or one belonging to a
        // deactivated landlord, was still fully viewable (and applyable —
        // see the matching check in applyToProperty below). The owning
        // landlord can still open their own listing here (e.g. to preview
        // it), everyone else gets bounced to the dashboard.
        boolean viewerIsOwner = principal != null
                && getCurrentUser(principal).getUserID().equals(property.getLandlordID());
        if (!isVisibleToStudents(property) && !viewerIsOwner) {
            return "redirect:/student-dashboard?propertyUnavailable=true";
        }

        List<PropertyImage> propertyImages = propertyImageRepository.findByPropertyID(id).stream()
                .sorted((a, b) -> {
                    Integer orderA = a.getDisplayOrder() != null ? a.getDisplayOrder() : 0;
                    Integer orderB = b.getDisplayOrder() != null ? b.getDisplayOrder() : 0;
                    return orderA.compareTo(orderB);
                })
                .collect(Collectors.toList());

        // Fetch raw reviews ONCE — used both to build the display rows AND to
        // check whether the current student has already left one (Review has
        // studentID; ReviewRowView doesn't need to).
        List<Review> rawReviews = reviewRepository.findByPropertyID(id);
        List<ReviewRowView> propertyReviews = rawReviews.stream()
                .map(this::toReviewRowView)
                .collect(Collectors.toList());

        boolean isLoggedIn = principal != null;
        boolean isStudent = false;
        boolean hasApplied = false;
        boolean alreadyReviewed = false;
        boolean canWriteReview = false;
        User currentUser = null;
        Student studentProfile = null;

        if (isLoggedIn) {
            currentUser = getCurrentUser(principal);
            isStudent = "STUDENT".equalsIgnoreCase(currentUser.getRole());

            if (isStudent) {
                Integer studentID = currentUser.getUserID();
                hasApplied = applicationRepository.existsByStudentIDAndPropertyID(studentID, id);
                studentProfile = studentRepository.findById(studentID).orElse(null);

                alreadyReviewed = rawReviews.stream()
                        .anyMatch(r -> studentID.equals(r.getStudentID()));

                // Can only review your CURRENT stay: the newest accepted
                // application whose stay has started, if not reviewed yet.
                Integer currentStayId = applicationRepository.findByStudentID(studentID).stream()
                        .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()) && a.getPropertyID() != null)
                        .filter(a -> propertyRepository.findById(a.getPropertyID()).map(StayEligibility::hasStarted).orElse(false))
                        .max(Comparator.comparing(Application::getApplicationID))
                        .map(Application::getPropertyID)
                        .orElse(null);
                canWriteReview = id.equals(currentStayId) && !alreadyReviewed;
            }
        }

        model.addAttribute("property", property);
        model.addAttribute("images", propertyImages);
        model.addAttribute("amenityCategoryViews", buildAmenityCategoryViews(property));
        model.addAttribute("reviews", propertyReviews);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("hasApplied", hasApplied);
        model.addAttribute("alreadyReviewed", alreadyReviewed);
        model.addAttribute("canWriteReview", canWriteReview);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("studentProfile", studentProfile);
        model.addAttribute("freeRooms", freeSpaces(property)); // capacity minus accepted students
        return "student/property-detail";
    }


    // Public search — same LIVE_STATUS rule as the student dashboard, so
    // search never surfaces a Pending/Draft/Inactive property.
    // FIX: same isAvailable gap as viewStudentDashboard — a deactivated
    // landlord's properties still pass the repository's status-only
    // queries, so every branch below now also filters through
    // isVisibleToStudents() before handing results to the template.
    @GetMapping("/search")
    public String searchProperties(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) java.math.BigDecimal maxRent,
            Model model) {

        List<Property> results;
        if (maxRent != null) {
            results = propertyRepository.findByStatusAndRentLessThanEqual(LIVE_STATUS, maxRent);
        } else if (query != null && !query.isBlank()) {
            results = propertyRepository.searchActive(query);
        } else {
            results = propertyRepository.findByStatus(LIVE_STATUS);
        }

        model.addAttribute("properties", results.stream()
                .filter(this::isVisibleToStudents)
                .collect(Collectors.toList()));
        return "properties";
    }

    // ============================================================
    // C102 — View Reviews (landlord side)
    // ============================================================

    // "Reviews" in the sidebar opens the All Properties list: one row per live
    // property with its review count. Properties with reviews the landlord has
    // not opened yet come first.
    @GetMapping("/my-property-reviews")
    public String viewPropertyReviews(Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        List<Property> myProperties = findLiveOwnedProperties(landlordID);
        if (myProperties.isEmpty()) {
            return "redirect:/landlord-index?noReviewsYet=true";
        }

        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        Map<Integer, List<Review>> reviewsByProperty = reviewRepository.findByPropertyIDIn(propertyIds).stream()
                .filter(r -> r.getPropertyID() != null)
                .collect(Collectors.groupingBy(Review::getPropertyID));

        // Cover photo per property: the main image if there is one, otherwise the first image.
        Map<Integer, String> coverLookup = new LinkedHashMap<>();
        for (PropertyImage img : propertyImageRepository.findByPropertyIDIn(propertyIds)) {
            if (img.getUrl() == null) continue;
            boolean isMain = Boolean.TRUE.equals(img.getIsMain());
            if (isMain || !coverLookup.containsKey(img.getPropertyID())) {
                coverLookup.put(img.getPropertyID(), img.getUrl());
            }
        }

        java.util.Map<Integer, java.time.LocalDateTime> latestReview = new java.util.HashMap<>();
        List<ReviewOverviewView> summaries = new ArrayList<>();
        long totalReviewsAll = 0;
        for (Property p : myProperties) {
            List<Review> reviews = reviewsByProperty.getOrDefault(p.getPropertyID(), new ArrayList<>());
            long unviewed = reviews.stream()
                    .filter(r -> Boolean.FALSE.equals(r.getLandlordViewed()))
                    .count();

            ReviewOverviewView view = new ReviewOverviewView();
            view.setPropertyID(p.getPropertyID());
            view.setTitle(p.getTitle());
            view.setCoverUrl(coverLookup.get(p.getPropertyID()));
            view.setTotalCount(reviews.size());
            view.setUnviewedCount(unviewed);
            summaries.add(view);
            totalReviewsAll += reviews.size();
            reviews.stream().map(Review::getReviewDate).filter(java.util.Objects::nonNull).max(java.util.Comparator.naturalOrder()).ifPresent(d -> latestReview.put(p.getPropertyID(), d));
        }

        java.util.Comparator<java.time.LocalDateTime> newestFirst = java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder());
        summaries.sort(Comparator
                .comparing((ReviewOverviewView v) -> latestReview.get(v.getPropertyID()), newestFirst)
                .thenComparing(ReviewOverviewView::getTitleSortKey));

        model.addAttribute("summaries", summaries);
        model.addAttribute("totalPropertiesCount", summaries.size());
        model.addAttribute("totalReviewsAll", totalReviewsAll);
        return "landlord/reviews-overview";
    }

    // C102 — View Reviews detail (landlord side, one property). Stats
    // (average/high/poor) are always computed from the FULL review list,
    // never just the current page — only the review cards themselves are
    // paginated.
    @GetMapping("/property-reviews/{id}")
    public String viewPropertyReviewsDetail(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);

        List<ReviewRowView> allReviews = reviewRepository.findByPropertyID(id).stream()
                .map(this::toReviewRowView)
                .collect(Collectors.toList());

        // Most recent first
        allReviews.sort((a, b) -> {
            if (a.getReviewDate() == null) return 1;
            if (b.getReviewDate() == null) return -1;
            return b.getReviewDate().compareTo(a.getReviewDate());
        });

        int reviewCount = allReviews.size();
        double averageRating = allReviews.isEmpty() ? 0.0
                : allReviews.stream().mapToInt(ReviewRowView::getRating).average().orElse(0.0);

        long highRatingsCount = allReviews.stream().filter(r -> r.getRating() >= 3).count();
        long poorRatingsCount = allReviews.stream().filter(r -> r.getRating() < 3).count();

        // Pagination — sliced in memory since reviews are already loaded as a List
        int totalItems = allReviews.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) size);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        int fromIndex = currentPage * size;
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<ReviewRowView> pageReviews = totalItems == 0
                ? new ArrayList<>()
                : allReviews.subList(fromIndex, toIndex);

        // NEW — opening a page of reviews counts as viewing them. Only the
        // reviews actually shown on this page are marked, then the sidebar
        // badge is refreshed so it is already correct on this same page.
        List<Integer> shownIds = pageReviews.stream()
                .map(ReviewRowView::getReviewID)
                .collect(Collectors.toList());
        if (!shownIds.isEmpty()) {
            List<Review> toMark = reviewRepository.findAllById(shownIds).stream()
                    .filter(r -> Boolean.FALSE.equals(r.getLandlordViewed()))
                    .collect(Collectors.toList());
            if (!toMark.isEmpty()) {
                toMark.forEach(r -> r.setLandlordViewed(true));
                reviewRepository.saveAll(toMark);
            }
        }
        model.addAttribute("unviewedReviewsCount", countUnviewedReviews(landlordID));

        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageNumbers.add(i);
        }

        // Property switcher: Active properties with at least one Accepted
        // application — unchanged from before.
        List<Property> allMyProperties = propertyRepository.findByLandlordID(landlordID);
        List<Integer> allPropertyIds = allMyProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        Set<Integer> propertyIdsWithAcceptedStudents = applicationRepository.findByPropertyIDIn(allPropertyIds).stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                .map(Application::getPropertyID)
                .collect(Collectors.toSet());

        List<Property> eligibleProperties = findLiveOwnedProperties(landlordID).stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());

        // Arrows: only properties that have reviews, newest review first.
        java.util.Map<Integer, java.time.LocalDateTime> arrowLatest = new java.util.HashMap<>();
        for (Property ep : eligibleProperties) {
            reviewRepository.findByPropertyID(ep.getPropertyID()).stream()
                    .map(Review::getReviewDate)
                    .filter(java.util.Objects::nonNull)
                    .max(java.util.Comparator.naturalOrder())
                    .ifPresent(d -> arrowLatest.put(ep.getPropertyID(), d));
        }
        List<Property> arrowList = eligibleProperties.stream()
                .filter(ep -> arrowLatest.containsKey(ep.getPropertyID()))
                .sorted(Comparator
                        .comparing((Property ep) -> arrowLatest.get(ep.getPropertyID()), Comparator.reverseOrder())
                        .thenComparing(ep -> ep.getTitle() == null ? "" : ep.getTitle().toLowerCase()))
                .collect(Collectors.toList());

        Integer nextPropertyId = property.getPropertyID();
        if (!arrowList.isEmpty()) {
            int currentIndex = -1;
            for (int i = 0; i < arrowList.size(); i++) {
                if (arrowList.get(i).getPropertyID().equals(id)) {
                    currentIndex = i;
                    break;
                }
            }
            nextPropertyId = (currentIndex == -1)
                    ? arrowList.get(0).getPropertyID()
                    : arrowList.get((currentIndex + 1) % arrowList.size()).getPropertyID();
        }
        model.addAttribute("property", property);
        model.addAttribute("reviews", pageReviews);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("highRatingsCount", highRatingsCount);
        model.addAttribute("poorRatingsCount", poorRatingsCount);
        model.addAttribute("eligibleProperties", eligibleProperties);
        model.addAttribute("nextPropertyId", nextPropertyId);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("fromIndex", totalItems == 0 ? 0 : fromIndex + 1);
        model.addAttribute("toIndex", toIndex);
        return "landlord/property-reviews-detail";
    }

    // Clears the "Reported" flag on a review — the review itself is never
    // deleted, only un-flagged. Ownership is checked via the review's
    // property, same pattern as update-application-status.
    @PostMapping("/resolve-review/{reviewId}")
    public String resolveReview(@PathVariable Integer reviewId, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));
        Property property = propertyRepository.findById(review.getPropertyID())
                .orElseThrow(() -> new RuntimeException("Property not found for this review"));
        if (!property.getLandlordID().equals(landlordID)) {
            throw new RuntimeException("You do not have permission to manage this review");
        }

        review.setIsReported(false);
        reviewRepository.save(review);
        return "redirect:/property-reviews/" + property.getPropertyID();
    }

    // ============================================================
    // C200 — View Applications (landlord side)
    // ============================================================

    // CHANGED — "Applications" in the sidebar now lands on the All Properties
    // overview (see viewApplicationsOverview below) instead of jumping
    // straight into one property, so the landlord first sees WHERE the new
    // applications are. This URL is kept (and still landlord-protected) so
    // every existing link, including the redirects after "Accept all" /
    // "Reject all", keeps working. A landlord with no live properties is
    // bounced to the dashboard by the overview itself.
    @GetMapping("/manage-applications")
    public String manageApplications() {
        return "redirect:/applications-overview";
    }

    // NEW — case-insensitive "does this property match what the landlord
    // typed in the search box": name, street address or suburb.
    private boolean propertyMatchesSearch(Property p, String needle) {
        return (p.getTitle() != null && p.getTitle().toLowerCase().contains(needle))
                || (p.getAddress() != null && p.getAddress().toLowerCase().contains(needle))
                || (p.getSuburb() != null && p.getSuburb().toLowerCase().contains(needle));
    }

    // NEW — C200 "All Properties" overview. One card per LIVE property the
    // landlord owns, showing how many applications are Pending / Accepted /
    // Rejected, so the landlord can see at a glance where the activity is
    // without opening every property.
    //
    // Order: properties with the most PENDING (new) applications come first;
    // ties go to the property that received an application most recently,
    // then alphabetically. Properties with nothing pending sink to the end.
    //
    // Search (?q=): matches property name / address / suburb, AND the
    // applicant's name or email. When a search is active the page gets
    // two result lists: matching properties and matching applications.
    @GetMapping("/applications-overview")
    public String viewApplicationsOverview(@RequestParam(required = false) String q,
                                           Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        List<Property> myProperties = findLiveOwnedProperties(landlordID);
        if (myProperties.isEmpty()) {
            return "redirect:/landlord-index?noApplicationsYet=true";
        }

        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());
        List<Application> allApps = applicationRepository.findByPropertyIDIn(propertyIds).stream()
                .filter(a -> a.getPropertyID() != null)
                .collect(Collectors.toList());
        Map<Integer, List<Application>> appsByProperty = allApps.stream()
                .collect(Collectors.groupingBy(Application::getPropertyID));

        // Cover photo per property: the main image if there is one, otherwise
        // whichever image comes first.
        Map<Integer, String> coverLookup = new LinkedHashMap<>();
        for (PropertyImage img : propertyImageRepository.findByPropertyIDIn(propertyIds)) {
            if (img.getUrl() == null) continue;
            boolean isMain = Boolean.TRUE.equals(img.getIsMain());
            if (isMain || !coverLookup.containsKey(img.getPropertyID())) {
                coverLookup.put(img.getPropertyID(), img.getUrl());
            }
        }

        List<ApplicationOverviewView> summaries = new ArrayList<>();
        for (Property p : myProperties) {
            List<Application> apps = appsByProperty.getOrDefault(p.getPropertyID(), new ArrayList<>());

            long pending = apps.stream().filter(a -> isPending(a.getStatus())).count();
            long accepted = apps.stream().filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).count();
            long rejected = apps.stream().filter(a -> "Rejected".equalsIgnoreCase(a.getStatus())).count();

            java.time.LocalDateTime latest = apps.stream()
                    .map(Application::getApplicationDate)
                    .filter(java.util.Objects::nonNull)
                    .max((x, y) -> x.compareTo(y))
                    .orElse(null);
            java.time.LocalDateTime oldestPending = apps.stream()
                    .filter(a -> isPending(a.getStatus()))
                    .map(Application::getApplicationDate)
                    .filter(java.util.Objects::nonNull)
                    .min((x, y) -> x.compareTo(y))
                    .orElse(null);

            int capacity = p.getCapacity() != null ? p.getCapacity() : 0;
            long acceptedPercent = capacity == 0 ? 0 : Math.min(100, Math.round((accepted * 100.0) / capacity));

            ApplicationOverviewView view = new ApplicationOverviewView();
            view.setPropertyID(p.getPropertyID());
            view.setTitle(p.getTitle());
            view.setSuburb(p.getSuburb());
            view.setCoverUrl(coverLookup.get(p.getPropertyID()));
            view.setCapacity(capacity);
            view.setPendingCount(pending);
            view.setAcceptedCount(accepted);
            view.setRejectedCount(rejected);
            view.setTotalCount(apps.size());
            view.setAcceptedPercent(acceptedPercent);
            view.setLatestApplicationDate(latest);
            view.setOldestPendingDate(oldestPending);
            summaries.add(view);
        }

        // Priority order: most pending first, then most recent activity, then A-Z.
        java.util.Comparator<java.time.LocalDateTime> newestFirst = java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder());
        summaries.sort(Comparator
                .comparing(ApplicationOverviewView::getLatestApplicationDate, newestFirst)
                .thenComparing(ApplicationOverviewView::getTitleSortKey));

        long totalPendingAll = summaries.stream().mapToLong(ApplicationOverviewView::getPendingCount).sum();
        long totalAcceptedAll = summaries.stream().mapToLong(ApplicationOverviewView::getAcceptedCount).sum();

        // ── Search ──
        String query = q == null ? "" : q.trim();
        boolean searching = !query.isEmpty();
        List<ApplicationOverviewView> shownSummaries = summaries;
        List<ApplicationRowView> matchingApplications = new ArrayList<>();

        if (searching) {
            String needle = query.toLowerCase();

            Map<Integer, Property> propertyById = myProperties.stream()
                    .collect(Collectors.toMap(Property::getPropertyID, p -> p));

            shownSummaries = summaries.stream()
                    .filter(v -> {
                        Property p = propertyById.get(v.getPropertyID());
                        return p != null && propertyMatchesSearch(p, needle);
                    })
                    .collect(Collectors.toList());

            Set<Integer> studentIds = allApps.stream()
                    .map(Application::getStudentID)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Integer, User> usersById = userRepository.findAllById(studentIds).stream()
                    .collect(Collectors.toMap(User::getUserID, u -> u));

            for (Application app : allApps) {
                User student = usersById.get(app.getStudentID());
                if (student == null) continue;

                String first = student.getFirstName() != null ? student.getFirstName() : "";
                String last = student.getLastName() != null ? student.getLastName() : "";
                String fullName = (first + " " + last).trim();
                String email = student.getEmail() != null ? student.getEmail() : "";

                if (!fullName.toLowerCase().contains(needle) && !email.toLowerCase().contains(needle)) {
                    continue;
                }

                Property p = propertyById.get(app.getPropertyID());

                ApplicationRowView row = new ApplicationRowView();
                row.setApplicationID(app.getApplicationID());
                row.setStudentID(app.getStudentID());
                row.setPropertyID(app.getPropertyID());
                row.setStudentName(fullName.isEmpty() ? "Unknown student" : fullName);
                row.setStudentInitials(initialsOf(student));
                row.setStudentEmail(email);
                row.setStatus(app.getStatus());
                row.setApplicationDate(app.getApplicationDate());
                row.setPropertyName(p != null ? p.getTitle() : "");
                matchingApplications.add(row);
            }
            matchingApplications.sort(this::compareQueueOrder);
        }

        model.addAttribute("summaries", shownSummaries);
        model.addAttribute("matchingApplications", matchingApplications);
        model.addAttribute("searchQuery", query);
        model.addAttribute("searching", searching);
        model.addAttribute("totalPropertiesCount", summaries.size());
        model.addAttribute("totalPendingAll", totalPendingAll);
        model.addAttribute("totalAcceptedAll", totalAcceptedAll);
        model.addAttribute("totalApplicationsAll", allApps.size());
        return "landlord/applications-overview";
    }

    // C200 — View Applications detail (landlord side, one property). Mirrors
    // the property-reviews-detail pattern: a property switcher + next/back
    // arrows across every ACTIVE property this landlord owns, two summary
    // stat cards (Pending % / Accepted %) computed over THIS property's
    // applications only, and the same capacity-based Accept gating as
    // before.
    //
    // NEW — optional ?q= filters the list to students whose name or email
    // contains the text (used by the overview's application search). The
    // stat cards and counts always reflect ALL of the property's
    // applications, never just the filtered ones.
    @GetMapping("/property-applications/{id}")
    public String viewPropertyApplicationsDetail(@PathVariable Integer id,
                                                 @RequestParam(required = false) String q,
                                                 Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);



        List<Application> propApps = applicationRepository.findByPropertyIDIn(List.of(id));

        List<ApplicationRowView> rows = new ArrayList<>();
        for (Application app : propApps) {
            User student = userRepository.findById(app.getStudentID()).orElse(null);
            Student studentProfile = studentRepository.findById(app.getStudentID()).orElse(null);

            ApplicationRowView row = new ApplicationRowView();
            row.setApplicationID(app.getApplicationID());
            row.setStudentID(app.getStudentID());
            row.setPropertyID(app.getPropertyID());
            row.setStatus(app.getStatus());
            row.setApplicationDate(app.getApplicationDate());

            if (student != null) {
                row.setStudentName(student.getFirstName() + " " + student.getLastName());
                row.setStudentInitials(
                        ("" + student.getFirstName().charAt(0) + student.getLastName().charAt(0)).toUpperCase());
                row.setStudentEmail(student.getEmail());
            } else {
                row.setStudentName("Unknown student");
                row.setStudentInitials("?");
            }

            if (studentProfile != null) {
                row.setYearOfStudy(studentProfile.getYearOfStudy());
            }

            // Funding status and the student's message are captured per
            // application now (not on the student's profile), since a
            // student's situation can differ from one application to the
            // next.
            row.setFundingStatus(app.getFundingStatus());
            row.setMessageToLandlord(app.getMessageToLandlord());
            row.setLandlordResponded(app.getLandlordResponded());
            row.setDocuments(applicationDocumentRepository.findByApplicationID(app.getApplicationID()));

            row.setPropertyName(property.getTitle());
            row.setPropertyAddress(buildApplicationAddress(property));
            row.setRoomType(buildApplicationRoomType(property));

            rows.add(row);
        }

        // CHANGED — first come, first served. Pending applications are listed
        // OLDEST FIRST, so the list reads top to bottom in the same order
        // "Accept up to capacity" works through it. Anything already decided
        // (Accepted or Rejected) drops to the end, newest first.
        rows.sort(this::compareQueueOrder);

        long totalCount = rows.size();
        long pendingCount = rows.stream().filter(r -> "Pending".equalsIgnoreCase(r.getStatus())).count();
        long acceptedCount = rows.stream().filter(r -> "Accepted".equalsIgnoreCase(r.getStatus())).count();

        int capacity = property.getCapacity() != null ? property.getCapacity() : 0;

        // FIX: Pending% is NOT "pending applicants ÷ capacity" — that gave
        // wrong numbers like 20 pending applications against capacity 53
        // showing 38% instead of the expected 100%. What's actually wanted:
        // Accepted% = how much of capacity is filled so far, and Pending%
        // = how much capacity is STILL OPEN/unfilled — i.e. the complement
        // of Accepted%, not a separate count-based ratio. So with 0
        // accepted, Pending is 100% regardless of how many applications
        // are sitting in Pending status; with 2 accepted out of 53,
        // Accepted is 4% and Pending (remaining room) is 96%.
        long acceptedPercent = capacity == 0 ? 0 : Math.min(100, Math.round((acceptedCount * 100.0) / capacity));
        long pendingPercent = capacity == 0 ? 0 : 100 - acceptedPercent;

        // NEW — search box on this page: keep only students whose name or
        // email contains the text. Counts above were taken BEFORE this.
        String query = q == null ? "" : q.trim();
        List<ApplicationRowView> displayRows = rows;
        if (!query.isEmpty()) {
            String needle = query.toLowerCase();
            displayRows = rows.stream()
                    .filter(r -> (r.getStudentName() != null && r.getStudentName().toLowerCase().contains(needle))
                            || (r.getStudentEmail() != null && r.getStudentEmail().toLowerCase().contains(needle)))
                    .collect(Collectors.toList());
        }

        // Property switcher: every LIVE (Active) property this landlord
        // owns — deactivated (Inactive) AND not-yet-approved (Draft/
        // Pending) properties are excluded, since applications only really
        // exist against properties students can actually see and apply to.
        // A landlord who already has one of those excluded properties'
        // page open (via a direct/older link) can still view it directly;
        // it just won't appear in this dropdown.
        List<Property> eligibleProperties = findLiveOwnedProperties(landlordID).stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());

        // Arrows: only properties that have applications, newest application first.
        java.util.Map<Integer, java.time.LocalDateTime> arrowLatest = new java.util.HashMap<>();
        if (!eligibleProperties.isEmpty()) {
            List<Integer> arrowIds = eligibleProperties.stream()
                    .map(Property::getPropertyID)
                    .collect(Collectors.toList());
            for (Application ea : applicationRepository.findByPropertyIDIn(arrowIds)) {
                if (ea.getPropertyID() != null && ea.getApplicationDate() != null) {
                    arrowLatest.merge(ea.getPropertyID(), ea.getApplicationDate(), (d1, d2) -> d1.isAfter(d2) ? d1 : d2);
                }
            }
        }
        List<Property> arrowList = eligibleProperties.stream()
                .filter(ep -> arrowLatest.containsKey(ep.getPropertyID()))
                .sorted(Comparator
                        .comparing((Property ep) -> arrowLatest.get(ep.getPropertyID()), Comparator.reverseOrder())
                        .thenComparing(ep -> ep.getTitle() == null ? "" : ep.getTitle().toLowerCase()))
                .collect(Collectors.toList());

        Integer nextPropertyId = property.getPropertyID();
        Integer previousPropertyId = property.getPropertyID();
        if (!arrowList.isEmpty()) {
            int currentIndex = -1;
            for (int i = 0; i < arrowList.size(); i++) {
                if (arrowList.get(i).getPropertyID().equals(id)) {
                    currentIndex = i;
                    break;
                }
            }
            int arrowSize = arrowList.size();
            nextPropertyId = (currentIndex == -1)
                    ? arrowList.get(0).getPropertyID()
                    : arrowList.get((currentIndex + 1) % arrowSize).getPropertyID();
            previousPropertyId = (currentIndex == -1)
                    ? arrowList.get(0).getPropertyID()
                    : arrowList.get((currentIndex - 1 + arrowSize) % arrowSize).getPropertyID();
        }
        // NEW — pending (new) applications per property, so the dropdown can
        // show "Albert Luthuli (3 new)", plus the same properties in
        // priority order (most pending first) for the dropdown to list.
        // The next/back arrows keep using the stable A-Z list above, so a
        // property doesn't jump position right after you accept someone.
        Map<Integer, Long> pendingCountByProperty = new LinkedHashMap<>();
        if (!eligibleProperties.isEmpty()) {
            List<Integer> eligibleIds = eligibleProperties.stream()
                    .map(Property::getPropertyID)
                    .collect(Collectors.toList());
            applicationRepository.findByPropertyIDIn(eligibleIds).stream()
                    .filter(a -> a.getPropertyID() != null && isPending(a.getStatus()))
                    .forEach(a -> pendingCountByProperty.merge(a.getPropertyID(), 1L, Long::sum));
        }
        List<Property> switcherProperties = eligibleProperties.stream()
                .sorted(Comparator
                        .comparingLong((Property p) -> -pendingCountByProperty.getOrDefault(p.getPropertyID(), 0L))
                        .thenComparing(p -> p.getTitle() == null ? "" : p.getTitle().toLowerCase()))
                .collect(Collectors.toList());

        model.addAttribute("property", property);
        model.addAttribute("applications", displayRows);
        model.addAttribute("applicationCount", totalCount);
        model.addAttribute("pendingPercent", pendingPercent);
        model.addAttribute("acceptedPercent", acceptedPercent);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("capacity", capacity);
        model.addAttribute("eligibleProperties", eligibleProperties);
        model.addAttribute("switcherProperties", switcherProperties);
        model.addAttribute("pendingCountByProperty", pendingCountByProperty);
        model.addAttribute("searchQuery", query);
        model.addAttribute("nextPropertyId", nextPropertyId);
        model.addAttribute("previousPropertyId", previousPropertyId); // NEW
        return "landlord/property-applications-detail";
    }

    // Accept/Reject a single application. Checks ownership (a landlord can
    // only act on applications for THEIR OWN properties) and, when
    // accepting, enforces capacity so you can never end up with more
    // Accepted applications than the property can actually hold.
    @PostMapping("/update-application-status/{applicationId}")
    public String updateApplicationStatus(
            @PathVariable Integer applicationId,
            @RequestParam String status,
            Principal principal) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));

        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = propertyRepository.findById(application.getPropertyID())
                .orElseThrow(() -> new RuntimeException("Property not found for this application"));
        if (!property.getLandlordID().equals(landlordID)) {
            throw new RuntimeException("You do not have permission to manage this application");
        }

        String redirectBase = "redirect:/property-applications/" + property.getPropertyID();

        // Only the three statuses the UI can send are allowed. Anything else is
        // ignored, so a hand-crafted POST can't write arbitrary text into the column.
        boolean validStatus = "Accepted".equalsIgnoreCase(status)
                || "Rejected".equalsIgnoreCase(status)
                || "Pending".equalsIgnoreCase(status);
        if (!validStatus) {
            return redirectBase;
        }

        // An accepted student can't be rejected. The Reject button is hidden for
        // accepted cards in the template, and this stops a replayed or
        // hand-crafted POST from doing it anyway.
        if ("Rejected".equalsIgnoreCase(status) && "Accepted".equalsIgnoreCase(application.getStatus())) {
            return redirectBase;
        }

        // Accepting is capped by the property's capacity. Skipped when the
        // application is already Accepted, so re-submitting can't trip the cap.
        if ("Accepted".equalsIgnoreCase(status) && !"Accepted".equalsIgnoreCase(application.getStatus())) {
            long acceptedCount = applicationRepository.findByPropertyIDIn(List.of(property.getPropertyID())).stream()
                    .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                    .count();
            int capacity = property.getCapacity() != null ? property.getCapacity() : 0;
            if (acceptedCount >= capacity) {
                // The applications page shows a toast for this exact query flag.
                return redirectBase + "?capacityFull=true";
            }
        }

        String previousStatus = application.getStatus();
        application.setStatus(status);
        applicationRepository.save(application);

        // Tell the student, but only when the decision actually changed
        // (re-submitting "Accepted" must not notify twice). Moving back to
        // Pending sends nothing.
        if (!status.equalsIgnoreCase(previousStatus)) {
            if ("Accepted".equalsIgnoreCase(status)) {
                notifyStudentOfDecision(application, property, true);
            } else if ("Rejected".equalsIgnoreCase(status)) {
                notifyStudentOfDecision(application, property, false);
            }
        }
        return redirectBase;
    }

    // "Accept up to capacity" for ONE property: FIRST COME, FIRST SERVED —
    // accepts the students who applied earliest (see
    // compareFirstComeFirstServed) until capacity is reached, then rejects
    // everyone else who is still pending for that same property. One-click
    // way to fill a listing.
    @PostMapping("/accept-up-to-capacity/{propertyId}")
    public String acceptUpToCapacity(@PathVariable Integer propertyId, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(propertyId, landlordID);

        List<Application> propApps = applicationRepository.findByPropertyIDIn(List.of(propertyId));

        long acceptedCount = propApps.stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                .count();
        int capacity = property.getCapacity() != null ? property.getCapacity() : 0;
        long remaining = capacity - acceptedCount;

        List<Application> pending = propApps.stream()
                .filter(a -> "Pending".equalsIgnoreCase(a.getStatus()))
                .sorted(this::compareFirstComeFirstServed) // earliest applicant first
                .collect(Collectors.toList());

        for (int i = 0; i < pending.size(); i++) {
            Application app = pending.get(i);
            boolean accepting = i < remaining;
            app.setStatus(accepting ? "Accepted" : "Rejected");
            applicationRepository.save(app);
            notifyStudentOfDecision(app, property, accepting);
        }

        return "redirect:/property-applications/" + propertyId;
    }

    // Builds "Single Room" / "Sharing" / "Room" text for the applications
    // table's Property & Room column.
    private String buildApplicationRoomType(Property property) {
        StringBuilder sb = new StringBuilder();
        sb.append(property.getType() != null && !property.getType().isBlank() ? property.getType() : "Room");
        return sb.toString();
    }

    // CHANGED — shows the suburb only (city is always Gqeberha, so it adds
    // nothing). Falls back to the raw address if no suburb is set.
    private String buildApplicationAddress(Property property) {
        if (property.getSuburb() != null && !property.getSuburb().isBlank()) {
            return property.getSuburb();
        }
        return property.getAddress() != null ? property.getAddress() : "";
    }

    // Accept All — across EVERY property this landlord owns, accepts the
    // earliest applicants first (first come, first served), up to each
    // property's own remaining capacity. Properties already full, or with no
    // capacity set at all, are skipped entirely. This is the whole-portfolio
    // version of acceptUpToCapacity above (which only does one property at a
    // time).
    @PostMapping("/accept-all-applications")
    public String acceptAllApplications(Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        List<Property> myProperties = propertyRepository.findByLandlordID(landlordID);
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        List<Application> allApplications = applicationRepository.findByPropertyIDIn(propertyIds);
        Map<Integer, List<Application>> byProperty = allApplications.stream()
                .collect(Collectors.groupingBy(Application::getPropertyID));
        Map<Integer, Property> propertyLookup = myProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

        for (Map.Entry<Integer, List<Application>> entry : byProperty.entrySet()) {
            Property property = propertyLookup.get(entry.getKey());
            if (property == null || property.getCapacity() == null) continue;

            List<Application> propApps = entry.getValue();
            long acceptedCount = propApps.stream()
                    .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                    .count();
            long remaining = property.getCapacity() - acceptedCount;
            if (remaining <= 0) continue;

            List<Application> pending = propApps.stream()
                    .filter(a -> "Pending".equalsIgnoreCase(a.getStatus()))
                    .sorted(this::compareFirstComeFirstServed) // earliest applicant first
                    .collect(Collectors.toList());

            for (int i = 0; i < pending.size() && i < remaining; i++) {
                Application app = pending.get(i);
                app.setStatus("Accepted");
                applicationRepository.save(app);
                notifyStudentOfDecision(app, property, true);
            }
        }

        return "redirect:/manage-applications";
    }

    // Reject All — for every property this landlord owns that is ALREADY
    // at full capacity, rejects its remaining Pending applications.
    // Properties that still have space are left completely untouched, so
    // this can never reject an application that could still be accepted.
    @PostMapping("/reject-all-applications")
    public String rejectAllApplications(Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        List<Property> myProperties = propertyRepository.findByLandlordID(landlordID);
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        List<Application> allApplications = applicationRepository.findByPropertyIDIn(propertyIds);
        Map<Integer, List<Application>> byProperty = allApplications.stream()
                .collect(Collectors.groupingBy(Application::getPropertyID));
        Map<Integer, Property> propertyLookup = myProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

        for (Map.Entry<Integer, List<Application>> entry : byProperty.entrySet()) {
            Property property = propertyLookup.get(entry.getKey());
            if (property == null || property.getCapacity() == null) continue;

            List<Application> propApps = entry.getValue();
            long acceptedCount = propApps.stream()
                    .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                    .count();
            long remaining = property.getCapacity() - acceptedCount;
            if (remaining > 0) continue; // still has space — leave for manual review / Accept All

            for (Application app : propApps) {
                if ("Pending".equalsIgnoreCase(app.getStatus())) {
                    app.setStatus("Rejected");
                    applicationRepository.save(app);
                    notifyStudentOfDecision(app, property, false);
                }
            }
        }

        return "redirect:/manage-applications";
    }

    // ============================================================
    // Student-side endpoints
    // ============================================================

      @PostMapping("/apply/{propertyId}")
    public String applyToProperty(
            @PathVariable Integer propertyId,
            @RequestParam(required = false) Integer yearOfStudy,
            @RequestParam(required = false) String fundingStatus,
            @RequestParam(required = false) String messageToLandlord,
            @RequestParam(value = "documents", required = false) MultipartFile documents,
            Principal principal) throws IOException {
        // NOTE: kept as required = false (rather than true) deliberately —
        // Spring's required=true throws a MissingServletRequestPartException
        // BEFORE this method body runs, which shows a raw 400 error page.
        // The explicit null/empty check below gives the student a friendly
        // message instead.

        Integer studentID = getCurrentUser(principal).getUserID();

        // Block applying to a suspended listing or one belonging to a
        // deactivated landlord, even via a direct POST.
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + propertyId));
        if (!isVisibleToStudents(property)) {
            return "redirect:/student-dashboard?propertyUnavailable=true";
        }

        // Occupant restriction: "Female only" / "Male only" listings accept
        // only students of that gender. Enforced here on the server, so a
        // direct POST cannot bypass it. Students with no gender on file
        // (registered before the gender field existed) are also blocked
        // from gender-restricted listings.
        String occupantRule = property.getOccupantRestriction();
        if (occupantRule != null) {
            String genderNeeded = null;
            if (occupantRule.trim().equalsIgnoreCase("Female only")) genderNeeded = "Female";
            else if (occupantRule.trim().equalsIgnoreCase("Male only")) genderNeeded = "Male";
            if (genderNeeded != null) {
                String applicantGender = getCurrentUser(principal).getGender();
                if (applicantGender == null || !genderNeeded.equalsIgnoreCase(applicantGender.trim())) {
                    return "redirect:/property/" + propertyId + "?applyError=gender-restricted";
                }
            }
        }

        // Year-of-study restrictions: "Seniors Only" needs year 2 or above,
        // "First Years Only" needs year 1. Server-side, so a direct POST
        // cannot bypass it. Uses the year on this application form if sent,
        // otherwise the year already on the student's profile.
        String yearRule = property.getOccupantRestriction();
        if (yearRule != null) {
            String yr = yearRule.trim().toLowerCase();
            boolean seniorsOnly = yr.startsWith("senior");
            boolean firstYearsOnly = yr.startsWith("first year");
            if (seniorsOnly || firstYearsOnly) {
                Integer applicantYear = (yearOfStudy != null)
                        ? yearOfStudy
                        : studentRepository.findById(studentID).map(Student::getYearOfStudy).orElse(null);
                if (seniorsOnly && (applicantYear == null || applicantYear < 2)) {
                    return "redirect:/property/" + propertyId + "?applyError=senior-restricted";
                }
                if (firstYearsOnly && (applicantYear == null || applicantYear != 1)) {
                    return "redirect:/property/" + propertyId + "?applyError=first-year-restricted";
                }
            }
        }

        // Funding rule: if the landlord accepts only one funding type, the
        // student's choice is ignored and the property's type is used. This
        // is the real enforcement; the form only mirrors it.
        String required = property.getAcceptedFunding();
        if (required != null && !required.isBlank() && !"Any".equalsIgnoreCase(required)) {
            fundingStatus = required;
        } else if ("Self-funded".equalsIgnoreCase(fundingStatus)) {
            fundingStatus = "Self-Paying";
        }

        // One application per student per property.
        if (applicationRepository.existsByStudentIDAndPropertyID(studentID, propertyId)) {
            return "redirect:/property/" + propertyId + "?applyError=already-applied";
        }

        // Documents are compulsory: a zip of supporting documents.
        if (documents == null || documents.isEmpty()) {
            return "redirect:/property/" + propertyId + "?applyError=missing-file";
        }
        String filename = documents.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            return "redirect:/property/" + propertyId + "?applyError=invalid-file";
        }

        // Year of study lives on the student's PROFILE (not per-application).
        if (yearOfStudy != null) {
            Student studentProfile = studentRepository.findById(studentID).orElseGet(() -> {
                Student s = new Student();
                s.setStudentID(studentID);
                return s;
            });
            studentProfile.setYearOfStudy(yearOfStudy);
            studentRepository.save(studentProfile);
        }

        Application application = new Application();
        application.setStudentID(studentID);
        application.setPropertyID(propertyId);
        application.setStatus(PENDING_STATUS);
        application.setApplicationDate(java.time.LocalDateTime.now());
        application.setFundingStatus(fundingStatus);
        application.setMessageToLandlord(messageToLandlord);
        Application savedApplication = applicationRepository.save(application);

        // Store the zip. If storing fails, roll back the application row and
        // send the student back with a friendly error instead of a raw 500.
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String uniqueFileName = savedApplication.getApplicationID() + "_" + System.currentTimeMillis()
                    + "_" + documents.getOriginalFilename();
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(documents.getInputStream(), filePath);

            ApplicationDocument doc = new ApplicationDocument();
            doc.setApplicationID(savedApplication.getApplicationID());
            doc.setFileName(documents.getOriginalFilename());
            doc.setFilePath(filePath.toString());
            doc.setUploadedAt(java.time.LocalDateTime.now());
            applicationDocumentRepository.save(doc);
        } catch (Exception ex) {
            System.err.println("[APPLY] Document save failed for property " + propertyId
                    + " / application " + savedApplication.getApplicationID() + ": " + ex);
            ex.printStackTrace();
            applicationRepository.deleteById(savedApplication.getApplicationID());
            return "redirect:/property/" + propertyId + "?applyError=upload-failed";
        }

        // Notify both sides now that the application and its documents are
        // fully saved: the student's bell and the landlord's bell.
        try {
            studentNotificationService.applicationSubmitted(studentID, property.getTitle());
        } catch (Exception ignored) {
            // a notification problem must never fail the application
        }
        landlordNotificationService.notifyStudentApplied(savedApplication); // never throws

        // Centered success popup is shown by property-detail.html when it sees ?applied=1
        return "redirect:/property/" + propertyId + "?applied=1";
    }

    // A200 — View Applications (student's own)
    @GetMapping("/my-applications")
    public String viewApplications(Model model, Principal principal) {
        User currentUser = getCurrentUser(principal);
        Integer studentID = currentUser.getUserID();

        // Newest first so the most recent application is at the top.
        List<Application> applications = applicationRepository.findByStudentID(studentID).stream()
                .sorted(Comparator.comparing(Application::getApplicationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // FIX: the old template read ${propertyNames[...]} and ${currentUserName},
        // but neither was ever set — indexing the null map threw during render
        // as soon as a student had an application. Now supplies the full
        // Property per application (title, photo, suburb, rent) in one
        // findAllById call; a deleted property is just absent from the map.
        List<Integer> propertyIds = applications.stream()
                .map(Application::getPropertyID)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Property> applicationProperties = propertyRepository.findAllById(propertyIds).stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p, (a, b) -> a));

        String lastName = currentUser.getLastName();
        String initials = initialsOf(currentUser); // null-safe (see viewStudentDashboard)

        model.addAttribute("applications", applications);
        model.addAttribute("applicationProperties", applicationProperties);
        model.addAttribute("currentUserName", currentUser.getFirstName());
        model.addAttribute("userInitials", initials);
        model.addAttribute("totalApplications", applications.size());
        model.addAttribute("pendingCount", applications.stream().filter(a -> "Pending".equalsIgnoreCase(a.getStatus())).count());
        model.addAttribute("acceptedCount", applications.stream().filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).count());
        model.addAttribute("rejectedCount", applications.stream().filter(a -> "Rejected".equalsIgnoreCase(a.getStatus())).count());
        model.addAttribute("savedCount", savedPropertyRepository.findByStudentID(studentID).size());
        // FIX: this template was moved to templates/student/my-applications.html
        // during the frontend reorg — returning "my-applications" here throws
        // Thymeleaf's TemplateInputException ("template might not exist").
        return "student/my-applications";
    }

    // A202 — Cancel Application
    @PostMapping("/applications/cancel/{applicationId}")
        @PostMapping("/applications/cancel/{applicationId}")
    public String cancelApplication(@PathVariable Integer applicationId, Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();

        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null || !application.getStudentID().equals(studentID)) {
            return "redirect:/my-applications";
        }

        List<ApplicationDocument> documents = applicationDocumentRepository.findByApplicationID(applicationId);
        for (ApplicationDocument document : documents) {
            if (document.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(document.getFilePath()));
                } catch (IOException e) {
                    // Suppress deletion errors so DB rows can still be cleared safely
                }
            }
        }
        applicationDocumentRepository.deleteAll(documents);
        applicationRepository.deleteById(applicationId);
        return "redirect:/my-applications";
    }
        
        List<ApplicationDocument> documents = applicationDocumentRepository.findByApplicationID(applicationId);
        for (ApplicationDocument document : documents) {
            if (document.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(document.getFilePath()));
                } catch (IOException e) {
                    // Suppress deletion errors so DB rows can still be cleared safely
                }
            }
        }
        applicationDocumentRepository.deleteAll(documents);
        applicationRepository.deleteById(applicationId);
        return "redirect:/my-applications";
    }


    // Tells a student their application was accepted or rejected. Never
    // throws: a notification problem must not undo the landlord's decision.
    private void notifyStudentOfDecision(Application application, Property property, boolean accepted) {
        try {
            studentNotificationService.applicationDecision(
                    application.getStudentID(), property.getTitle(), accepted);
        } catch (Exception ignored) {
            // intentionally ignored, see comment above
        }
    }

    private boolean toggleSaved(Integer studentID, Integer propertyId) {
        List<SavedProperty> existing = savedPropertyRepository.findByStudentIDAndPropertyID(studentID, propertyId);
        if (!existing.isEmpty()) {
            savedPropertyRepository.deleteAll(existing);
            return false;
        }
        SavedProperty saved = new SavedProperty();
        saved.setStudentID(studentID);
        saved.setPropertyID(propertyId);
        saved.setSavedStatus(true);
        savedPropertyRepository.save(saved);

        // Only on ADD (the remove branch above returns early), so un-saving
        // and re-saving doesn't pile up duplicate "removed" noise.
        try {
            String savedTitle = propertyRepository.findById(propertyId).map(Property::getTitle).orElse(null);
            studentNotificationService.favouriteAdded(studentID, propertyId, savedTitle);
        } catch (Exception ignored) {
            // a notification problem must never block saving a property
        }
        return true;
    }

    // Normal form post (Saved Properties page): toggle, then redirect back.
    @PostMapping("/favorite/{propertyId}")
    public String addFavorite(@PathVariable Integer propertyId,
                              @RequestParam(required = false) String returnTo,
                              Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();
        toggleSaved(studentID, propertyId);

        boolean safeReturnTo = "/saved-properties".equals(returnTo) || "/student-dashboard".equals(returnTo);
        return "redirect:" + (safeReturnTo ? returnTo : "/student-dashboard");
    }

    // NEW — fetch() version used by the dashboard heart. Spring picks this one
    // when the request carries the X-Requested-With: XMLHttpRequest header.
    // Returns {"saved": true|false} instead of redirecting.
    @PostMapping(value = "/favorite/{propertyId}", headers = "X-Requested-With=XMLHttpRequest")
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> toggleFavoriteAjax(@PathVariable Integer propertyId, Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();
        boolean saved = toggleSaved(studentID, propertyId);
        return Map.of("saved", saved);
    }

    // Saved Properties page — lists every property this student has saved.
    @GetMapping("/saved-properties")
    public String viewSavedProperties(Model model, Principal principal) {
        User currentUser = getCurrentUser(principal);
        Integer studentID = currentUser.getUserID();

        List<Integer> savedPropertyIds = savedPropertyRepository.findByStudentID(studentID).stream()
                .map(SavedProperty::getPropertyID)
                .collect(Collectors.toList());

        List<Property> savedProperties = propertyRepository.findAllById(savedPropertyIds);

        // Sidebar/top-bar data shared with My Dashboard and My Applications.
        String lastName = currentUser.getLastName();
        String initials = initialsOf(currentUser); // null-safe (see viewStudentDashboard)
        model.addAttribute("currentUserFirstName", currentUser.getFirstName());
        model.addAttribute("userInitials", initials);
        model.addAttribute("savedCount", savedProperties.size());
        model.addAttribute("totalApplications", applicationRepository.findByStudentID(studentID).size());

        model.addAttribute("properties", savedProperties);
        return "saved-properties";
    }

    // My Dashboard — the student's personal overview: saved/pending/accepted
    // counts, a short preview of saved properties, and a short preview of
    // recent applications. Both previews reuse the exact lists the
    // dedicated Saved Properties / My Applications pages already load.
    @GetMapping("/my-dashboard")
    public String viewMyDashboard(Model model, Principal principal) {
        User currentUser = getCurrentUser(principal);
        Integer studentID = currentUser.getUserID();

        List<Integer> savedPropertyIds = savedPropertyRepository.findByStudentID(studentID).stream()
                .map(SavedProperty::getPropertyID)
                .collect(Collectors.toList());
        List<Property> savedProperties = propertyRepository.findAllById(savedPropertyIds);

        List<Application> applications = applicationRepository.findByStudentID(studentID);
        long pendingCount = applications.stream().filter(a -> "Pending".equalsIgnoreCase(a.getStatus())).count();
        long acceptedCount = applications.stream().filter(a -> "Accepted".equalsIgnoreCase(a.getStatus())).count();

        long rejectedCount = applications.stream().filter(a -> "Rejected".equalsIgnoreCase(a.getStatus())).count();

        // Most recent applications first, same ordering used elsewhere.
        List<Application> recentApplications = applications.stream()
                .sorted(Comparator.comparing(Application::getApplicationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        List<Application> applicationsPreview =
                recentApplications.size() > 4 ? recentApplications.subList(0, 4) : recentApplications;

        // propertyID -> Property for the application cards, so the template
        // can show the real title / photo / suburb instead of "Property ID: 7".
        // One findAllById call (same lookup-map pattern as the landlord
        // dashboard's imageLookup). A deleted property is simply absent from
        // the map and the template falls back to the ID.
        List<Integer> appPropertyIds = applicationsPreview.stream()
                .map(Application::getPropertyID)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Property> applicationProperties = propertyRepository.findAllById(appPropertyIds).stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p, (a, b) -> a));

        String lastName = currentUser.getLastName();
        String initials = initialsOf(currentUser); // null-safe (see viewStudentDashboard)

        model.addAttribute("currentUserFirstName", currentUser.getFirstName());
        model.addAttribute("userInitials", initials);
        model.addAttribute("savedCount", savedProperties.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalApplications", applications.size());
        model.addAttribute("savedPreview", savedProperties.size() > 3 ? savedProperties.subList(0, 3) : savedProperties);
        model.addAttribute("applicationsPreview", applicationsPreview);
        model.addAttribute("applicationProperties", applicationProperties);
        return "student/my-dashboard";
    }


    // A201 — Submit Documents
    @PostMapping("/submit-documents/{applicationId}")
    public String submitDocuments(@PathVariable Integer applicationId, @RequestParam("file") MultipartFile file) throws IOException {

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String uniqueFileName = applicationId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), filePath);

        ApplicationDocument doc = new ApplicationDocument();
        doc.setApplicationID(applicationId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(filePath.toString());
        doc.setUploadedAt(java.time.LocalDateTime.now());
        applicationDocumentRepository.save(doc);

        return "redirect:/my-applications";
    }

    // Category icon lookup — plain Java, not Thymeleaf ternaries, so there's
// no parser depth limit and it's trivial to extend with new categories.
    private String resolveCategoryIcon(String category) {
        String c = category == null ? "" : category.toLowerCase();
        if (c.contains("facilit")) return "🏢";
        if (c.contains("secur")) return "🔒";
        if (c.contains("util")) return "💡";
        if (c.contains("outdoor")) return "🌳";
        if (c.contains("kitchen")) return "🍳";
        if (c.contains("bathroom")) return "🚿";
        if (c.contains("laundry")) return "🧺";
        if (c.contains("internet") || c.contains("connectiv")) return "📶";
        if (c.contains("room")) return "🛏️";
        return "🏷️";
    }

    // Per-amenity icon lookup — same reasoning as above.
    private String resolveAmenityIcon(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.contains("wifi") || n.contains("internet")) return "📶";
        if (n.contains("parking")) return "🅿️";
        if (n.contains("security") || n.contains("guard") || n.contains("cctv")) return "🔒";
        if (n.contains("pool")) return "🏊";
        if (n.contains("gym")) return "🏋️";
        if (n.contains("laundry")) return "🧺";
        if (n.contains("kitchen")) return "🍳";
        if (n.contains("study") || n.contains("desk")) return "📚";
        if (n.contains("shuttle") || n.contains("transport")) return "🚌";
        if (n.contains("power") || n.contains("backup") || n.contains("generator")) return "🔋";
        if (n.contains("water")) return "🚿";
        if (n.contains("furnish")) return "🛋️";
        if (n.contains("wardrobe")) return "🚪";
        if (n.contains("tv") || n.contains("flatscreen")) return "📺";
        return "✓";
    }

    // Builds the display-ready category → items structure the template
// iterates over. Replaces the old amenitiesByCategory Map<String,List<Amenity>>
// + inline ternary icon lookups.
    private List<AmenityCategoryView> buildAmenityCategoryViews(Property property) {
        if (property.getAmenities() == null || property.getAmenities().isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<Amenity>> grouped = property.getAmenities().stream()
                .collect(Collectors.groupingBy(Amenity::getCategory, LinkedHashMap::new, Collectors.toList()));

        List<AmenityCategoryView> result = new ArrayList<>();
        for (Map.Entry<String, List<Amenity>> entry : grouped.entrySet()) {
            AmenityCategoryView catView = new AmenityCategoryView();
            catView.setCategoryName(entry.getKey());
            catView.setCategoryIcon(resolveCategoryIcon(entry.getKey()));

            List<AmenityItemView> items = new ArrayList<>();
            for (Amenity amenity : entry.getValue()) {
                AmenityItemView item = new AmenityItemView();
                item.setName(amenity.getName());
                item.setIcon(resolveAmenityIcon(amenity.getName()));
                items.add(item);
            }
            catView.setItems(items);
            result.add(catView);
        }
        return result;
    }



    // ============================================================
    // C100 — List a New Property (creation wizard)
    // ============================================================

    // Shows the blank wizard, passing amenities (grouped by category) and
    // the semester dropdown options for the JS to render.
    // NEW — a deactivated landlord (User.isActive == false, see
    // AdminController.deactivateUser()) can no longer open this wizard at
    // all. Previously nothing stopped a deactivated account from still
    // submitting new listings even though their existing properties were
    // hidden. Redirects back to the dashboard with a flag the toast can key
    // off, same pattern as the other ?toast=... redirects on that page.
    @GetMapping({"/listProperty", "/list-property"})
    public String listPropertyForm(Model model, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (Boolean.FALSE.equals(currentUser.getIsActive())) {
            return "redirect:/landlord-index?accountDeactivated=true";
        }

        List<Amenity> allAmenities = amenityRepository.findAllByOrderByCategoryAscNameAsc();
        Map<String, List<Amenity>> amenityCategories = allAmenities.stream()
                .collect(Collectors.groupingBy(Amenity::getCategory, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("amenityCategories", amenityCategories);
        model.addAttribute("availableFromOptions", generateSemesterOptions());
        return "landlord/listProperty";
    }

    // Lets a landlord download a document a student attached to their
    // application. Ownership is checked via the document → application →
    // property chain, so a landlord can only ever open documents attached
    // to applications for their own properties.
    @GetMapping("/download-application-document/{documentId}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadApplicationDocument(
            @PathVariable Integer documentId, Principal principal) throws IOException {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));
        Application application = applicationRepository.findById(doc.getApplicationID())
                .orElseThrow(() -> new RuntimeException("Application not found for this document"));

        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = propertyRepository.findById(application.getPropertyID())
                .orElseThrow(() -> new RuntimeException("Property not found for this application"));
        if (!property.getLandlordID().equals(landlordID)) {
            throw new RuntimeException("You do not have permission to view this document");
        }

        Path filePath = Paths.get(doc.getFilePath());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
        if (!resource.exists()) {
            throw new RuntimeException("File not found on disk: " + doc.getFilePath());
        }

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    // Creates the property (also handles "Save as Draft"). New listings
    // always start with isAvailable=false — a property only becomes
    // available once an Admin approves it AND the landlord marks it
    // available (see togglePropertyStatus).
    // NEW — same deactivated-account block as listPropertyForm() above,
    // repeated here because a deactivated landlord could otherwise still
    // POST directly to this endpoint (e.g. resubmitting a form they had
    // open in another tab, or a raw request) even with the GET wizard
    // blocked.
    @PostMapping("/listProperty")
    public String listProperty(
            @RequestParam String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String address,
            // CHANGED — was "city". The wizard now sends "suburb" (a dropdown).
            @RequestParam(required = false) String suburb,
            @RequestParam(required = false) java.math.BigDecimal rent,
            @RequestParam(required = false) java.math.BigDecimal deposit,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String commuteType,
            @RequestParam(required = false) String occupantRestriction,
            @RequestParam(required = false) String acceptedFunding,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(value = "amenityIds", required = false) List<Integer> amenityIds,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "action", defaultValue = "submit") String action,
            Principal principal) throws IOException {

        User currentUser = getCurrentUser(principal);
        if (Boolean.FALSE.equals(currentUser.getIsActive())) {
            return "redirect:/landlord-index?accountDeactivated=true";
        }
        Integer landlordID = currentUser.getUserID();
        boolean isDraft = "draft".equals(action);

        // NEW — property names must be unique across all properties. The
        // wizard asks /check-property-title first, so a landlord normally
        // never gets here with a taken name; this is the backstop for a
        // bypassed or racing request. Nothing has been saved yet.
        if (isTitleTaken(title, null)) {
            return "redirect:/list-property?error=duplicate-title";
        }

        // Set when a "submit" had to be saved as a draft because a required
        // field was missing or the rent was too low — the landlord is told why.
        boolean downgradedToDraft = false;

        Property property = new Property();
        property.setLandlordID(landlordID);
        property.setTitle(title != null ? title.trim() : null);
        property.setType(type);
        property.setAddress(address);
        // CHANGED — the landlord picks a suburb; the city is always the same.
        property.setSuburb(suburb != null && !suburb.isBlank() ? suburb.trim() : null);
        property.setCity(CITY);
        property.setRent(rent);
        property.setDeposit(deposit);
        property.setDescription(description);
        property.setCommuteType(commuteType);
        // Defaults if the wizard didn't send a value: restriction "Mixed",
        // funding "Any" (landlord takes every funding type).
        property.setOccupantRestriction(
                (occupantRestriction != null && !occupantRestriction.isBlank()) ? occupantRestriction : "Mixed");
        property.setAcceptedFunding(
                (acceptedFunding != null && !acceptedFunding.isBlank()) ? acceptedFunding : "Any");
        // CHANGED — no more silent default of 1. A landlord has to enter the
        // capacity themselves. If it's missing, the listing can only be saved
        // as a Draft (meetsPublishRequirements below enforces that), and the
        // edit page flags the empty field until they fill it in.
        property.setCapacity(capacity);
        property.setIsAvailable(false);

        // ── Server-side enforcement (mirrors the wizard's client-side JS,
        //    but this is the check that actually can't be bypassed) ──
        // Even if the client claims action=submit, a listing missing a
        // required field, with no address, or with rent below R1 000, can
        // never go to "Pending" — it is silently saved as "Draft" instead.
        // (Create keeps the silent-downgrade behaviour rather than
        // rejecting outright, because the wizard's state lives entirely in
        // client-side JS — bouncing the request back would lose everything
        // the landlord typed across all seven steps. The update path below
        // has no such constraint, since the edit form is fully
        // server-rendered and every field persists in the DOM regardless.)
        if (!isDraft && (!meetsPublishRequirements(property.getTitle(), property.getRent(), property.getAddress(), property.getCapacity())
                || !hasRequiredPhotos(coverImage, images))) {
            return "redirect:/list-property?error=incomplete";
        }
        property.setStatus(isDraft ? "Draft" : "Pending");

        if (availableFrom != null && !availableFrom.isBlank()) {
            property.setAvailableFrom(java.time.LocalDate.parse(availableFrom));
        }

        property.setAmenities(amenityIds != null
                ? amenityRepository.findAllById(amenityIds)
                : new ArrayList<>());

        Property savedProperty = propertyRepository.save(property);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        int order = 1;

        // FIX: previously, if coverImage was empty/not sent (e.g. the
        // wizard only submits the "images" gallery field, no separate
        // cover upload), EVERY gallery photo got hardcoded isMain=false —
        // the property ended up with photos but no main image, permanently
        // (updateProperty's append logic below can't fix this after the
        // fact either, see the FIX note there). Track whether the cover
        // slot was actually filled so the first gallery photo can take
        // over as main when it wasn't.
        boolean coverImageProvided = coverImage != null && !coverImage.isEmpty();

        // Cover image is always marked isMain=true and saved first.
        if (coverImageProvided) {
            String uniqueFileName = savedProperty.getPropertyID() + "_" + System.currentTimeMillis() + "_" + coverImage.getOriginalFilename();
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(coverImage.getInputStream(), filePath);

            PropertyImage coverPropImage = new PropertyImage();
            coverPropImage.setPropertyID(savedProperty.getPropertyID());
            coverPropImage.setUrl("/uploads/" + uniqueFileName);
            coverPropImage.setCategory("exterior");
            coverPropImage.setIsMain(true);
            coverPropImage.setDisplayOrder(order);
            propertyImageRepository.save(coverPropImage);
            order++;
        }

        // Remaining gallery photos, in upload order. If there was no cover
        // image, the FIRST non-empty gallery photo becomes the main image
        // instead of every photo defaulting to isMain=false.
        boolean firstGalleryImageHandled = false;
        if (images != null) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String uniqueFileName = savedProperty.getPropertyID() + "_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(image.getInputStream(), filePath);

                    boolean isMain = !coverImageProvided && !firstGalleryImageHandled;

                    PropertyImage propImage = new PropertyImage();
                    propImage.setPropertyID(savedProperty.getPropertyID());
                    propImage.setUrl("/uploads/" + uniqueFileName);
                    propImage.setCategory("exterior");
                    propImage.setIsMain(isMain);
                    propImage.setDisplayOrder(order);
                    propertyImageRepository.save(propImage);
                    order++;

                    if (isMain) {
                        firstGalleryImageHandled = true;
                    }
                }
            }
        }

        if (isDraft) {
            return downgradedToDraft
                    ? "redirect:/landlord-index?draftSaved=true&incomplete=true"
                    : "redirect:/landlord-index?draftSaved=true";
        }
        return "redirect:/landlord-index?added=true";
    }
   
}


