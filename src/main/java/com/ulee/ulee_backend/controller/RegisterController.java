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
import org.springframework.security.crypto.password.PasswordEncoder;
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

@Controller
public class RegisterController {

    private static final String STUDENT_ROLE = "student";
    private static final String LANDLORD_ROLE = "landlord";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
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
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer yearOfStudy,
            @RequestParam(required = false) String companyName,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String normalizedRole = role.trim().toLowerCase(Locale.ROOT);
        if (!STUDENT_ROLE.equals(normalizedRole) && !LANDLORD_ROLE.equals(normalizedRole)) {
            model.addAttribute("error", "Please choose either Student or Landlord.");
            model.addAttribute("role", STUDENT_ROLE);
            return "register";
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            model.addAttribute("error", "That email is already registered. Try logging in instead.");
            model.addAttribute("role", normalizedRole);
            return "register";
        }

        LocalDate parsedDateOfBirth = null;
        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            try {
                parsedDateOfBirth = LocalDate.parse(dateOfBirth);
            } catch (DateTimeParseException exception) {
                model.addAttribute("error", "Please enter a valid date of birth.");
                model.addAttribute("role", normalizedRole);
                return "register";
            }
        }

        // Step 1 — create the shared User row first.
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setRole(normalizedRole.toUpperCase(Locale.ROOT));
        user.setDateOfBirth(parsedDateOfBirth);
        user.setIsActive(true);
        user.setWarningCount(0);

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
