package com.ulee.ulee_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String showLoginPage() {
        return "logIn";
    }



    @GetMapping("/admin-dashboard")
    public String showAdminDashboard() {
        return "admin-index";
    }

    @GetMapping("/landlord-dashboard")
    public String showLandlordDashboard() {
        return "landlord/landlord-index";
    }

    @GetMapping("/application")
    public String showApplicationPage() {
        return "application";
    }

    @GetMapping("/update-property")
    public String showUpdatePage() {
        return "landlord/update";
    }
}