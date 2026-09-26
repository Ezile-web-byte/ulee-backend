package com.ulee.ulee_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Build a proper file:// URL via File.toURI() instead of hand-assembling
        // the string. This matters because paths containing spaces (e.g. a
        // Windows username like "unakho gadavu") are NOT valid raw file: URLs —
        // "file:///C:/Users/unakho gadavu/..." silently fails to resolve and
        // every image 404s. File.toURI() percent-encodes the space (%20)
        // correctly and also normalizes \ vs / automatically.
        java.io.File dir = new java.io.File(uploadDir);
        String location = dir.toURI().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        // Checks the external upload folder first (new landlord-uploaded photos),
        // then falls back to the bundled classpath location (original seed images
        // like /uploads/Dunes/main.png that ship inside src/main/resources/static/uploads)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location, "classpath:/static/uploads/");
    }
}