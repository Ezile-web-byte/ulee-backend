package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import com.ulee.ulee_backend.repository.PropertyRepository;
import com.ulee.ulee_backend.repository.SavedPropertyRepository;
import com.ulee.ulee_backend.repository.ApplicationRepository;
import com.ulee.ulee_backend.repository.ReviewRepository;
import com.ulee.ulee_backend.repository.ApplicationDocumentRepository;
import com.ulee.ulee_backend.repository.AmenityRepository;
import com.ulee.ulee_backend.repository.PropertyFeatureRepository;
import com.ulee.ulee_backend.repository.PropertyFeatureImageRepository;
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
    @Autowired
    private PropertyFeatureRepository propertyFeatureRepository;
    @Autowired
    private PropertyFeatureImageRepository propertyFeatureImageRepository;

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

    // The one exact status string that means "live and visible to students".
    // Every query/branch that decides visibility should use this constant
    // instead of typing "Approved" by hand, so a typo can't silently break
    // visibility somewhere.
    private static final String LIVE_STATUS = "Approved";

    // The one exact status string that means "deactivated / soft-deleted".
    // Any list of a landlord's properties that's meant to represent what
    // still "exists" (dashboards, switchers, dropdowns) should exclude
    // this, the same way LIVE_STATUS marks what's visible to students.
    private static final String DEACTIVATED_STATUS = "Inactive";

    // ── Small reusable helpers, used by many endpoints below ──

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
    // place instead of re-typed in every endpoint. Draft/Pending properties
    // ARE included here, since the landlord is still actively managing
    // those from their dashboard.
    private List<Property> findActiveOwnedProperties(Integer landlordID) {
        return propertyRepository.findByLandlordID(landlordID).stream()
                .filter(p -> !DEACTIVATED_STATUS.equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    // Stricter than findActiveOwnedProperties: only status == "Approved".
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

    // Generates the semester-start options for the "Available From"
    // dropdown: Jan and July of each year, one year back through three
    // years ahead.
    private List<LocalDate> generateSemesterOptions() {
        List<LocalDate> options = new ArrayList<>();
        int startYear = LocalDate.now().getYear() - 1;
        int endYear = LocalDate.now().getYear() + 3;
        for (int year = startYear; year <= endYear; year++) {
            options.add(LocalDate.of(year, 1, 1));
            options.add(LocalDate.of(year, 7, 1));
        }
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

    // Public-facing: only properties whose status is exactly "Approved"
    // (LIVE_STATUS) show up here. isAvailable is NOT checked on this
    // endpoint — status is the single source of truth for whether students
    // can see a listing at all.
    @GetMapping("/student-dashboard")
    public String viewStudentDashboard(Model model) {
        model.addAttribute("properties", propertyRepository.findByStatus(LIVE_STATUS));
        return "student/student-dashboard";
    }

    // ============================================================
    // C100 support — Landlord Dashboard ("My Properties" inventory page)
    // ============================================================
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
        long activeListings = myProperties.stream().filter(Property::getIsAvailable).count();
        long vacantRooms = totalProperties - activeListings;

        // ── FIX: "Awaiting Review" card ──────────────────────────────────
        // This counts PROPERTIES this landlord owns that are sitting in
        // "Pending" status — i.e. submitted, but not yet approved by an
        // Admin. This is deliberately separate from pendingApplications
        // below (which counts STUDENT APPLICATIONS waiting on the
        // landlord's own Accept/Reject decision — a completely different
        // thing). The dashboard's "Awaiting Review" label was previously
        // wired to pendingApplications by mistake, which is why creating a
        // new Pending property never moved that number.
        long awaitingApprovalCount = myProperties.stream()
                .filter(p -> "Pending".equals(p.getStatus()))
                .count();

        List<Application> myApplications = applicationRepository.findByPropertyIDIn(propertyIds);
        long pendingApplications = myApplications.stream().filter(a -> "Pending".equals(a.getStatus())).count();
        long tenantsPlaced = myApplications.stream().filter(a -> "Accepted".equals(a.getStatus())).count();

        long occupancyRate = totalProperties == 0 ? 0 : Math.round((activeListings * 100.0) / totalProperties);

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
        model.addAttribute("activeListings", activeListings);
        model.addAttribute("vacantRooms", vacantRooms);
        model.addAttribute("occupancyRate", occupancyRate);
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("awaitingApprovalCount", awaitingApprovalCount); // NEW — bind this in landlord-index.html
        model.addAttribute("tenantsPlaced", tenantsPlaced);
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
        model.addAttribute("features", propertyFeatureRepository.findByPropertyID(id));
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
    @PostMapping("/update-property/{id}")
    public String updateProperty(
            @PathVariable Integer id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String rent,
            @RequestParam(required = false) String deposit,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String commuteType,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(value = "amenityIds", required = false) List<Integer> amenityIds,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "action", defaultValue = "update") String action,
            Principal principal) throws IOException {

        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);
        boolean isDraft = "draft".equals(action);

        // Only overwrite fields that were actually submitted (title/type/
        // address use "if not null" guards); rent/deposit parse from
        // Strings so a blank input doesn't crash BigDecimal parsing.
        if (title != null) property.setTitle(title);
        if (type != null) property.setType(type);
        if (address != null) property.setAddress(address);
        property.setCity(city);
        property.setRent((rent != null && !rent.isBlank()) ? new java.math.BigDecimal(rent) : property.getRent());
        property.setDeposit((deposit != null && !deposit.isBlank()) ? new java.math.BigDecimal(deposit) : null);
        property.setDescription(description);
        property.setCommuteType(commuteType);
        if (capacity != null) property.setCapacity(capacity);

        if (availableFrom != null && !availableFrom.isBlank()) {
            property.setAvailableFrom(LocalDate.parse(availableFrom));
        }

        // Amenity checkboxes are replaced wholesale each save — whatever
        // came through in the request IS the new full set.
        property.setAmenities(amenityIds != null
                ? amenityRepository.findAllById(amenityIds)
                : new ArrayList<>());

        if (isDraft) {
            property.setStatus("Draft");
        } else if ("Draft".equals(property.getStatus())) {
            // Finishing a draft via "Save Changes" submits it for Admin
            // approval — same as a brand-new listing going Pending.
            property.setStatus("Pending");
        }

        propertyRepository.save(property);

        // Append any newly uploaded photos after the ones already on file
        // (existingCount + 1 onward), rather than replacing them.
        if (images != null && images.length > 0) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            int existingCount = propertyImageRepository.findByPropertyID(id).size();
            int order = existingCount + 1;

            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String uniqueFileName = id + "_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(image.getInputStream(), filePath);

                    PropertyImage propImage = new PropertyImage();
                    propImage.setPropertyID(id);
                    propImage.setUrl("/uploads/" + uniqueFileName);
                    propImage.setCategory("exterior");
                    propImage.setIsMain(existingCount == 0 && order == existingCount + 1);
                    propImage.setDisplayOrder(order);
                    propertyImageRepository.save(propImage);
                    order++;
                }
            }
        }

        // Both branches return to the dashboard (not back to the edit
        // form), with a query flag so the dashboard's toast knows which
        // message to show.
        return isDraft
                ? "redirect:/landlord-index?draftSaved=true"
                : "redirect:/landlord-index?updated=true";
    }

    // Removes a single photo from a property.
    @PostMapping("/delete-property-image/{imageId}")
    public String deletePropertyImage(@PathVariable Integer imageId, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        Property property = getOwnedProperty(image.getPropertyID(), landlordID);

        propertyImageRepository.deleteById(imageId);
        return "redirect:/edit-property/" + property.getPropertyID();
    }

    // Special features (Study Hub, Braai Area, etc.) — add a new one with
    // its own photos.
    @PostMapping("/add-property-feature/{propertyId}")
    public String addPropertyFeature(
            @PathVariable Integer propertyId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            Principal principal) throws IOException {

        Integer landlordID = getCurrentUser(principal).getUserID();
        getOwnedProperty(propertyId, landlordID); // ownership check only — result unused

        PropertyFeature feature = new PropertyFeature();
        feature.setPropertyID(propertyId);
        feature.setName(name);
        feature.setDescription(description);
        PropertyFeature savedFeature = propertyFeatureRepository.save(feature);

        if (images != null && images.length > 0) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            int order = 1;
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String uniqueFileName = "feature_" + savedFeature.getFeatureID() + "_"
                            + System.currentTimeMillis() + "_" + image.getOriginalFilename();
                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(image.getInputStream(), filePath);

                    PropertyFeatureImage featureImage = new PropertyFeatureImage();
                    featureImage.setFeatureID(savedFeature.getFeatureID());
                    featureImage.setUrl("/uploads/" + uniqueFileName);
                    featureImage.setDisplayOrder(order);
                    propertyFeatureImageRepository.save(featureImage);
                    order++;
                }
            }
        }

        return "redirect:/edit-property/" + propertyId;
    }

    // Special features — remove one entirely (and its photos).
    @PostMapping("/delete-property-feature/{featureId}")
    public String deletePropertyFeature(@PathVariable Integer featureId, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        PropertyFeature feature = propertyFeatureRepository.findById(featureId)
                .orElseThrow(() -> new RuntimeException("Feature not found with id: " + featureId));
        Property property = getOwnedProperty(feature.getPropertyID(), landlordID);

        List<PropertyFeatureImage> featureImages = propertyFeatureImageRepository.findByFeatureID(featureId);
        propertyFeatureImageRepository.deleteAll(featureImages);
        propertyFeatureRepository.deleteById(featureId);

        return "redirect:/edit-property/" + property.getPropertyID();
    }

    // Deactivate or reactivate a listing. This is a SOFT delete — the row
    // is never removed from the database.
    //   isAvailable=false → status becomes "Inactive" regardless of what it
    //                        was before (Draft, Pending, or Approved). This
    //                        is what lets viewLandlordDashboard's filter
    //                        hide it no matter which state it was in.
    //   isAvailable=true  → if it was Inactive, restores status to
    //                        LIVE_STATUS ("Approved"), making it visible to
    //                        students again immediately (no re-approval).
    @PostMapping("/toggle-property-status/{id}")
    public String togglePropertyStatus(@PathVariable Integer id, @RequestParam Boolean isAvailable, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);

        if (Boolean.FALSE.equals(isAvailable)) {
            property.setStatus(DEACTIVATED_STATUS);
        } else if (DEACTIVATED_STATUS.equals(property.getStatus())) {
            property.setStatus(LIVE_STATUS);
        }
        property.setIsAvailable(isAvailable);
        propertyRepository.save(property);
        return "redirect:/landlord-index";
    }

    // Public property detail page — anyone can view (no ownership check),
    // since this is what a student clicks into from the dashboard.
    @GetMapping("/property/{id}")
    public String viewPropertyDetail(@PathVariable Integer id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        model.addAttribute("property", property);
        model.addAttribute("reviews", reviewRepository.findByPropertyID(id));
        model.addAttribute("vrImages", propertyImageRepository.findByPropertyIDAndIsVRTrue(id));
        return "property-detail";
    }

    // Public search — same LIVE_STATUS rule as the student dashboard, so
    // search never surfaces a Pending/Draft/Inactive property.
    @GetMapping("/search")
    public String searchProperties(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) java.math.BigDecimal maxRent,
            Model model) {

        if (maxRent != null) {
            model.addAttribute("properties",
                    propertyRepository.findByStatusAndRentLessThanEqual(LIVE_STATUS, maxRent));
        } else if (query != null && !query.isBlank()) {
            model.addAttribute("properties",
                    propertyRepository.searchApproved(query));
        } else {
            model.addAttribute("properties", propertyRepository.findByStatus(LIVE_STATUS));
        }
        return "properties";
    }

    // ============================================================
    // C102 — View Reviews (landlord side)
    // ============================================================

    // "Reviews" in the sidebar goes straight to the single-property view.
    // Picks the first Approved property (alphabetically); falls back to
    // any owned property if none are Approved yet, and only bounces to
    // the dashboard if the landlord owns literally zero properties.
    @GetMapping("/my-property-reviews")
    public String viewPropertyReviews(Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        List<Property> myProperties = propertyRepository.findByLandlordID(landlordID);
        if (myProperties.isEmpty()) {
            return "redirect:/landlord-index?noReviewsYet=true";
        }

        List<Property> approved = myProperties.stream()
                .filter(p -> "Approved".equals(p.getStatus()))
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());
        if (!approved.isEmpty()) {
            return "redirect:/property-reviews/" + approved.get(0).getPropertyID();
        }

        List<Property> any = myProperties.stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());
        return "redirect:/property-reviews/" + any.get(0).getPropertyID();
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

        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageNumbers.add(i);
        }

        // Property switcher: Approved properties with at least one Accepted
        // application — unchanged from before.
        List<Property> allMyProperties = propertyRepository.findByLandlordID(landlordID);
        List<Integer> allPropertyIds = allMyProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        Set<Integer> propertyIdsWithAcceptedStudents = applicationRepository.findByPropertyIDIn(allPropertyIds).stream()
                .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                .map(Application::getPropertyID)
                .collect(Collectors.toSet());

        List<Property> eligibleProperties = allMyProperties.stream()
                .filter(p -> "Approved".equals(p.getStatus()) && propertyIdsWithAcceptedStudents.contains(p.getPropertyID()))
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());

        Integer nextPropertyId = property.getPropertyID();
        if (!eligibleProperties.isEmpty()) {
            int currentIndex = -1;
            for (int i = 0; i < eligibleProperties.size(); i++) {
                if (eligibleProperties.get(i).getPropertyID().equals(id)) {
                    currentIndex = i;
                    break;
                }
            }
            nextPropertyId = (currentIndex == -1)
                    ? eligibleProperties.get(0).getPropertyID()
                    : eligibleProperties.get((currentIndex + 1) % eligibleProperties.size()).getPropertyID();
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

    // C102 — Landlord responds to (or edits their existing response on) a
    // review. Single response per review — write-and-overwrite, no
    // threading. Ownership is checked via the review's PROPERTY, so a
    // landlord can only respond to reviews left on their own listings.
    @PostMapping("/respond-to-review/{reviewId}")
    public String respondToReview(
            @PathVariable Integer reviewId,
            @RequestParam String response,
            Principal principal) {

        Integer landlordID = getCurrentUser(principal).getUserID();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        Property property = getOwnedProperty(review.getPropertyID(), landlordID);

        review.setLandlordResponse(response);
        review.setResponseDate(java.time.LocalDateTime.now());
        reviewRepository.save(review);

        return "redirect:/property-reviews/" + property.getPropertyID();
    }

    // ============================================================
    // C200 — View Applications (landlord side)
    // ============================================================

    // "Applications" in the sidebar goes straight to the single-property
    // view — same entry-point pattern as Reviews (C102): picks the first
    // owned property that actually has at least one application
    // (alphabetically), falls back to any owned property if none have
    // applications yet, and only bounces to the dashboard if the landlord
    // owns literally zero (non-deactivated) properties.
    @GetMapping("/manage-applications")
    public String manageApplications(Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        // FIX: was findByLandlordID with no status filter, so deactivated
        // (Inactive) AND unapproved (Draft/Pending) properties could still
        // be picked as the landing property here. Applications only make
        // sense against Approved properties, so this now uses the same
        // "live" rule as the switcher below.
        List<Property> myProperties = findLiveOwnedProperties(landlordID);
        if (myProperties.isEmpty()) {
            return "redirect:/landlord-index?noApplicationsYet=true";
        }

        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());
        Set<Integer> propertyIdsWithApplications = applicationRepository.findByPropertyIDIn(propertyIds).stream()
                .map(Application::getPropertyID)
                .collect(Collectors.toSet());

        List<Property> withApplications = myProperties.stream()
                .filter(p -> propertyIdsWithApplications.contains(p.getPropertyID()))
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());
        if (!withApplications.isEmpty()) {
            return "redirect:/property-applications/" + withApplications.get(0).getPropertyID();
        }

        List<Property> any = myProperties.stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());
        return "redirect:/property-applications/" + any.get(0).getPropertyID();
    }

    // C200 — View Applications detail (landlord side, one property). Mirrors
    // the property-reviews-detail pattern: a property switcher + next/back
    // arrows across every ACTIVE property this landlord owns, two summary
    // stat cards (Pending % / Accepted %) computed over THIS property's
    // applications only, and the same capacity-based Accept gating as
    // before.
    @GetMapping("/property-applications/{id}")
    public String viewPropertyApplicationsDetail(@PathVariable Integer id, Model model, Principal principal) {
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
            } else {
                row.setStudentName("Unknown student");
                row.setStudentInitials("?");
            }

            if (studentProfile != null) {
                row.setYearOfStudy(studentProfile.getYearOfStudy());
                row.setBudgetMin(studentProfile.getBudgetMin());
                row.setBudgetMax(studentProfile.getBudgetMax());
                row.setFundingStatus(studentProfile.getFundingStatus());
                row.setHousingPreferences(studentProfile.getHousingPreferences());
            }

            row.setPropertyName(property.getTitle());
            row.setPropertyAddress(buildApplicationAddress(property));
            row.setRoomType(buildApplicationRoomType(property));

            rows.add(row);
        }

        // Most recent first
        rows.sort((a, b) -> {
            if (a.getApplicationDate() == null) return 1;
            if (b.getApplicationDate() == null) return -1;
            return b.getApplicationDate().compareTo(a.getApplicationDate());
        });

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

        // Property switcher: every LIVE (Approved) property this landlord
        // owns — deactivated (Inactive) AND not-yet-approved (Draft/
        // Pending) properties are excluded, since applications only really
        // exist against properties students can actually see and apply to.
        // A landlord who already has one of those excluded properties'
        // page open (via a direct/older link) can still view it directly;
        // it just won't appear in this dropdown.
        List<Property> eligibleProperties = findLiveOwnedProperties(landlordID).stream()
                .sorted((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()))
                .collect(Collectors.toList());

        Integer nextPropertyId = property.getPropertyID();
        Integer previousPropertyId = property.getPropertyID();
        if (!eligibleProperties.isEmpty()) {
            int currentIndex = -1;
            for (int i = 0; i < eligibleProperties.size(); i++) {
                if (eligibleProperties.get(i).getPropertyID().equals(id)) {
                    currentIndex = i;
                    break;
                }
            }
            int size = eligibleProperties.size();
            nextPropertyId = (currentIndex == -1)
                    ? eligibleProperties.get(0).getPropertyID()
                    : eligibleProperties.get((currentIndex + 1) % size).getPropertyID();
            // NEW — mirrors nextPropertyId but walks the list backward
            // (with wraparound), for the back arrow to the left of the
            // "All Properties" switcher.
            previousPropertyId = (currentIndex == -1)
                    ? eligibleProperties.get(0).getPropertyID()
                    : eligibleProperties.get((currentIndex - 1 + size) % size).getPropertyID();
        }

        model.addAttribute("property", property);
        model.addAttribute("applications", rows);
        model.addAttribute("applicationCount", totalCount);
        model.addAttribute("pendingPercent", pendingPercent);
        model.addAttribute("acceptedPercent", acceptedPercent);
        model.addAttribute("acceptedCount", acceptedCount);
        model.addAttribute("capacity", capacity);
        model.addAttribute("eligibleProperties", eligibleProperties);
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

        if ("Accepted".equalsIgnoreCase(status)) {
            long acceptedCount = applicationRepository.findByPropertyIDIn(List.of(property.getPropertyID())).stream()
                    .filter(a -> "Accepted".equalsIgnoreCase(a.getStatus()))
                    .count();
            int capacity = property.getCapacity() != null ? property.getCapacity() : 0;
            if (acceptedCount >= capacity) {
                // Refuse the accept; the applications page shows a toast
                // for this exact query flag.
                return "redirect:/property-applications/" + property.getPropertyID() + "?capacityFull=true";
            }
        }

        application.setStatus(status);
        applicationRepository.save(application);
        return "redirect:/property-applications/" + property.getPropertyID();
    }

    // "Accept up to capacity" for ONE property: accepts the oldest Pending
    // applications first (first come, first served) until capacity is
    // reached, then rejects everyone else who applied to that same
    // property. One-click way to fill a listing.
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
                .sorted((a, b) -> {
                    if (a.getApplicationDate() == null) return 1;
                    if (b.getApplicationDate() == null) return -1;
                    return a.getApplicationDate().compareTo(b.getApplicationDate()); // oldest first
                })
                .collect(Collectors.toList());

        for (int i = 0; i < pending.size(); i++) {
            Application app = pending.get(i);
            app.setStatus(i < remaining ? "Accepted" : "Rejected");
            applicationRepository.save(app);
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

    // Builds "Suburb, City" (falling back to the raw address if neither is
    // set) for the applications table's Property & Room column.
    private String buildApplicationAddress(Property property) {
        StringBuilder sb = new StringBuilder();
        if (property.getSuburb() != null && !property.getSuburb().isBlank()) {
            sb.append(property.getSuburb());
        }
        if (property.getCity() != null && !property.getCity().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(property.getCity());
        }
        if (sb.length() == 0 && property.getAddress() != null) {
            sb.append(property.getAddress());
        }
        return sb.toString();
    }

    // Accept All — across EVERY property this landlord owns, accepts the
    // oldest Pending applications first, up to each property's own
    // remaining capacity. Properties already full, or with no capacity set
    // at all, are skipped entirely. This is the whole-portfolio version of
    // acceptUpToCapacity above (which only does one property at a time).
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
                    .sorted((a, b) -> {
                        if (a.getApplicationDate() == null) return 1;
                        if (b.getApplicationDate() == null) return -1;
                        return a.getApplicationDate().compareTo(b.getApplicationDate());
                    })
                    .collect(Collectors.toList());

            for (int i = 0; i < pending.size() && i < remaining; i++) {
                Application app = pending.get(i);
                app.setStatus("Accepted");
                applicationRepository.save(app);
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
                }
            }
        }

        return "redirect:/manage-applications";
    }

    // ============================================================
    // Student-side endpoints
    // ============================================================

    // A105 — Apply to Residence
    @PostMapping("/apply/{propertyId}")
    public String applyToProperty(@PathVariable Integer propertyId, Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();

        // One application per student per property — if they've already
        // applied (in any status, including Rejected/Accepted), don't let a
        // second one through. The my-applications page shows a toast for
        // this exact query flag.
        if (applicationRepository.existsByStudentIDAndPropertyID(studentID, propertyId)) {
            return "redirect:/my-applications?alreadyApplied=true";
        }

        Application application = new Application();
        application.setStudentID(studentID);
        application.setPropertyID(propertyId);
        application.setStatus("Pending");
        application.setApplicationDate(java.time.LocalDateTime.now());
        applicationRepository.save(application);
        return "redirect:/my-applications";
    }

    // A200 — View Applications (student's own)
    @GetMapping("/my-applications")
    public String viewApplications(Model model, Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();
        model.addAttribute("applications", applicationRepository.findByStudentID(studentID));
        return "my-applications";
    }

    // A202 — Cancel Application
    @PostMapping("/cancel-application/{applicationId}")
    public String cancelApplication(@PathVariable Integer applicationId) {
        applicationRepository.deleteById(applicationId);
        return "redirect:/my-applications";
    }

    @PostMapping("/favorite/{propertyId}")
    public String addFavorite(@PathVariable Integer propertyId, Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();

        SavedProperty saved = new SavedProperty();
        saved.setStudentID(studentID);
        saved.setPropertyID(propertyId);
        saved.setSavedStatus(true);
        savedPropertyRepository.save(saved);
        return "redirect:/student-dashboard";
    }

    // A500 — Write Review and Rating
    @PostMapping("/review/{propertyId}")
    public String submitReview(@PathVariable Integer propertyId, @RequestParam Integer rating,
                               @RequestParam String comment, Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();

        Review review = new Review();
        review.setStudentID(studentID);
        review.setPropertyID(propertyId);
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewDate(java.time.LocalDateTime.now());
        reviewRepository.save(review);
        return "redirect:/property/" + propertyId;
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

    // ============================================================
    // C100 — List a New Property (creation wizard)
    // ============================================================

    // Shows the blank wizard, passing amenities (grouped by category) and
    // the semester dropdown options for the JS to render.
    @GetMapping({"/listProperty", "/list-property"})
    public String listPropertyForm(Model model) {
        List<Amenity> allAmenities = amenityRepository.findAllByOrderByCategoryAscNameAsc();
        Map<String, List<Amenity>> amenityCategories = allAmenities.stream()
                .collect(Collectors.groupingBy(Amenity::getCategory, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("amenityCategories", amenityCategories);
        model.addAttribute("availableFromOptions", generateSemesterOptions());
        return "landlord/listProperty";
    }

    // Creates the property (also handles "Save as Draft"). New listings
    // always start with isAvailable=false — a property only becomes
    // available once an Admin approves it AND the landlord marks it
    // available (see togglePropertyStatus).
    @PostMapping("/listProperty")
    public String listProperty(
            @RequestParam String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) java.math.BigDecimal rent,
            @RequestParam(required = false) java.math.BigDecimal deposit,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String commuteType,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(value = "amenityIds", required = false) List<Integer> amenityIds,
            @RequestParam(value = "featureNames", required = false) List<String> featureNames,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "action", defaultValue = "submit") String action,
            Principal principal) throws IOException {

        Integer landlordID = getCurrentUser(principal).getUserID();
        boolean isDraft = "draft".equals(action);

        Property property = new Property();
        property.setLandlordID(landlordID);
        property.setTitle(title);
        property.setType(type);
        property.setAddress(address);
        property.setCity(city);
        property.setRent(rent);
        property.setDeposit(deposit);
        property.setDescription(description);
        property.setCommuteType(commuteType);
        // Defaults to 1 if the wizard didn't send a capacity (shouldn't
        // happen anymore now that Step 0 always includes the field, but
        // this keeps old/partial submissions from crashing).
        property.setCapacity(capacity != null ? capacity : 1);
        property.setIsAvailable(false);
        property.setStatus(isDraft ? "Draft" : "Pending");

        if (availableFrom != null && !availableFrom.isBlank()) {
            property.setAvailableFrom(java.time.LocalDate.parse(availableFrom));
        }

        property.setAmenities(amenityIds != null
                ? amenityRepository.findAllById(amenityIds)
                : new ArrayList<>());

        Property savedProperty = propertyRepository.save(property);

        // Special-feature tags entered in the wizard. Created without
        // photos here — the landlord attaches photos to each one
        // afterwards from the Manage Property page.
        if (featureNames != null) {
            for (String name : featureNames) {
                if (name != null && !name.isBlank()) {
                    PropertyFeature feature = new PropertyFeature();
                    feature.setPropertyID(savedProperty.getPropertyID());
                    feature.setName(name.trim());
                    propertyFeatureRepository.save(feature);
                }
            }
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        int order = 1;

        // Cover image is always marked isMain=true and saved first.
        if (coverImage != null && !coverImage.isEmpty()) {
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

        // Remaining gallery photos, in upload order.
        if (images != null) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String uniqueFileName = savedProperty.getPropertyID() + "_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(image.getInputStream(), filePath);

                    PropertyImage propImage = new PropertyImage();
                    propImage.setPropertyID(savedProperty.getPropertyID());
                    propImage.setUrl("/uploads/" + uniqueFileName);
                    propImage.setCategory("exterior");
                    propImage.setIsMain(false);
                    propImage.setDisplayOrder(order);
                    propertyImageRepository.save(propImage);
                    order++;
                }
            }
        }

        return isDraft
                ? "redirect:/landlord-index?draftSaved=true"
                : "redirect:/landlord-index?added=true";
    }
}