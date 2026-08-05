package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import com.ulee.ulee_backend.repository.PropertyRepository;
import com.ulee.ulee_backend.repository.SavedPropertyRepository;
import com.ulee.ulee_backend.repository.ApplicationRepository;
import com.ulee.ulee_backend.repository.ReviewRepository;
import com.ulee.ulee_backend.repository.ApplicationDocumentRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ulee.ulee_backend.repository.PropertyImageRepository;

@Controller
public class PropertyController {
    @Autowired
    private PropertyImageRepository propertyImageRepository;
    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

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

    // C100 support — Landlord Dashboard (now with real stats, not hardcoded)
    @GetMapping("/landlord-index")
    public String viewLandlordDashboard(Model model) {
        List<Property> myProperties = propertyRepository.findByLandlordID(1); // hardcoded test landlord for now
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        long totalProperties = myProperties.size();
        long activeListings = myProperties.stream().filter(Property::getIsAvailable).count();
        long vacantRooms = totalProperties - activeListings; // adjust if "vacant" should mean something else once you add a rooms/units field

        List<Application> myApplications = applicationRepository.findByPropertyIDIn(propertyIds);
        long pendingApplications = myApplications.stream().filter(a -> "Pending".equals(a.getStatus())).count();
        long tenantsPlaced = myApplications.stream().filter(a -> "Accepted".equals(a.getStatus())).count();

        long occupancyRate = totalProperties == 0 ? 0 : Math.round((activeListings * 100.0) / totalProperties);

        // one thumbnail per property, keyed by propertyID — same pattern as manageProperties()
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

    // C101 — Manage Property: list landlord's own properties, with portfolio-level stats for the dashboard bar
    @GetMapping("/manage-properties")
    public String manageProperties(Model model) {
        List<Property> myProperties = propertyRepository.findByLandlordID(1); // hardcoded test landlord for now
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        long totalProperties = myProperties.size();
        long availableCount = myProperties.stream().filter(Property::getIsAvailable).count();
        long inactiveCount = totalProperties - availableCount;

        List<Application> myApplications = applicationRepository.findByPropertyIDIn(propertyIds);
        long pendingApplications = myApplications.stream().filter(a -> "Pending".equals(a.getStatus())).count();

        // one thumbnail per property, keyed by propertyID, so the template avoids a query per card
        Map<Integer, String> imageLookup = propertyImageRepository.findByPropertyIDIn(propertyIds).stream()
                .collect(Collectors.toMap(
                        PropertyImage::getPropertyID,
                        PropertyImage::getUrl,
                        (existing, replacement) -> existing // keep the first image found per property
                ));

        model.addAttribute("properties", myProperties);
        model.addAttribute("imageLookup", imageLookup);
        model.addAttribute("totalProperties", totalProperties);
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("pendingApplications", pendingApplications);
        return "manage-properties";
    }

    // C101 — show the edit form pre-filled with existing data
    @GetMapping("/edit-property/{id}")
    public String editPropertyForm(@PathVariable Integer id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        model.addAttribute("property", property);
        return "edit-property";
    }

    // C101 — save the edited fields
    @PostMapping("/update-property/{id}")
    public String updateProperty(
            @PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam String address,
            @RequestParam(required = false) String city,
            @RequestParam Integer bedrooms,
            @RequestParam Integer bathrooms,
            @RequestParam java.math.BigDecimal rent,
            @RequestParam(required = false) java.math.BigDecimal deposit,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean furnished) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));

        property.setTitle(title);
        property.setType(type);
        property.setAddress(address);
        property.setCity(city);
        property.setBedrooms(bedrooms);
        property.setBathrooms(bathrooms);
        property.setRent(rent);
        property.setDeposit(deposit);
        property.setDescription(description);
        property.setFurnished(furnished != null && furnished);

        if (availableFrom != null && !availableFrom.isBlank()) {
            property.setAvailableFrom(java.time.LocalDate.parse(availableFrom));
        }

        propertyRepository.save(property);
        return "redirect:/manage-properties";
    }

    // C101 — deactivate or reactivate a listing (no hard delete, avoids FK issues)
    @PostMapping("/toggle-property-status/{id}")
    public String togglePropertyStatus(@PathVariable Integer id, @RequestParam Boolean isAvailable) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        property.setIsAvailable(isAvailable);
        propertyRepository.save(property);
        return "redirect:/manage-properties";
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
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) java.math.BigDecimal maxRent,
            Model model) {

        if (minBedrooms != null || maxRent != null) {
            model.addAttribute("properties",
                    propertyRepository.findByIsAvailableTrueAndBedroomsGreaterThanEqualAndRentLessThanEqual(
                            minBedrooms != null ? minBedrooms : 0,
                            maxRent != null ? maxRent : new java.math.BigDecimal("999999")));
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
    public String viewPropertyReviews(Model model) {
        List<Property> myProperties = propertyRepository.findByLandlordID(1); // hardcoded test landlord for now
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        List<Review> allReviews = reviewRepository.findByPropertyIDIn(propertyIds);

        // group reviews by which property they belong to, so the template can show each property's own reviews together
        Map<Integer, List<Review>> reviewsByProperty = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getPropertyID));

        model.addAttribute("properties", myProperties);
        model.addAttribute("reviewsByProperty", reviewsByProperty);
        return "my-property-reviews";
    }

    // C200 — View Applications (landlord side, across all their properties)
    @GetMapping("/manage-applications")
    public String manageApplications(Model model) {
        List<Property> myProperties = propertyRepository.findByLandlordID(1); // hardcoded test landlord for now
        List<Integer> propertyIds = myProperties.stream()
                .map(Property::getPropertyID)
                .collect(Collectors.toList());

        List<Application> applications = applicationRepository.findByPropertyIDIn(propertyIds);

        // build a lookup so the template can show each application's property title without a second query per row
        Map<Integer, Property> propertyLookup = myProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

        List<ApplicationRowView> rows = new ArrayList<>();
        for (Application app : applications) {
            User student = userRepository.findById(app.getStudentID()).orElse(null);
            Property property = propertyLookup.get(app.getPropertyID());

            ApplicationRowView row = new ApplicationRowView();
            row.setApplicationID(app.getApplicationID());
            row.setStatus(app.getStatus()); // "Pending" / "Accepted" / "Rejected" — matches existing DB convention
            row.setApplicationDate(app.getApplicationDate());

            if (student != null) {
                row.setStudentName(student.getFirstName() + " " + student.getLastName());
                row.setStudentInitials(
                        ("" + student.getFirstName().charAt(0) + student.getLastName().charAt(0)).toUpperCase());
            } else {
                row.setStudentName("Unknown student");
                row.setStudentInitials("?");
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

            rows.add(row);
        }

        long pendingCount = rows.stream().filter(r -> "Pending".equalsIgnoreCase(r.getStatus())).count();
        long acceptedTodayCount = rows.stream()
                .filter(r -> "Accepted".equalsIgnoreCase(r.getStatus())
                        && r.getApplicationDate() != null
                        && r.getApplicationDate().toLocalDate().isEqual(java.time.LocalDate.now()))
                .count();

        model.addAttribute("applications", rows);
        model.addAttribute("propertyLookup", propertyLookup); // kept in case other views still rely on it
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("acceptedTodayCount", acceptedTodayCount);
        model.addAttribute("avgResponseTime", "—"); // needs a decision-timestamp column to compute for real
        return "landlord/manage-applications"; // nested under templates/landlord/, per folder reorg
    }

    // Builds a short room-type line like "2-Bed Apartment · Furnished"
    private String buildApplicationRoomType(Property property) {
        StringBuilder sb = new StringBuilder();
        if (property.getBedrooms() != null) {
            sb.append(property.getBedrooms()).append("-Bed ");
        }
        sb.append(property.getType() != null && !property.getType().isBlank() ? property.getType() : "Room");
        if (Boolean.TRUE.equals(property.getFurnished())) {
            sb.append(" · Furnished");
        }
        return sb.toString();
    }

    // Builds a short address line like "Suburb, City"
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

    // A105 — Apply to Residence
    @PostMapping("/apply/{propertyId}")
    public String applyToProperty(@PathVariable Integer propertyId) {
        Application application = new Application();
        application.setStudentID(2); // hardcoded test student for now
        application.setPropertyID(propertyId);
        application.setStatus("Pending");
        application.setApplicationDate(java.time.LocalDateTime.now());
        applicationRepository.save(application);
        return "redirect:/my-applications";
    }

    // A200 — View Applications
    @GetMapping("/my-applications")
    public String viewApplications(Model model) {
        model.addAttribute("applications", applicationRepository.findByStudentID(2)); // hardcoded test student
        return "my-applications";
    }

    // A202 — Cancel Application
    @PostMapping("/cancel-application/{applicationId}")
    public String cancelApplication(@PathVariable Integer applicationId) {
        applicationRepository.deleteById(applicationId);
        return "redirect:/my-applications";
    }

    @PostMapping("/favorite/{propertyId}")
    public String addFavorite(@PathVariable Integer propertyId) {
        SavedProperty saved = new SavedProperty();
        saved.setStudentID(2); // hardcoded test student for now
        saved.setPropertyID(propertyId);
        saved.setSavedStatus(true);
        savedPropertyRepository.save(saved);
        return "redirect:/student-dashboard";
    }
    // A500 — Write Review and Rating
    @PostMapping("/review/{propertyId}")
    public String submitReview(@PathVariable Integer propertyId, @RequestParam Integer rating, @RequestParam String comment) {
        Review review = new Review();
        review.setStudentID(2); // hardcoded test student for now
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

        // Create the uploads folder if it doesn't exist yet
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Build a unique filename so two students' files with the same name don't overwrite each other
        String uniqueFileName = applicationId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Save the actual file bytes to disk
        Files.copy(file.getInputStream(), filePath);

        // Save a record of it in the database
        ApplicationDocument doc = new ApplicationDocument();
        doc.setApplicationID(applicationId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(filePath.toString());
        doc.setUploadedAt(java.time.LocalDateTime.now());
        applicationDocumentRepository.save(doc);

        return "redirect:/my-applications";
    }
    // C100 — List Property
    @PostMapping("/list-property")
    public String listProperty(
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam String address,
            @RequestParam(required = false) String city,
            @RequestParam Integer bedrooms,
            @RequestParam Integer bathrooms,
            @RequestParam java.math.BigDecimal rent,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean furnished,
            @RequestParam(value = "images", required = false) MultipartFile[] images) throws IOException {

        Property property = new Property();
        property.setLandlordID(1); // hardcoded test landlord for now
        property.setTitle(title);
        property.setType(type);
        property.setAddress(address);
        property.setCity(city);
        property.setBedrooms(bedrooms);
        property.setBathrooms(bathrooms);
        property.setRent(rent);
        property.setDescription(description);
        property.setFurnished(furnished != null && furnished);
        property.setIsAvailable(true);

        if (availableFrom != null && !availableFrom.isBlank()) {
            property.setAvailableFrom(java.time.LocalDate.parse(availableFrom));
        }

        Property savedProperty = propertyRepository.save(property);

        // Save uploaded images, if any
        if (images != null) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            int order = 1;
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String uniqueFileName = savedProperty.getPropertyID() + "_" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
                    Path filePath = uploadPath.resolve(uniqueFileName);
                    Files.copy(image.getInputStream(), filePath);

                    PropertyImage propImage = new PropertyImage();
                    propImage.setPropertyID(savedProperty.getPropertyID());
                    propImage.setUrl("/" + uniqueFileName);
                    propImage.setCategory("exterior");
                    propImage.setIsMain(order == 1);
                    propImage.setDisplayOrder(order);
                    propertyImageRepository.save(propImage);
                    order++;
                }
            }
        }

        return "redirect:/landlord-index";
    }

}