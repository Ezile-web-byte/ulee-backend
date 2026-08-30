# Implementation Plan: View Property Location (A104)

## Overview

This plan implements an interactive Leaflet.js + OpenStreetMap map on the property detail page.
Work is confined to exactly two files: `src/main/resources/templates/property-detail.html` and
`database/ulee_database.sql`. There is no automated test suite for this project, so verification is
manual and visual — the final task is a hands-on verification checklist rather than automated tests.

**Hard scope (Requirement 7.1, 7.3):** only `property-detail.html` and `database/ulee_database.sql`
change. No `Property.java` change, no `PropertyController` / route change, no other page or resource.

> The user wants to review this plan and the two-file change list before implementation begins.

## Tasks

- [x] 1. Update the seed SQL: coordinate precision + Summerstrand seed data
  - [x] 1.1 Widen coordinate column precision in `database/ulee_database.sql`
    - Add an `ALTER TABLE \`property\`` statement after the `property` rows are inserted that
      `MODIFY`s `latitude` and `longitude` from `decimal(38,2)` to `decimal(9,6) DEFAULT NULL`
    - `decimal(9,6)` = 3 integer digits + 6 fractional digits, holding lat −90.000000…90.000000
      and lng −180.000000…180.000000 without truncation or overflow
    - _Requirements: 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Add the three Summerstrand `UPDATE` statements in `database/ulee_database.sql`
    - One `UPDATE ... WHERE \`propertyID\` = N` per property, each matching exactly one row:
      ID 1 (The Dunes) → (-34.009800, 25.673500); ID 2 (The Gomery) → (-34.005500, 25.666000);
      ID 3 (The admiralty) → (-34.013500, 25.679500)
    - All values sit inside lat [−34.02, −33.97] and lng [25.63, 25.69] with 6 decimal places,
      match each property's recorded Summerstrand address, and are pairwise well over 50 m apart
    - Place the `UPDATE`s after the `ALTER` from 1.1; a zero-match `UPDATE` leaves coordinates
      unchanged and MySQL surfaces the matched-row count
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 2. Add the map to `property-detail.html`
  - [x] 2.1 Add Leaflet.js + OpenStreetMap CDN assets to the page `<head>`
    - Add the SRI-pinned Leaflet 1.9.4 `<link rel="stylesheet">` (leaflet.css) and
      `<script>` (leaflet.js) from the unpkg CDN, alongside existing asset links
    - No API key and no billing credential are used; tiles come from OpenStreetMap tile servers
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 2.2 Add the map card CSS inside the existing `<style>` block
    - Add `.detail-map-card`, `#propertyMap`, and `.map-unavailable` rules mirroring the existing
      full-width cards (`.detail-description`, `.detail-vr`)
    - `.detail-map-card`: `grid-column: 1 / -1`, `background: var(--white)`,
      `border-radius: var(--radius)` (12px), `box-shadow: var(--shadow-md)`
    - `#propertyMap`: `height: 400px` (within 380–420px), `width: 100%`,
      `border-radius: var(--radius)`, `overflow: hidden` to clip OSM tiles to the rounded corners,
      `z-index: 0`
    - _Requirements: 2.4, 5.1, 5.2, 5.3, 5.4_

  - [x] 2.3 Add the Thymeleaf-guarded map card below the address
    - Insert a `.detail-map-card` block guarded by
      `th:if="${property.latitude != null and property.longitude != null}"` immediately after the
      `.detail-info-card` block, so it renders directly below the `.detail-location` address text
    - Include the `#propertyMap` div carrying `data-lat`/`data-lng` via `th:attr`, and a hidden
      `#mapUnavailable` fallback `<p>` that echoes the address
    - When either coordinate is null, the entire card (container, placeholder, loader) is omitted;
      the existing `.detail-location` address text is left unchanged and always renders
    - _Requirements: 2.1, 2.6, 3.1, 3.2, 3.3, 7.2_

  - [x] 2.4 Add the Leaflet init script before `</body>`
    - Add a `DOMContentLoaded` inline `<script>` near the existing thumbnail script
    - Locate `#propertyMap`; if absent (missing coords), do nothing (no map init, no error)
    - Parse `data-lat`/`data-lng`, guard against `typeof L === 'undefined'` or `NaN`, then
      `L.map('propertyMap').setView([lat, lng], 15)` (zoom within 14–16)
    - Add the OSM tile layer with the required attribution string, add exactly one marker at the
      coordinates, and enable default pan/zoom controls
    - Wire a 10-second fallback: if no tile `load` event fires, reveal `#mapUnavailable` while
      retaining the map container; wrap init in `try/catch` to show the unavailable message on error
    - _Requirements: 2.2, 2.3, 2.5, 2.7, 6.4, 6.5, 3.3_

- [x] 3. Manual verification checklist (visual — no automated tests)
  - Run the `ALTER` + three `UPDATE` statements against the database, then load the pages below
  - Load `GET /property/1`, `/property/2`, `/property/3` and confirm for each: a ~400px rounded
    map card appears directly below the address; a single pin sits over the correct Summerstrand
    location; the OpenStreetMap attribution text is visible; pan and zoom work; and the map tiles
    are clipped to the 12px rounded corners (no square tile edges poking past the corners)
  - Confirm a property with `NULL` coordinates renders the address text only — no map container,
    no placeholder/loader, and no browser-console error
  - Confirm all existing sections still render as before: image gallery, info card, description,
    features/amenities, 360° tour, and reviews
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 5.4, 6.4, 7.4, 7.5_

## Notes

- This project has no automated test suite; per the feature's hard constraint, verification is
  manual and visual only (Task 3). No unit, integration, or property-based test tasks are created.
- Every change is confined to two files: `src/main/resources/templates/property-detail.html` and
  `database/ulee_database.sql` (Requirement 7.1). No `Property.java`, `PropertyController`, route,
  or other resource is touched (Requirement 7.3).
- Each task references specific requirement clauses for traceability.
- The SQL `ALTER` + `UPDATE`s can also be run once directly against an existing database in MySQL
  Workbench.
- Task 3 is a manual verification pass, so it is not part of the parallel dependency graph below.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "2.2"] },
    { "id": 2, "tasks": ["2.3"] },
    { "id": 3, "tasks": ["2.4"] }
  ]
}
```
