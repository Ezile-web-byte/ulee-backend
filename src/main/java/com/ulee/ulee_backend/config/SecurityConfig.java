package com.ulee.ulee_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                        .successHandler(roleBasedSuccessHandler())
                        .failureUrl("/?loginError=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    // Redirects each role to its own dashboard after successful login
    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            String redirectUrl = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> switch (a.getAuthority()) {
                        case "ROLE_LANDLORD" -> "/landlord-index";
                        case "ROLE_ADMIN" -> "/admin-dashboard";
                        default -> "/student-dashboard";
                    })
                    .orElse("/student-dashboard");
            response.sendRedirect(redirectUrl);
        };
    }
}