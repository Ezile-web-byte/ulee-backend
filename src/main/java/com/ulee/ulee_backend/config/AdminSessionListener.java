package com.ulee.ulee_backend.config;

import com.ulee.ulee_backend.model.AdminSession;
import com.ulee.ulee_backend.repository.AdminSessionRepository;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Companion to the explicit-logout handler in SecurityConfig: that one
 * covers someone clicking "Log out", this one covers every other way a
 * session ends (timeout, browser data cleared, server restart cleanup)
 * so the Active Sessions list in Settings doesn't keep showing stale rows.
 */
@Component
public class AdminSessionListener implements HttpSessionListener {

    @Autowired
    private AdminSessionRepository adminSessionRepository;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        String sessionId = event.getSession().getId();
        adminSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setActive(false);
            adminSessionRepository.save(session);
        });
    }
}