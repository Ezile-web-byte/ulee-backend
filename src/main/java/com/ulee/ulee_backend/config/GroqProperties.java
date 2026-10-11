package com.ulee.ulee_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the Groq LLM API key/base URL from Spring configuration
 * (application.properties), which in turn resolves them from the
 * GROQ_API_KEY / GROQ_BASE_URL environment variables. The key is NEVER
 * hard-coded here or in application.properties — see the ${...:}
 * placeholder defaults there, which resolve to an empty string when the
 * env var is absent so the app can still start locally without it.
 *
 * GroqAiService (added in a later stage) is responsible for treating a
 * blank apiKey as "the AI provider is not configured" and failing fast
 * with a dedicated exception rather than sending a request with no key.
 */
@Configuration
@ConfigurationProperties(prefix = "groq.api")
public class GroqProperties {

    private String key;
    private String baseUrl;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
