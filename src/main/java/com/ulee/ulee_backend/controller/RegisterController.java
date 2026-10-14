package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.Landlord;
import com.ulee.ulee_backend.model.Student;
import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.LandlordRepository;
import com.ulee.ulee_backend.repository.StudentRepository;
import com.ulee.ulee_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

@Controller
public class RegisterController {

    private static final String STUDENT_ROLE = "student";
    private static final String LANDLORD_ROLE = "landlord";

    // Registration rules (mirrored in register.html so the browser catches them first;
    // these are the real checks, because the browser can be bypassed).
    // Student email: "s" + exactly 9 digits + @mandela.ac.za
    private static final Pattern STUDENT_EMAIL = Pattern.compile("^s\\d{9}@mandela\\.ac\\.za$");
    private static final Pattern ANY_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    // Phone: exactly 10 digits, numbers only
    private static final Pattern PHONE = Pattern.compile("^\\d{10}$");
    // Date of birth: the year must have exactly 4 digits (yyyy-MM-dd)
    private static final Pattern DOB_FORMAT = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final int MIN_YEAR_OF_STUDY = 1;
    private static final int MAX_YEAR_OF_STUDY = 6;
    private static final int MIN_BIRTH_YEAR = 1900;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private LandlordRepository landlordRepository;
    @Autowired
    private UserDetailsService userDetailsService;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    // Puts an error on the register page (keeping the chosen role) and returns the view name.
    private String reject(Model model, String role, String message) {
        model.addAttribute("error", message);
        model.addAttribute("role", role);
        return "register";
    }

    // C300 — Register. The transaction keeps the users row and its matching
    // student/landlord row atomic: either both are saved or neither is.
    @Transactional
    @PostMapping("/register")
    public String register(
            @RequestParam String role,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer yearOfStudy,
            @RequestParam(required = false) String companyName,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String normalizedRole = role.trim().toLowerCase(Locale.ROOT);
        if (!STUDENT_ROLE.equals(normalizedRole) && !LANDLORD_ROLE.equals(normalizedRole)) {
            return reject(model, STUDENT_ROLE, "Please choose either Student or Landlord.");
        }

        // ── Email ──
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (STUDENT_ROLE.equals(normalizedRole)) {
            if (!STUDENT_EMAIL.matcher(normalizedEmail).matches()) {
                return reject(model, normalizedRole,
                        "Student emails must be an \"s\" followed by 9 digits and @mandela.ac.za (e.g. s227676637@mandela.ac.za).");
            }
        } else if (!ANY_EMAIL.matcher(normalizedEmail).matches()) {
            return reject(model, normalizedRole, "Please enter a valid email address.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            return reject(model, normalizedRole, "That email is already registered. Try logging in instead.");
        }

        // ── Phone (optional, but exactly 10 digits when given) ──
        String cleanedPhone = (phone == null || phone.isBlank()) ? null : phone.trim();
        if (cleanedPhone != null && !PHONE.matcher(cleanedPhone).matches()) {
            return reject(model, normalizedRole, "Phone number must be exactly 10 digits, numbers only.");
        }

        // ── Year of study (students only): 1 to 6 ──
        if (STUDENT_ROLE.equals(normalizedRole) && yearOfStudy != null
                && (yearOfStudy < MIN_YEAR_OF_STUDY || yearOfStudy > MAX_YEAR_OF_STUDY)) {
            return reject(model, normalizedRole, "Year of study must be between 1 and 6.");
        }

        // ── Date of birth (optional): 4-digit year, 1900 or later, not in the future ──
        LocalDate parsedDateOfBirth = null;
        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            String dob = dateOfBirth.trim();
            if (!DOB_FORMAT.matcher(dob).matches()) {
                return reject(model, normalizedRole, "The year in your date of birth must have exactly 4 digits.");
            }
            try {
                parsedDateOfBirth = LocalDate.parse(dob);
            } catch (DateTimeParseException exception) {
                return reject(model, normalizedRole, "Please enter a valid date of birth.");
            }
            if (parsedDateOfBirth.getYear() < MIN_BIRTH_YEAR || parsedDateOfBirth.isAfter(LocalDate.now())) {
                return reject(model, normalizedRole, "Please enter a valid date of birth.");
            }
        }

        String passwordError = com.ulee.ulee_backend.util.PasswordRules.check(password);
        if (passwordError != null) {
            return reject(model, normalizedRole, passwordError);
        }

        // Gender: required for every new account, "Female" or "Male".
        String cleanedGender = null;
        if (gender != null && gender.trim().equalsIgnoreCase("Female")) cleanedGender = "Female";
        else if (gender != null && gender.trim().equalsIgnoreCase("Male")) cleanedGender = "Male";
        if (cleanedGender == null) {
            return reject(model, normalizedRole, "Please choose your gender.");
        }

        // Step 1 — create the shared User row first.
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        // Stored as plain text on purpose (project requirement: the lecturers
        // read passwords directly from the database). No hashing here.
        user.setPassword(password);
        user.setPhone(cleanedPhone);
        user.setRole(normalizedRole.toUpperCase(Locale.ROOT));
        user.setDateOfBirth(parsedDateOfBirth);
        user.setIsActive(true);
        user.setWarningCount(0);
        user.setGender(cleanedGender);

        User savedUser = userRepository.save(user);

        // Step 2 — create the matching subtype row using the same ID.
        String redirectUrl;
        if (LANDLORD_ROLE.equals(normalizedRole)) {
            Landlord landlord = new Landlord();
            landlord.setLandlordID(savedUser.getUserID());
            landlord.setCompanyName(companyName == null || companyName.isBlank() ? null : companyName.trim());
            landlord.setPropertiesCount(0);
            landlordRepository.save(landlord);
            redirectUrl = "/landlord-index";
        } else {
            Student student = new Student();
            student.setStudentID(savedUser.getUserID());
            student.setYearOfStudy(yearOfStudy);
            studentRepository.save(student);
            redirectUrl = "/student-dashboard";
        }

        // Step 3 — authenticate the new account before redirecting. Loading
        // through UserDetailsService guarantees the same principal and roles
        // used by normal Spring Security login.
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);

        return "redirect:" + redirectUrl;
    }
}