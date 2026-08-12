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
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ulee.ulee_backend.repository.UserRepository;
import com.ulee.ulee_backend.repository.StudentRepository;
import com.ulee.ulee_backend.repository.LandlordRepository;


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

    @GetMapping("/admin/listings")
    public String viewAllListings(Model model) {
        List<Property> allProperties = propertyRepository.findAll();
        model.addAttribute("properties", allProperties);
        model.addAttribute("totalListings", allProperties.size());
        return "admin/admin-listings";
    }

    @GetMapping("/admin/pending-listings")
    public String viewPendingListings(Model model) {
        List<Property> pendingProperties = propertyRepository.findByStatus("Pending");
        model.addAttribute("pendingProperties", pendingProperties);
        return "admin/admin-pending-listings";
    }

    @GetMapping("/admin/approved-properties")
    public String viewApprovedProperties(Model model) {
        List<Property> approvedProperties = propertyRepository.findByStatus("Approved");
        model.addAttribute("approvedProperties", approvedProperties);
        model.addAttribute("totalApproved", approvedProperties.size());
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
    public String viewReviews(Model model) {
        List<Review> allReviews = reviewRepository.findAll();
        List<Property> allProperties = propertyRepository.findAll();

        Map<Integer, Property> propertyLookup = allProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyID, p -> p));

        model.addAttribute("reviews", allReviews);
        model.addAttribute("propertyLookup", propertyLookup);
        model.addAttribute("totalReviews", allReviews.size());
        return "admin/admin-reviews";
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

        model.addAttribute("property", property);
        model.addAttribute("landlord", landlordUserOpt.orElse(null));
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
        int current = landlordUser.getWarningCount() != null ? landlordUser.getWarningCount() : 0;
        landlordUser.setWarningCount(current + 1);
        userRepository.save(landlordUser);
        redirectAttributes.addFlashAttribute("actionMessage", "Warning sent to " + safeName(landlordUser) + " (warning #" + (current + 1) + ")");
        return "redirect:/admin/reported-listing/" + propertyId;
    }

    @PostMapping("/admin/remove-listing/{id}")
    public String removeListing(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Optional<Property> propertyOpt = propertyRepository.findById(id);
        if (propertyOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("actionError", "Listing #" + id + " could not be found.");
            return "redirect:/admin/reported-listings";
        }
        String title = propertyOpt.get().getTitle();
        propertyRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("actionMessage", "Removed listing \"" + title + "\"");
        return "redirect:/admin/reported-listings";
    }

    @GetMapping("/admin-index")
    public String viewAdminDashboard(Model model) {
        List<Property> allProperties = propertyRepository.findAll();
        List<Review> allReviews = reviewRepository.findAll();
        List<Property> pendingProperties = propertyRepository.findByStatus("Pending");
        List<Property> reportedProperties = propertyRepository.findByIsReportedTrue();

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
        model.addAttribute("pendingProperties", pendingProperties);
        model.addAttribute("reportedProperties", reportedProperties);
        model.addAttribute("landlordLookup", landlordLookup);
        model.addAttribute("totalListings", allProperties.size());
        model.addAttribute("totalReviews", allReviews.size());
        model.addAttribute("totalPending", pendingProperties.size());
        model.addAttribute("totalReported", reportedProperties.size());

        return "admin/admin-index";
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
        return "admin/admin-listing-details";
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