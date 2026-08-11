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
        // Normalize to a proper file:// URL regardless of OS path separators
        String normalized = uploadDir.replace("\\", "/");
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        String location = "file:///" + normalized;

        // Checks the external upload folder first (new landlord-uploaded photos),
        // then falls back to the bundled classpath location (original seed images
        // like /uploads/Dunes/main.png that ship inside src/main/resources/static/uploads)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location, "classpath:/static/uploads/");
    }
}