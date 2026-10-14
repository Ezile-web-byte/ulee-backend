package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    private final UserRepository userRepository;

    public CurrentUserAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("userInitials")
    public String userInitials(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return null;

        return userRepository.findById(userId)
                .map(u -> initialsOf(u.getFirstName(), u.getLastName()))
                .orElse(null);
    }

    private String initialsOf(String first, String last) {
        String f = (first == null || first.isBlank()) ? "" : first.trim().substring(0, 1);
        String l = (last == null || last.isBlank()) ? "" : last.trim().substring(0, 1);
        String result = (f + l).toUpperCase();
        return result.isEmpty() ? null : result;
    }
}