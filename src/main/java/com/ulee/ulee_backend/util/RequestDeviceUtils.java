package com.ulee.ulee_backend.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns raw request info (User-Agent header, remote address) into the
 * human-readable strings shown on Settings > Security ("Chrome on Windows",
 * "192.168.1.4"). This is a light, dependency-free parser covering the
 * common browsers/OSes — it won't be perfect for every User-Agent string,
 * but it's good enough for an admin-facing device label and avoids pulling
 * in a full UA-parsing library for one small feature.
 */
public final class RequestDeviceUtils {

    private RequestDeviceUtils() {}

    public static String deviceLabel(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) return "Unknown device";

        String browser;
        if (ua.contains("Edg/")) browser = "Edge";
        else if (ua.contains("OPR/") || ua.contains("Opera")) browser = "Opera";
        else if (ua.contains("Chrome/")) browser = "Chrome";
        else if (ua.contains("Firefox/")) browser = "Firefox";
        else if (ua.contains("Safari/") && !ua.contains("Chrome")) browser = "Safari";
        else browser = "Browser";

        String os;
        if (ua.contains("Windows")) os = "Windows";
        else if (ua.contains("Mac OS X") || ua.contains("Macintosh")) os = "macOS";
        else if (ua.contains("Android")) os = "Android";
        else if (ua.contains("iPhone")) os = "iPhone";
        else if (ua.contains("iPad")) os = "iPad";
        else if (ua.contains("Linux")) os = "Linux";
        else os = "Unknown OS";

        return browser + " on " + os;
    }

    /**
     * Best-effort caller IP. Checks X-Forwarded-For first in case this app
     * ever sits behind a proxy/load balancer, otherwise falls back to the
     * direct connection address.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}