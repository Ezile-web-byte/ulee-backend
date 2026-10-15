package com.ulee.ulee_backend.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private static final long VALID_MINUTES = 24 * 60;

    private record Entry(String email, Instant expires) { }

    public record ResetRequest(String email, String token, Instant expires) { }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String createToken(String email) {
        purgeExpired();
        tokens.values().removeIf(e -> e.email().equalsIgnoreCase(email));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, new Entry(email, Instant.now().plusSeconds(VALID_MINUTES * 60)));
        return token;
    }

    public List<ResetRequest> activeRequests() {
        purgeExpired();
        return tokens.entrySet().stream()
                .map(e -> new ResetRequest(e.getValue().email(), e.getKey(), e.getValue().expires()))
                .sorted(Comparator.comparing(ResetRequest::expires).reversed())
                .toList();
    }

    public Optional<String> emailFor(String token) {
        if (token == null) return Optional.empty();
        Entry e = tokens.get(token);
        if (e == null) return Optional.empty();
        if (e.expires().isBefore(Instant.now())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(e.email());
    }

    public void invalidate(String token) {
        tokens.remove(token);
    }

    public long getValidMinutes() {
        return VALID_MINUTES;
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        tokens.values().removeIf(e -> e.expires().isBefore(now));
    }
}