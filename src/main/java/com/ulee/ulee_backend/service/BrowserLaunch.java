package com.ulee.ulee_backend.service;

import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
public class BrowserLaunch implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            System.out.println("Desktop supported: " + Desktop.isDesktopSupported());
            if (Desktop.isDesktopSupported()) {
                System.out.println("Browse supported: " + Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("http://localhost:8080/"));
                System.out.println("Browser launch attempted.");
            }
        } catch (Exception e) {
            System.out.println("Could not auto-open browser: " + e.getMessage());
        }
    }
}