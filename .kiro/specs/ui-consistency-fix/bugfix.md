# Bugfix Requirements Document

## Introduction

The application's UI is visually inconsistent across student, landlord, and admin pages because three team members built their respective sections independently using different design systems. Student pages use `Plus Jakarta Sans` + `Playfair Display` fonts with a teal color scheme (`hsl(180,67%,47%)`), pill-shaped buttons, and soft-shadow cards. Landlord pages use `Plus Jakarta Sans` + `Hanken Grotesk` with a different teal (`#147592`) plus royal purple palette and rectangular buttons. Admin pages use `Sora` with the correct teal but rectangular buttons. The login/register page uses `DM Sans` + `DM Serif Display` with a dark background theme. Additionally, the login page references a broken background image path (`image2.jpeg` instead of `login-image2.jpeg`).

**Revised direction — Hybrid design system.** Rather than unifying every non-student page under a single teal Sora/Playfair Display system, non-student pages now split into two tracks:

1. **Landing/Marketing pages** — `properties.html` (the root template served by `GET /search`), the login page (`logIn.html`), and `register.html`. These adopt the "bold hero" style: a full-width background photo with a dark overlay, bold white/yellow typography (`Plus Jakarta Sans` display / `Hanken Grotesk` body), pill-shaped glass-panel search/action bars, and yellow CTA buttons. This is not a new invention — it is the existing pattern already implemented in `student/student-dashboard.html`'s hero section (yellow accent `#ffe170` / `tertiary-fixed`, `bg-white/10 backdrop-blur-xl` glass search bar, dark gradient overlay on a full-bleed photo). That hero section itself remains untouched and out of scope (it is an isolated Tailwind theme); it now additionally serves as the canonical visual reference for this Landing/Marketing track.
2. **Dashboard/Functional pages** — landlord pages, admin pages, applications, update, reviews, and property-detail — continue using the teal design system exactly as already specified (`Sora` + `Playfair Display`, teal `hsl(180,67%,47%)`, pill 50px buttons, 12px cards). This track is unaffected by the revised direction.

**New finding.** Investigation of `properties.html` (the root template) revealed it is currently rendered with no styling whatsoever — no linked CSS, no classes, no font-family, plain default browser rendering. The original version of this document incorrectly assumed this page already matched a design system and froze it as an "unchanged" reference in the preservation scope. This has been corrected below: `properties.html` having no design system applied is now documented as a defect, and the page is moved into the Landing/Marketing track's expected-behavior scope. Two additional stylesheet path mismatches (`manage-applications.html` and landlord `my-property-reviews.html`), previously flagged only as risks to verify, are likewise promoted to formal defects here.

The goal is to: (a) bring every Dashboard/Functional page under the teal `Sora`/`Playfair Display` system, (b) bring every Landing/Marketing page under the bold-hero `Plus Jakarta Sans`/`Hanken Grotesk` + yellow-CTA system already referenced in `student/student-dashboard.html`, and (c) fix broken/mismatched asset and stylesheet references.

## Bug Analysis

### Current Behavior (Defect)

**Dashboard/Functional track**

1.1 WHEN a user navigates to landlord pages (dashboard, manage properties, applications) THEN the system renders text in `Plus Jakarta Sans` / `Hanken Grotesk` fonts instead of the design system's `Sora` / `Playfair Display` fonts

1.2 WHEN a user navigates to landlord pages THEN the system uses a different primary color (`#147592` deep teal) and a secondary purple (`#441587`) palette instead of the unified teal `hsl(180,67%,47%)` color scheme

1.3 WHEN a user navigates to landlord pages THEN the system renders buttons with rectangular corners (`border-radius: 8px`) instead of pill-shaped buttons (`border-radius: 50px`)

1.4 WHEN a user navigates to landlord pages THEN the system renders cards with `border-radius: 24px` and shadows using `rgba(20, 117, 146, 0.08)` instead of the standard `border-radius: 12px` and soft teal-tinted shadows

**Landing/Marketing track — login page**

1.5 WHEN a user navigates to the login page THEN the system renders text in `DM Sans` / `DM Serif Display` fonts instead of the Landing/Marketing Hero typography (`Plus Jakarta Sans` display / `Hanken Grotesk` body)

1.6 WHEN a user navigates to the login page THEN the system renders a flat, unstyled dark background fill (`--bg: #121212`) with no full-width background photo, instead of the intended full-width photo-with-dark-overlay hero treatment (and its buttons/panels use the wrong teal-oriented palette instead of the yellow-CTA glass-panel styling)

1.7 WHEN a user navigates to the login page THEN the system attempts to load a background image from `url("image2.jpeg")` which is a broken reference (file does not exist at that path), resulting in no background image displayed

**Dashboard/Functional track (continued)**

1.8 WHEN a user navigates to the landlord applications page (application.css) THEN the system renders text in `DM Sans` font instead of the design system's `Sora` font

1.9 WHEN a user navigates to the update/edit property page THEN the system renders text in `DM Sans` / `Fraunces` fonts instead of `Sora` / `Playfair Display`

1.10 WHEN a user navigates to the landlord reviews page THEN the system renders text in `Hanken Grotesk` / `Plus Jakarta Sans` instead of `Sora` / `Playfair Display`

1.11 WHEN a user navigates to the admin dashboard THEN buttons use rectangular corners (`border-radius: 7px`) instead of pill-shaped buttons

**Newly identified defects**

1.12 WHEN a user navigates to `properties.html` (root template, `GET /search`) THEN the system renders it completely unstyled — no linked stylesheet, no custom classes, no font-family override, plain default browser rendering — instead of any design system

1.13 WHEN a user navigates to `register.html` THEN the system renders it using the teal dashboard system (`Sora` / `Playfair Display` fonts, teal pill buttons) instead of the Landing/Marketing Hero style now required for landing pages

1.14 WHEN a user navigates to the landlord manage-applications page THEN the system links its stylesheet as `/applications-style.css` (lowercase) while the actual file on disk is `Applications-style.css`, a case mismatch that can 404 on case-sensitive servers

1.15 WHEN a user navigates to the landlord reviews page (`landlord/my-property-reviews.html`) THEN the system links its stylesheet as `/css/landlord-reviews.css` while the actual file is served from the static root at `/landlord-reviews.css`, causing the link to 404

### Expected Behavior (Correct)

**Dashboard/Functional track**

2.1 WHEN a user navigates to landlord pages THEN the system SHALL render text using `Sora` as the body font and `Playfair Display` as the display/heading font

2.2 WHEN a user navigates to landlord pages THEN the system SHALL use the unified teal color scheme with `--primary: hsl(180,67%,47%)`, `--primary-dark: hsl(180,67%,36%)`, and `--primary-deeper: hsl(180,67%,28%)` without a purple secondary palette

2.3 WHEN a user navigates to landlord pages THEN the system SHALL render all action buttons with pill-shaped corners (`border-radius: 50px`) consistent with the teal dashboard system

2.4 WHEN a user navigates to landlord pages THEN the system SHALL render cards with `border-radius: 12px` and soft shadows consistent with the teal dashboard card style

**Landing/Marketing track — login page**

2.5 WHEN a user navigates to the login page THEN the system SHALL render text using `Plus Jakarta Sans` as the display font and `Hanken Grotesk` as the body font, consistent with the Landing/Marketing Hero reference in `student/student-dashboard.html`

2.6 WHEN a user navigates to the login page THEN the system SHALL render a full-width background photo with a dark overlay, bold white/yellow typography, pill-shaped glass-panel input/action styling, and yellow CTA buttons (instead of a flat dark fill and teal-oriented buttons)

2.7 WHEN a user navigates to the login page THEN the system SHALL load the background image from the correct path `url("login-image2.jpeg")` so the full-bleed hero photo and its dark overlay are visible

**Dashboard/Functional track (continued)**

2.8 WHEN a user navigates to the landlord applications page THEN the system SHALL render text using `Sora` as the body font consistent with the design system

2.9 WHEN a user navigates to the update/edit property page THEN the system SHALL render text using `Sora` as the body font and `Playfair Display` for headings

2.10 WHEN a user navigates to the landlord reviews page THEN the system SHALL render text using `Sora` as the body font and `Playfair Display` for group titles

2.11 WHEN a user navigates to the admin dashboard THEN buttons SHALL use pill-shaped corners (`border-radius: 50px`) consistent with the teal dashboard system

**Newly identified expected behaviors**

2.12 WHEN a user navigates to `properties.html` THEN the system SHALL render it using the Landing/Marketing Hero design system — a full-width background photo with dark overlay, bold white/yellow typography, a pill-shaped glass-panel search bar, and a yellow CTA button — consistent with the reference pattern in `student/student-dashboard.html`

2.13 WHEN a user navigates to `register.html` THEN the system SHALL render it using the Landing/Marketing Hero design system (bold white/yellow typography, full-width photo with dark overlay where applicable, pill-shaped glass-panel styling, yellow CTA buttons) instead of the teal dashboard system

2.14 WHEN a user navigates to the landlord manage-applications page THEN the system SHALL reference its applications stylesheet using a path that resolves without a 404 (fix the case mismatch between `/applications-style.css` and `Applications-style.css`)

2.15 WHEN a user navigates to the landlord reviews page THEN the system SHALL reference `landlord-reviews.css` using its correct static-root path so it loads without a 404

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a user navigates to student detail pages (property detail, saved properties) THEN the system SHALL CONTINUE TO use `Sora` + `Playfair Display` fonts, teal color scheme `hsl(180,67%,47%)`, pill-shaped buttons, and soft-shadow card layouts without any visual changes. (Note: root `properties.html` is explicitly excluded from this preserved-visual set — see 1.12/2.12 — and `register.html` is explicitly excluded — see 1.13/2.13 — since both now intentionally adopt the Landing/Marketing Hero style instead.)

3.2 WHEN a user interacts with any page's JavaScript functionality (form submissions, filtering, modal dialogs, sidebar collapse, tab switching) THEN the system SHALL CONTINUE TO function identically since no JavaScript or backend logic is modified

3.3 WHEN a user views the admin dashboard stat cards, tables, and panels THEN the system SHALL CONTINUE TO display the same layout structure and data — only colors, fonts, border-radius, and shadows are updated

3.4 WHEN a user views property cards on landlord pages THEN the system SHALL CONTINUE TO display the same card layout structure (image, name, address, amenities, footer) with only visual styling updated

3.5 WHEN a user accesses the login page on a mobile viewport THEN the system SHALL CONTINUE TO display the login card in a centered, responsive layout, even though its visual theme changes to the Landing/Marketing Hero style

3.6 WHEN a user navigates between pages THEN the system SHALL CONTINUE TO reach the same controller routes and see the same data — no backend endpoints or Thymeleaf template logic is changed

3.7 WHEN `properties.html` or `register.html` is restyled under the Landing/Marketing Hero system THEN the system SHALL CONTINUE TO preserve their interactive contract exactly — including JavaScript hooks (e.g. `login-script.js` panel-switch and password-toggle behavior, `register.html`'s `setRole` script), form `action` attributes, and input `name`/`id` attributes — even though the visual styling changes substantially

3.8 WHEN a user navigates to `student/student-dashboard.html` THEN the system SHALL CONTINUE TO render its existing hero section unchanged (isolated Tailwind theme, out of scope), which now additionally serves as the canonical visual reference for the Landing/Marketing track
