package com.ulee.ulee_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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
                        .requestMatchers("/admin-dashboard", "/admin-index", "/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
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

    private AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            String redirectUrl = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(a -> switch (a.getAuthority()) {
                        case "ROLE_LANDLORD" -> "/landlord-index";
                        case "ROLE_ADMIN" -> "/admin-index";
                        default -> "/student-dashboard";
                    })
                    .orElse("/student-dashboard");
            response.sendRedirect(redirectUrl);
        };
    }
}