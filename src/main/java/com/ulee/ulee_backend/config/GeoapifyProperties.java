package com.ulee.ulee_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the Geoapify Places API key/base URL from Spring configuration
 * (application.properties), which in turn resolves them from the
 * GEOAPIFY_API_KEY / GEOAPIFY_BASE_URL environment variables. The key is
 * NEVER hard-coded here or in application.properties — see the ${...:}
 * placeholder defaults there, which resolve to an empty string when the
 * env var is absent so the app can still start locally without it.
 *
 * GeoapifyPlacesService (added in a later stage) is responsible for
 * treating a blank apiKey as "Places lookups are not configured" and
 * skipping the call rather than sending a request with no key.
 */
@Configuration
@ConfigurationProperties(prefix = "geoapify.api")
public class GeoapifyProperties {

    private String key;
    private String baseUrl;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
