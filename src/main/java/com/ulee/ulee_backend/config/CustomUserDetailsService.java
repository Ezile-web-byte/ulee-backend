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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // must already be BCrypt-hashed
                .authorities(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .disabled(user.getIsActive() != null && !user.getIsActive())
                .build();
    }
}