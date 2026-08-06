package com.ulee.ulee_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

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
                .authorizeHttpRequests(auth -> auth
                        // Public: homepage, browsing, registration, static assets
                        .requestMatchers(
                                "/", "/student-dashboard", "/property/**", "/search",
                                "/register", "/login", "/login-style.css", "/login-script.js",
                                "/student-style.css", "/student-script.js",
                                "/static/**", "/uploads/**"
                        ).permitAll()
                        .requestMatchers("/landlord-index", "/list-property",
                                "/edit-property/**", "/update-property/**", "/delete-property-image/**",
                                "/add-property-feature/**", "/delete-property-feature/**",
                                "/toggle-property-status/**", "/manage-applications", "/my-property-reviews")
                        .hasRole("LANDLORD")
                        .requestMatchers("/admin-dashboard").hasRole("ADMIN")
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