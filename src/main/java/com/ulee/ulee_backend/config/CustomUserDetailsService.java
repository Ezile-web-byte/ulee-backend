package com.ulee.ulee_backend.config;

import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Login is by email only (Spring Security's "username" parameter carries the email
        // value). Normalize the same way registration stores it, so case/whitespace
        // differences at the login form don't cause a valid account to be rejected.
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + email));

        String role = user.getRole();
        if (role == null || role.isBlank()) {
            throw new UsernameNotFoundException("User " + email + " has no role assigned");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + role.trim().toUpperCase())
                ))
                .disabled(user.getIsActive() != null && !user.getIsActive())
                .build();
    }
}