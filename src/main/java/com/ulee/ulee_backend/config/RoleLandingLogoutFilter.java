package com.ulee.ulee_backend.config;

import com.ulee.ulee_backend.repository.AdminSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Admins and landlords have their own dashboards, but the public pages
 * ("/" and "/student-dashboard", the login and register pages) are built
 * for students and visitors. The browser shares ONE login session across all
 * tabs, so before this filter existed, an admin or landlord who opened the
 * website in a second tab was still "logged in" there and was treated like a
 * student.
 *
 * Now, when someone signed in as ADMIN or LANDLORD opens one of those public
 * pages, they are signed out first (session ended, Settings > Active Sessions
 * entry closed) and the same page is loaded again as a normal visitor.
 * Students are never touched.
 */
public class RoleLandingLogoutFilter extends OncePerRequestFilter {

    /** Public entry pages where an admin/landlord must not stay signed in. */
    private static final Set<String> PUBLIC_ENTRY_PATHS =
            Set.of("/", "/student-dashboard", "/login", "/register");

    private final AdminSessionRepository adminSessionRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public RoleLandingLogoutFilter(AdminSessionRepository adminSessionRepository) {
        this.adminSessionRepository = adminSessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Only page visits (GET). The login form POST to /login is left alone.
        if ("GET".equalsIgnoreCase(request.getMethod()) && isPublicEntryPage(request)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && hasStaffRole(auth)) {
                // Close the matching row in Settings > Active Sessions BEFORE the session is ended
                HttpSession session = request.getSession(false);
                if (session != null) {
                    adminSessionRepository.findBySessionId(session.getId()).ifPresent(s -> {
                        s.setActive(false);
                        adminSessionRepository.save(s);
                    });
                }

                // End the session and clear the login
                logoutHandler.logout(request, response, auth);

                // Load the same page again, now as a normal visitor
                String query = request.getQueryString();
                String target = request.getRequestURI() + (query != null ? "?" + query : "");
                response.sendRedirect(target);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicEntryPage(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if (path.isEmpty()) path = "/";
        return PUBLIC_ENTRY_PATHS.contains(path);
    }

    private boolean hasStaffRole(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_LANDLORD".equals(a.getAuthority()));
    }
}