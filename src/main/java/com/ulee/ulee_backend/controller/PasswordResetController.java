package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.UserRepository;
import com.ulee.ulee_backend.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Forgot password (administrator-assisted): the system creates a one-time link,
 * the administrator sends it to the user, and the link opens a page to choose
 * a new password.
 */
@Controller
public class PasswordResetController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    public record ResetRow(String email, String link, String expires, String mailto) { }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordResetService resetService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // The same confirmation is shown whether or not the email exists.
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<User> userOpt = normalized.isEmpty() ? Optional.empty() : userRepository.findByEmail(normalized);
        userOpt.ifPresent(user -> resetService.createToken(normalized));
        model.addAttribute("email", normalized);
        return "auth/forgot-sent";
    }

    // Open requests, shown on the admin Profile page (Reset requests tab).
    @GetMapping("/admin/password-resets/data")
    @ResponseBody
    public List<ResetRow> adminResetRequests() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH)
                .withZone(ZoneId.systemDefault());
        return resetService.activeRequests().stream().map(r -> {
            String link = baseUrl + "/reset-password?token=" + r.token();
            String until = fmt.format(r.expires());
            String body = "Hi,\n\nWe received a request to reset your ULEE password.\n"
                    + "Open this link to choose a new one (it works until " + until + "):\n\n" + link
                    + "\n\nIf you did not ask for this, you can ignore this email.";
            String mailto = "mailto:" + r.email() + "?subject=" + enc("Reset your ULEE password")
                    + "&body=" + enc(body);
            return new ResetRow(r.email(), link, until, mailto);
        }).toList();
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        boolean valid = resetService.emailFor(token).isPresent();
        return page(model, token, !valid, false, null);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model) {
        Optional<String> emailOpt = resetService.emailFor(token);
        if (emailOpt.isEmpty()) {
            return page(model, token, true, false, null);
        }
        String pwError = com.ulee.ulee_backend.util.PasswordRules.check(password);
        if (pwError != null) {
            return page(model, token, false, false,
                    pwError);
        }
        if (!password.equals(confirmPassword)) {
            return page(model, token, false, false, "The two passwords do not match.");
        }
        Optional<User> userOpt = userRepository.findByEmail(emailOpt.get());
        if (userOpt.isEmpty()) {
            return page(model, token, true, false, null);
        }
        User user = userOpt.get();
        // Stored exactly like RegisterController stores it (plain text, project requirement).
        user.setPassword(password);
        userRepository.save(user);
        resetService.invalidate(token);
        return page(model, token, false, true, null);
    }

    private String page(Model model, String token, boolean invalid, boolean done, String error) {
        model.addAttribute("token", token);
        model.addAttribute("invalid", invalid);
        model.addAttribute("done", done);
        model.addAttribute("error", error);
        return "auth/reset-password";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}