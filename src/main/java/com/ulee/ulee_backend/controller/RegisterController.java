package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.Landlord;
import com.ulee.ulee_backend.model.Student;
import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.UserRepository;
import com.ulee.ulee_backend.repository.StudentRepository;
import com.ulee.ulee_backend.repository.LandlordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private LandlordRepository landlordRepository;


    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    // C300 — Register
    @PostMapping("/register")
    public String register(
            @RequestParam String role, // "student" or "landlord"
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String phone,
            // student-only fields
            @RequestParam(required = false) Integer yearOfStudy,

            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            // landlord-only fields
            @RequestParam(required = false) String companyName,
            Model model) {

        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "That email is already registered. Try logging in instead.");
            model.addAttribute("role", role);
            return "register";
        }

        // Step 1 — create the shared User row first
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        //user.setPassword(password); // plain text for now, matching the rest of this project's current approach
        user.setPhone(phone);
        user.setRole(role.toUpperCase());


        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            user.setDateOfBirth(LocalDate.parse(dateOfBirth));
        }

        User savedUser = userRepository.save(user); // this generates the real userID

        // Step 2 — create the matching subclass row, reusing the SAME id
        if ("landlord".equalsIgnoreCase(role)) {
            Landlord landlord = new Landlord();
            landlord.setLandlordID(savedUser.getUserID());
            landlord.setCompanyName(companyName);
            landlord.setPropertiesCount(0);
            landlordRepository.save(landlord);
            return "redirect:/landlord-index";
        } else {
            Student student = new Student();
            student.setStudentID(savedUser.getUserID());
            student.setYearOfStudy(yearOfStudy);
            student.setBudgetMin(budgetMin);
            student.setBudgetMax(budgetMax);
            studentRepository.save(student);
            return "redirect:/student-dashboard";
        }
    }
}