package com.ulee.ulee_backend.config;

import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.AdminSessionRepository;
import com.ulee.ulee_backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Ends the session of anyone who is already logged in but whose account has
 * been deactivated (the scheduled job flips User.isActive to false).
 *
 * Without this, deactivation only blocks the NEXT login; a landlord who is
 * already signed in could carry on until their session expired.
 *
 * NOTE: deliberately NOT a @Component. A Filter bean would also be registered
 * by Spring Boot outside the security chain; SecurityConfig adds it inside the
 * chain, right after the session's login is loaded.
 */
public class DeactivatedAccountFilter extends OncePerRequestFilter {

    public static final String REDIRECT_URL = "/student-dashboard?loginError=deactivated";
    public static final String MESSAGE = "Your account has been deactivated. Please contact support.";

    private final UserRepository userRepository;
    private final AdminSessionRepository adminSessionRepository;

    public DeactivatedAccountFilter(UserRepository userRepository, AdminSessionRepository adminSessionRepository) {
        this.userRepository = userRepository;
        this.adminSessionRepository = adminSessionRepository;
    }

    /** No database lookup for static files. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().toLowerCase();
        return path.startsWith("/images/") || path.startsWith("/uploads/") || path.startsWith("/static/")
                || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png")
                || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".svg")
                || path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        if (loggedIn) {
            Optional<User> userOpt = userRepository.findByEmail(auth.getName());
            if (userOpt.isPresent() && Boolean.FALSE.equals(userOpt.get().getIsActive())) {

                // Mark the "Active Sessions" row inactive before the session is destroyed.
                if (request.getSession(false) != null) {
                    String sessionId = request.getSession(false).getId();
                    adminSessionRepository.findBySessionId(sessionId).ifPresent(s -> {
                        s.setActive(false);
                        adminSessionRepository.save(s);
                    });
                }

                // Invalidates the HttpSession and clears the security context.
                new SecurityContextLogoutHandler().logout(request, response, auth);

                if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\":\"" + MESSAGE + "\",\"redirect\":\"" + REDIRECT_URL + "\"}");
                } else {
                    response.sendRedirect(REDIRECT_URL);
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }
}