package com.ulee.ulee_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces every /admin/** page to be non-cacheable by the browser.
 *
 * We hit a case where navigating to a page via a sidebar link showed a
 * stale, older version of the page while typing the same URL directly
 * showed the current one — the classic symptom of the browser serving a
 * cached response instead of re-fetching from the server. Rather than
 * add Cache-Control headers to every controller method one at a time,
 * this filter applies them to every /admin/** GET response in one place,
 * removing browser caching as a possible cause across the whole admin panel.
 */
@Configuration
public class NoCacheConfig {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> adminNoCacheFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
                filterChain.doFilter(request, response);
            }
        };

        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/admin/*", "/admin-index");
        registration.setName("adminNoCacheFilter");
        registration.setOrder(1);
        return registration;
    }
}