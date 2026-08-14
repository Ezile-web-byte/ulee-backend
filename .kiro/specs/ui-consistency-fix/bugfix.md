# Bugfix Requirements Document

## Introduction

The application's UI is visually inconsistent across student, landlord, and admin pages because three team members built their respective sections independently using different design systems. Student pages use `Plus Jakarta Sans` + `Playfair Display` fonts with a teal color scheme (`hsl(180,67%,47%)`), pill-shaped buttons, and soft-shadow cards. Landlord pages use `Plus Jakarta Sans` + `Hanken Grotesk` with a different teal (`#147592`) plus royal purple palette and rectangular buttons. Admin pages use `Sora` with the correct teal but rectangular buttons. The login/register page uses `DM Sans` + `DM Serif Display` with a dark background theme. Additionally, the login page references a broken background image path (`image2.jpeg` instead of `login-image2.jpeg`).

The goal is to unify all pages under one consistent, modern, premium design system based on the student page style: Sora + Playfair Display fonts, teal color scheme (`--primary: hsl(180,67%,47%)`), pill-shaped buttons, card layouts with soft shadows — and fix broken image references.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a user navigates to landlord pages (dashboard, manage properties, applications) THEN the system renders text in `Plus Jakarta Sans` / `Hanken Grotesk` fonts instead of the design system's `Sora` / `Playfair Display` fonts

1.2 WHEN a user navigates to landlord pages THEN the system uses a different primary color (`#147592` deep teal) and a secondary purple (`#441587`) palette instead of the unified teal `hsl(180,67%,47%)` color scheme

1.3 WHEN a user navigates to landlord pages THEN the system renders buttons with rectangular corners (`border-radius: 8px`) instead of pill-shaped buttons (`border-radius: 50px`)

1.4 WHEN a user navigates to landlord pages THEN the system renders cards with `border-radius: 24px` and shadows using `rgba(20, 117, 146, 0.08)` instead of the standard `border-radius: 12px` and soft teal-tinted shadows

1.5 WHEN a user navigates to the login or register page THEN the system renders text in `DM Sans` / `DM Serif Display` fonts instead of `Sora` / `Playfair Display`

1.6 WHEN a user navigates to the login or register page THEN the system uses a dark background theme (`--bg: #121212`) instead of the light background (`hsl(0,0%,99%)`) consistent with the rest of the app

1.7 WHEN a user navigates to the login or register page THEN the system attempts to load a background image from `url("image2.jpeg")` which is a broken reference (file does not exist at that path), resulting in no background image displayed

1.8 WHEN a user navigates to the landlord applications page (application.css) THEN the system renders text in `DM Sans` font instead of the design system's `Sora` font

1.9 WHEN a user navigates to the update/edit property page THEN the system renders text in `DM Sans` / `Fraunces` fonts instead of `Sora` / `Playfair Display`

1.10 WHEN a user navigates to the landlord reviews page THEN the system renders text in `Hanken Grotesk` / `Plus Jakarta Sans` instead of `Sora` / `Playfair Display`

1.11 WHEN a user navigates to the admin dashboard THEN buttons use rectangular corners (`border-radius: 7px`) instead of pill-shaped buttons

### Expected Behavior (Correct)

2.1 WHEN a user navigates to landlord pages THEN the system SHALL render text using `Sora` as the body font and `Playfair Display` as the display/heading font

2.2 WHEN a user navigates to landlord pages THEN the system SHALL use the unified teal color scheme with `--primary: hsl(180,67%,47%)`, `--primary-dark: hsl(180,67%,36%)`, and `--primary-deeper: hsl(180,67%,28%)` without a purple secondary palette

2.3 WHEN a user navigates to landlord pages THEN the system SHALL render all action buttons with pill-shaped corners (`border-radius: 50px`) consistent with the student pages

2.4 WHEN a user navigates to landlord pages THEN the system SHALL render cards with `border-radius: 12px` and soft shadows consistent with the student page card style

2.5 WHEN a user navigates to the login or register page THEN the system SHALL render text using `Sora` as the body font and `Playfair Display` for display text (logo)

2.6 WHEN a user navigates to the login or register page THEN the system SHALL use a light background theme consistent with the rest of the application, or a tasteful dark overlay on a working background image — not a flat dark `#121212` background

2.7 WHEN a user navigates to the login or register page THEN the system SHALL load the background image from the correct path `url("login-image2.jpeg")` so the blurred background is visible

2.8 WHEN a user navigates to the landlord applications page THEN the system SHALL render text using `Sora` as the body font consistent with the design system

2.9 WHEN a user navigates to the update/edit property page THEN the system SHALL render text using `Sora` as the body font and `Playfair Display` for headings

2.10 WHEN a user navigates to the landlord reviews page THEN the system SHALL render text using `Sora` as the body font and `Playfair Display` for group titles

2.11 WHEN a user navigates to the admin dashboard THEN buttons SHALL use pill-shaped corners (`border-radius: 50px`) consistent with the unified design system

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a user navigates to student pages (property listing, property detail, saved properties) THEN the system SHALL CONTINUE TO use `Sora` + `Playfair Display` fonts, teal color scheme `hsl(180,67%,47%)`, pill-shaped buttons, and soft-shadow card layouts without any visual changes

3.2 WHEN a user interacts with any page's JavaScript functionality (form submissions, filtering, modal dialogs, sidebar collapse, tab switching) THEN the system SHALL CONTINUE TO function identically since no JavaScript or backend logic is modified

3.3 WHEN a user views the admin dashboard stat cards, tables, and panels THEN the system SHALL CONTINUE TO display the same layout structure and data — only colors, fonts, border-radius, and shadows are updated

3.4 WHEN a user views property cards on landlord pages THEN the system SHALL CONTINUE TO display the same card layout structure (image, name, address, amenities, footer) with only visual styling updated

3.5 WHEN a user accesses the login page on a mobile viewport THEN the system SHALL CONTINUE TO display the login card in a centered, responsive layout

3.6 WHEN a user navigates between pages THEN the system SHALL CONTINUE TO reach the same controller routes and see the same data — no backend endpoints or Thymeleaf template logic is changed
