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

    // Small helper so every method resolves the logged-in user the same way
    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    // Small helper so ownership checks read the same way everywhere they're needed
    private Property getOwnedProperty(Integer propertyId, Integer landlordID) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + propertyId));
        if (!property.getLandlordID().equals(landlordID)) {
            throw new RuntimeException("You do not have permission to manage this property");
        }
        return property;
    }

    // Generates the semester-start options for the "Available From" dropdown:
    // Jan and July of each year, spanning one year back (so an already-set past
    // date still shows correctly) through three years ahead.
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

    // Builds a display-ready view of a Review, resolving the student's
    // name/year the same way manageApplications() resolves applicant names.
    // Used by both viewPropertyReviews() (Reviews page, all properties) and
    // editPropertyForm() (Manage Property page, single property) so the two
    // pages render identical review cards.
    private ReviewRowView toReviewRowView(Review review) {
        User user = userRepository.findById(review.getStudentID()).orElse(null);
        Student studentProfile = studentRepository.findById(review.getStudentID()).orElse(null);

        ReviewRowView view = new ReviewRowView();
        view.setReviewID(review.getReviewID());
        view.setPropertyID(review.getPropertyID());
        view.setRating(review.getRating());
        view.setComment(review.getComment());
        view.setReviewDate(review.getReviewDate());

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

    @GetMapping("/student-dashboard")
    public String viewStudentDashboard(Model model) {
        model.addAttribute("properties", propertyRepository.findByIsAvailableTrue());
        return "student/student-dashboard";
    }

    // C100 support — Landlord Dashboard (also serves as the "My Properties" inventory page)
    @GetMapping("/landlord-index")
    public String viewLandlordDashboard(Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        // Deactivated (soft-deleted) properties stay in the database but are
        // filtered out here so they never appear on the dashboard, in the
        // stat cards, or in the filter tabs — regardless of what status they
        // were in before being deactivated (Draft, Pending, or Available).
        // togglePropertyStatus is what sets status to "Inactive".
        List<Property> myProperties = propertyRepository.findByLandlordID(landlordID).stream()
                .filter(p -> !"Inactive".equals(p.getStatus()))
                .collect(Collectors.toList());

        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        long totalProperties = myProperties.size();
        long activeListings = myProperties.stream().filter(Property::getIsAvailable).count();
        long vacantRooms = totalProperties - activeListings;

        List<Application> myApplications = applicationRepository.findByPropertyIDIn(propertyIds);
        long pendingApplications = myApplications.stream().filter(a -> "Pending".equals(a.getStatus())).count();
        long tenantsPlaced = myApplications.stream().filter(a -> "Accepted".equals(a.getStatus())).count();

        long occupancyRate = totalProperties == 0 ? 0 : Math.round((activeListings * 100.0) / totalProperties);

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
        model.addAttribute("tenantsPlaced", tenantsPlaced);
        return "landlord/landlord-index";
    }

    // C101 — show the edit/"Manage" form pre-filled with existing data
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

        // Reviews for this one property, so the same comments shown on the
        // Reviews page also appear here on Manage Property.
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

    // C101 — save the edited fields, amenity selections, and optionally append new photos.
    // Also doubles as the "Save Draft" handler: when action=draft, required-field
    // validation is skipped client-side (formnovalidate) and whatever was filled
    // in gets saved with status="Draft" so the landlord can finish it later.
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

        if (title != null) property.setTitle(title);
        if (type != null) property.setType(type);
        if (address != null) property.setAddress(address);
        property.setCity(city);
        property.setRent((rent != null && !rent.isBlank()) ? new java.math.BigDecimal(rent) : property.getRent());
        property.setDeposit((deposit != null && !deposit.isBlank()) ? new java.math.BigDecimal(deposit) : null);
        property.setDescription(description);
        property.setCommuteType(commuteType);
        if (capacity != null) property.setCapacity(capacity);
        // Note: bedrooms/bathrooms/furnished are no longer edited from this form.
        // They stay untouched on the existing record (furnished is deprecated in
        // favour of the "Furnished" amenity checkbox below).

        if (availableFrom != null && !availableFrom.isBlank()) {
            property.setAvailableFrom(LocalDate.parse(availableFrom));
        }

        property.setAmenities(amenityIds != null
                ? amenityRepository.findAllById(amenityIds)
                : new ArrayList<>());

        if (isDraft) {
            property.setStatus("Draft");
        } else if ("Draft".equals(property.getStatus())) {
            // Finishing up a draft via "Save Changes" submits it for approval,
            // mirroring the existing /submit-property/{id} flow.
            property.setStatus("Pending");
        }

        propertyRepository.save(property);

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

        // Both branches now return to the dashboard rather than back to the
        // edit form, per the landlord workflow: drafts go back with a
        // "draftSaved" flag, real saves go back with an "updated" flag so the
        // dashboard can show the right confirmation toast.
        return isDraft
                ? "redirect:/landlord-index?draftSaved=true"
                : "redirect:/landlord-index?updated=true";
    }

    // C101 — remove a single photo from a property
    @PostMapping("/delete-property-image/{imageId}")
    public String deletePropertyImage(@PathVariable Integer imageId, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        Property property = getOwnedProperty(image.getPropertyID(), landlordID);

        propertyImageRepository.deleteById(imageId);
        return "redirect:/edit-property/" + property.getPropertyID();
    }

    // Special features (Study Hub, Braai Area, etc.) — add a new one with its own photos
    @PostMapping("/add-property-feature/{propertyId}")
    public String addPropertyFeature(
            @PathVariable Integer propertyId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            Principal principal) throws IOException {

        Integer landlordID = getCurrentUser(principal).getUserID();
        getOwnedProperty(propertyId, landlordID); // ownership check only

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

    // Special features — remove one entirely (and its photos)
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

    // C101 — deactivate or reactivate a listing.
    // This is a soft delete: the row itself is never removed from the
    // database. Deactivating sets status to "Inactive" regardless of what
    // the property's prior status was (Draft, Pending, or already-approved
    // and Available) — that's what lets the dashboard query in
    // viewLandlordDashboard hide it no matter which state it was deactivated
    // from. Reactivating restores it to "Approved" so it falls back into the
    // normal Available/Inactive badge logic used for non-Draft/Pending
    // listings.
    @PostMapping("/toggle-property-status/{id}")
    public String togglePropertyStatus(@PathVariable Integer id, @RequestParam Boolean isAvailable, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);

        if (Boolean.FALSE.equals(isAvailable)) {
            property.setStatus("Inactive");
        } else if ("Inactive".equals(property.getStatus())) {
            property.setStatus("Approved");
        }
        property.setIsAvailable(isAvailable);
        propertyRepository.save(property);
        return "redirect:/landlord-index";
    }

    @GetMapping("/property/{id}")
    public String viewPropertyDetail(@PathVariable Integer id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        model.addAttribute("property", property);
        model.addAttribute("reviews", reviewRepository.findByPropertyID(id));
        model.addAttribute("vrImages", propertyImageRepository.findByPropertyIDAndIsVRTrue(id));
        return "property-detail";
    }

    @GetMapping("/search")
    public String searchProperties(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) java.math.BigDecimal maxRent,
            Model model) {

        if (maxRent != null) {
            model.addAttribute("properties",
                    propertyRepository.findByIsAvailableTrueAndRentLessThanEqual(maxRent));
        } else if (query != null && !query.isBlank()) {
            model.addAttribute("properties",
                    propertyRepository.findByIsAvailableTrueAndTitleContainingIgnoreCaseOrIsAvailableTrueAndCityContainingIgnoreCase(query, query));
        } else {
            model.addAttribute("properties", propertyRepository.findByIsAvailableTrue());
        }
        return "properties";
    }

    // C102 — View Reviews (landlord side, across all their properties)
    @GetMapping("/my-property-reviews")
    public String viewPropertyReviews(Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        List<Property> myProperties = propertyRepository.findByLandlordID(landlordID);
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        List<Review> allReviews = reviewRepository.findByPropertyIDIn(propertyIds);

        Map<Integer, List<ReviewRowView>> reviewsByProperty = allReviews.stream()
                .map(this::toReviewRowView)
                .collect(Collectors.groupingBy(ReviewRowView::getPropertyID));

        model.addAttribute("properties", myProperties);
        model.addAttribute("reviewsByProperty", reviewsByProperty);
        return "landlord/my-property-reviews";
    }

    // C200 — View Applications (landlord side, across all their properties)
    @GetMapping("/manage-applications")
    public String manageApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();

        List<Property> myProperties = propertyRepository.findByLandlordID(landlordID);
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        List<Application> applications = applicationRepository.findByPropertyIDIn(propertyIds);

        Map<Integer, Property> propertyLookup = myProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

        List<ApplicationRowView> allRows = new ArrayList<>();
        for (Application app : applications) {
            User student = userRepository.findById(app.getStudentID()).orElse(null);
            Student studentProfile = studentRepository.findById(app.getStudentID()).orElse(null);
            Property property = propertyLookup.get(app.getPropertyID());

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

            if (property != null) {
                row.setPropertyName(property.getTitle());
                row.setPropertyAddress(buildApplicationAddress(property));
                row.setRoomType(buildApplicationRoomType(property));
            } else {
                row.setPropertyName("Unknown property");
                row.setPropertyAddress("");
                row.setRoomType("");
            }

            allRows.add(row);
        }

        // Most recent applications first — matches the "Recent Student Applications" panel title
        allRows.sort((a, b) -> {
            if (a.getApplicationDate() == null) return 1;
            if (b.getApplicationDate() == null) return -1;
            return b.getApplicationDate().compareTo(a.getApplicationDate());
        });

        long pendingCount = allRows.stream().filter(r -> "Pending".equalsIgnoreCase(r.getStatus())).count();
        long acceptedTodayCount = allRows.stream()
                .filter(r -> "Accepted".equalsIgnoreCase(r.getStatus())
                        && r.getApplicationDate() != null
                        && r.getApplicationDate().toLocalDate().isEqual(java.time.LocalDate.now()))
                .count();

        // Pagination — sliced in memory since applications are already loaded as a List
        int totalItems = allRows.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) size);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        int fromIndex = currentPage * size;
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<ApplicationRowView> pageRows = totalItems == 0
                ? new ArrayList<>()
                : allRows.subList(fromIndex, toIndex);

        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pageNumbers.add(i);
        }

        model.addAttribute("applications", pageRows);
        model.addAttribute("propertyLookup", propertyLookup);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("acceptedTodayCount", acceptedTodayCount);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("fromIndex", totalItems == 0 ? 0 : fromIndex + 1);
        model.addAttribute("toIndex", toIndex);
        return "landlord/manage-applications";
    }

    private String buildApplicationRoomType(Property property) {
        StringBuilder sb = new StringBuilder();
        sb.append(property.getType() != null && !property.getType().isBlank() ? property.getType() : "Room");
        return sb.toString();
    }

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

    // C201 — Accept/Reject Status
    @PostMapping("/update-application-status/{applicationId}")
    public String updateApplicationStatus(@PathVariable Integer applicationId, @RequestParam String status) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));
        application.setStatus(status);
        applicationRepository.save(application);
        return "redirect:/manage-applications";
    }

    // Accept All — across every property this landlord owns, accept the
    // oldest Pending applications first, up to that property's own
    // remaining capacity (capacity - already-Accepted count). Properties
    // that are already full are simply skipped.
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

    // Reject All — for every property this landlord owns that is already
    // at full capacity, reject its remaining Pending applications. Properties
    // that still have space are left untouched, so this never rejects an
    // application that could still be accepted.
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
            if (remaining > 0) continue; // still has space — don't reject, leave for Accept All / manual review

            for (Application app : propApps) {
                if ("Pending".equalsIgnoreCase(app.getStatus())) {
                    app.setStatus("Rejected");
                    applicationRepository.save(app);
                }
            }
        }

        return "redirect:/manage-applications";
    }

    // A105 — Apply to Residence
    @PostMapping("/apply/{propertyId}")
    public String applyToProperty(@PathVariable Integer propertyId, Principal principal) {
        Integer studentID = getCurrentUser(principal).getUserID();

        Application application = new Application();
        application.setStudentID(studentID);
        application.setPropertyID(propertyId);
        application.setStatus("Pending");
        application.setApplicationDate(java.time.LocalDateTime.now());
        applicationRepository.save(application);
        return "redirect:/my-applications";
    }

    // A200 — View Applications
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

    // C100 — show the blank "List a New Property" form
    @GetMapping("/list-property")
    public String listPropertyForm(Model model) {
        List<Amenity> allAmenities = amenityRepository.findAllByOrderByCategoryAscNameAsc();
        Map<String, List<Amenity>> amenityCategories = allAmenities.stream()
                .collect(Collectors.groupingBy(Amenity::getCategory, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("amenityCategories", amenityCategories);
        model.addAttribute("availableFromOptions", generateSemesterOptions());
        return "landlord/listProperty";
    }

    // C100 — List Property (also handles "Save as Draft")
    @PostMapping("/list-property")
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

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        int order = 1;

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

    // Drafts — resume editing, then submit for real once ready
    @PostMapping("/submit-property/{id}")
    public String submitProperty(@PathVariable Integer id, Principal principal) {
        Integer landlordID = getCurrentUser(principal).getUserID();
        Property property = getOwnedProperty(id, landlordID);

        property.setStatus("Pending");
        propertyRepository.save(property);

        return "redirect:/landlord-index?added=true";
    }

}