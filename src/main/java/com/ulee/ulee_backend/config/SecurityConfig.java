package com.ulee.ulee_backend.config;

import com.ulee.ulee_backend.model.AdminLoginActivity;
import com.ulee.ulee_backend.model.AdminSession;
import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.AdminLoginActivityRepository;
import com.ulee.ulee_backend.repository.AdminSessionRepository;
import com.ulee.ulee_backend.repository.UserRepository;
import com.ulee.ulee_backend.util.RequestDeviceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminSessionRepository adminSessionRepository;

    @Autowired
    private AdminLoginActivityRepository adminLoginActivityRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // TEMP: unblocks login without a CSRF token in the form
                .authorizeHttpRequests(auth -> auth
                        // Public: homepage, browsing, registration, ALL static assets
                        .requestMatchers(
                                "/", "/student-dashboard", "/property/**", "/search",
                                "/register", "/login",
                                "/images/**", "/*.css", "/*.js",
                                "/*.png", "/*.jpg", "/*.jpeg", "/*.svg", "/*.gif", "/*.webp",
                                "/login-style.css", "/login-script.js",
                                "/student-style.css", "/student-script.js",
                                "/static/**", "/uploads/**"
                        ).permitAll()

                        // Landlord Protected Routes (ADD `/listProperty` HERE)
                        .requestMatchers(
                                "/landlord-index",
                                "/listProperty",  // <-- BOTH MATCHED NOW
                                "/edit-property/**", "/update-property/**", "/delete-property-image/**",
                                "/add-property-feature/**", "/delete-property-feature/**", "/submit-property/**",
                                "/toggle-property-status/**", "/manage-applications", "/my-property-reviews"
                        ).hasRole("LANDLORD")

                        .requestMatchers("/admin-dashboard", "/admin-index", "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/login")     // where the login form POSTs to
                        // FIX: no .loginPage(...) was set, so whenever Spring
                        // Security needed a visitor to log in (e.g. hitting a
                        // protected page while logged out) it fell back to
                        // its own built-in default login page — the plain
                        // "Please sign in" form you're seeing, which has
                        // nothing to do with student-dashboard.html. Pointing
                        // it here instead sends people to the real ULEE page,
                        // which already knows to show the login banner and
                        // auto-open the styled login modal when
                        // loginRequired=true is present.
                        .loginPage("/student-dashboard?loginRequired=true")
                        .successHandler(roleBasedSuccessHandler())
                        .failureHandler(loginFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // Mark the AdminSession row for this HttpSession as
                        // inactive BEFORE the session itself is invalidated,
                        // so Settings > Active Sessions stops showing it.
                        .addLogoutHandler(sessionCleanupLogoutHandler())
                        // FIX: was "/", so logging out (from any role — admin or
                        // student) dropped you on the bare root path instead of
                        // ULEE's own landing/dashboard page. Now every logout,
                        // regardless of which role logged out, lands on
                        // /student-dashboard as requested.
                        .logoutSuccessUrl("/student-dashboard")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Redirects each role to its own dashboard after successful login, and
     * — new — records the login (Settings > Recent Login Activity) and
     * opens an AdminSession row for this device (Settings > Active
     * Sessions), tied to the HttpSession id so it can be found again on
     * logout/expiry.
     */
    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            Optional<User> userOpt = userRepository.findByEmail(authentication.getName());

            String deviceLabel = RequestDeviceUtils.deviceLabel(request);
            String ip = RequestDeviceUtils.clientIp(request);
            LocalDateTime now = LocalDateTime.now();

            AdminLoginActivity activity = new AdminLoginActivity();
            activity.setUserID(userOpt.map(User::getUserID).orElse(null));
            activity.setEmailAttempted(authentication.getName());
            activity.setSuccess(true);
            activity.setDeviceLabel(deviceLabel);
            activity.setIpAddress(ip);
            activity.setTimestamp(now);
            adminLoginActivityRepository.save(activity);

            if (userOpt.isPresent()) {
                AdminSession session = new AdminSession();
                session.setUserID(userOpt.get().getUserID());
                session.setSessionId(request.getSession().getId());
                session.setDeviceLabel(deviceLabel);
                session.setIpAddress(ip);
                session.setLoginAt(now);
                session.setLastActiveAt(now);
                session.setActive(true);
                adminSessionRepository.save(session);
            }

            String redirectUrl = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> switch (a.getAuthority()) {
                        case "ROLE_LANDLORD" -> "/landlord-index";
                        case "ROLE_ADMIN" -> "/admin-index";   // <-- changed from /admin-dashboard
                        default -> "/student-dashboard";
                    })
                    .orElse("/student-dashboard");
            response.sendRedirect(redirectUrl);
        };
    }

    /**
     * Records a failed login attempt (Settings > Recent Login Activity),
     * then falls back to the same failureUrl behaviour the app had before
     * ("/?loginError=true"). The attempted email is logged even if it
     * doesn't match any account, so failed attempts are still visible —
     * userID is just left null in that case.
     */
    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            String attemptedEmail = request.getParameter("username");

            AdminLoginActivity activity = new AdminLoginActivity();
            if (attemptedEmail != null) {
                userRepository.findByEmail(attemptedEmail).ifPresent(u -> activity.setUserID(u.getUserID()));
            }
            activity.setEmailAttempted(attemptedEmail);
            activity.setSuccess(false);
            activity.setDeviceLabel(RequestDeviceUtils.deviceLabel(request));
            activity.setIpAddress(RequestDeviceUtils.clientIp(request));
            activity.setTimestamp(LocalDateTime.now());
            adminLoginActivityRepository.save(activity);

            response.sendRedirect("/?loginError=true");
        };
    }

    /** Marks this HttpSession's AdminSession row inactive on explicit logout. */
    private LogoutHandler sessionCleanupLogoutHandler() {
        return (request, response, authentication) -> {
            if (request.getSession(false) == null) return;
            String sessionId = request.getSession(false).getId();
            adminSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                session.setActive(false);
                adminSessionRepository.save(session);
            });
        };
    }
}