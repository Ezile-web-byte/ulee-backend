# Requirements Document

## Introduction

Feature A104 — View Property Location. The property detail page (`property-detail.html`) currently shows a
property's location only as plain text (`📍 <address>, <city>` inside the `.detail-location` element). This
feature adds a small interactive map, rendered with **Leaflet.js over OpenStreetMap tiles**, directly below the
address text. The map shows a single pin at the property's exact coordinates and matches the existing card
design system (rounded corners, white surface, soft shadow).

The technology choice (Leaflet.js + OpenStreetMap, **not** Google Maps) is fixed: it is free, needs no API key
and no billing account, which suits a student project.

### Grounding notes (verified against the current code)

These facts are confirmed by reading the repository and shape the requirements below:

- **The `latitude` and `longitude` columns already exist.** In `database/ulee_database.sql` the `property`
  table declares `` `latitude` decimal(38,2) `` and `` `longitude` decimal(38,2) ``. No new column needs to be
  created. **However, `decimal(38,2)` stores only 2 decimal places (~1.1 km precision)**, which is too coarse
  for a street-level pin. The columns therefore need a precision change (an `ALTER`), not an add.
- **The entity is already wired.** `Property.java` already declares `private java.math.BigDecimal latitude;`
  and `private java.math.BigDecimal longitude;` with `getLatitude()/setLatitude()` and
  `getLongitude()/setLongitude()`. No entity change is needed.
- **The controller already passes the data.** `PropertyController.viewPropertyDetail(...)` already adds the
  full `property` object to the model (`model.addAttribute("property", property)`), so
  `${property.latitude}` and `${property.longitude}` are already reachable from the template. No controller
  route change is required.
- **The three seeded properties exist with `NULL` coordinates.** `database/ulee_database.sql` inserts
  propertyID 1 = *The Dunes*, 2 = *The Gomery*, 3 = *The admiralty* — all in Summerstrand, Gqeberha — with
  `latitude, longitude` currently set to `NULL`.
- **The card design tokens exist.** `property-detail.html` defines `--radius: 12px`, `--shadow-sm`,
  `--shadow-md` and renders full-width cards (`.detail-description`, `.detail-vr`) using
  `grid-column: 1 / -1`, `background: var(--white)`, `border-radius: var(--radius)` and a box shadow.

### Scope constraints (hard)

- Only `property-detail.html` (and the seed SQL / a small schema-precision migration) may change. No other
  page may be touched.
- No controller route may be modified beyond what is strictly needed to expose latitude/longitude to the view
  (and, as grounded above, nothing is currently needed since `property` is already in the model).
- No new automated test suite is introduced. Verification is manual and visual. Correctness properties are
  kept lightweight and are not backed by property-based tests.
- Project conventions are preserved: Spring Boot + Thymeleaf, plain JPA entities (manual getters/setters, no
  Lombok), `@Autowired` field injection in controllers, no service layer.

## Glossary

- **Property_Detail_Page**: The server-rendered Thymeleaf view `property-detail.html` reached via
  `GET /property/{id}`.
- **Address_Text**: The existing `.detail-location` element that renders `📍 <address>, <city>`.
- **Location_Map**: The new interactive map card added below the Address_Text on the Property_Detail_Page.
- **Map_Renderer**: The client-side Leaflet.js script that initializes the Location_Map, loads OpenStreetMap
  tiles, and places the pin.
- **Coordinates**: The pair of `latitude` and `longitude` `BigDecimal` values on the `Property` entity /
  `property` table.
- **Property**: The JPA entity `Property.java` and its backing `property` table row.
- **Seed_Script**: The SQL in `database/ulee_database.sql` (and any accompanying `UPDATE` statements) that
  populates the three demonstration properties.
- **Summerstrand_Area**: The Summerstrand suburb of Gqeberha (Port Elizabeth), Nelson Mandela Bay, South
  Africa — approximately latitude −34.02 to −33.97 and longitude 25.63 to 25.69.

## Requirements

### Requirement 1: Reuse existing coordinate storage with sufficient precision

**User Story:** As a developer, I want the property's latitude and longitude to be stored with enough precision to place an accurate pin, so that the map reflects the property's real location.

#### Acceptance Criteria

1. THE Property entity SHALL expose the existing `latitude` and `longitude` `BigDecimal` fields through their public getters, and SHALL NOT introduce any new coordinate-related fields.
2. WHERE the `latitude` and `longitude` columns already exist on the `property` table, THE Seed_Script SHALL reference those existing columns and SHALL NOT create additional coordinate columns.
3. WHERE the existing `latitude` and `longitude` columns store fewer than 6 fractional decimal digits (e.g., the current `decimal(38,2)` scale of 2), THE Seed_Script SHALL include an `ALTER TABLE` statement that redefines each column to a decimal type with a fractional scale of exactly 6 decimal digits.
4. WHERE the `latitude` and `longitude` columns are redefined or created, THE Seed_Script SHALL size each column so it can hold the full valid coordinate range, storing latitude values from -90.000000 to 90.000000 and longitude values from -180.000000 to 180.000000 without truncation or overflow.
5. IF a future `property` table is created without `latitude` and `longitude` columns, THEN THE Seed_Script SHALL define both columns as a decimal type with a fractional scale of exactly 6 decimal digits.

### Requirement 2: Display an interactive map below the address

**User Story:** As a student viewing a property, I want to see an interactive map with a pin, so that I can understand where the property is located.

#### Acceptance Criteria

1. WHEN the Property_Detail_Page is rendered for a Property whose `latitude` and `longitude` are both non-null, THE Property_Detail_Page SHALL display the Location_Map positioned immediately below the Address_Text (.detail-location element).
2. WHEN the Location_Map is displayed, THE Map_Renderer SHALL center the map on the Property's Coordinates and place exactly one pin at those Coordinates.
3. WHEN the Location_Map is displayed, THE Map_Renderer SHALL set an initial zoom level between 14 and 16 inclusive.
4. WHEN the Location_Map is displayed, THE Location_Map SHALL render with a visible height between 380 and 420 pixels inclusive.
5. WHEN the Location_Map is displayed, THE Map_Renderer SHALL allow the user to change the zoom level within the range supported by the map tiles and to pan the map in any direction.
6. IF the Property's `latitude` or `longitude` is null, THEN THE Property_Detail_Page SHALL NOT display the Location_Map and SHALL continue to display the Address_Text.
7. IF the map tiles or Map_Renderer fail to load within 10 seconds of the Property_Detail_Page rendering, THEN THE Location_Map SHALL display a visible message indicating the map is unavailable while preserving the display of the Address_Text.

### Requirement 3: Graceful degradation when coordinates are missing

**User Story:** As a student viewing a property that has no coordinates, I want to still see its address cleanly, so that I never encounter a broken or blank map.

#### Acceptance Criteria

1. WHEN the Property_Detail_Page is rendered for a Property whose `latitude` is null OR whose `longitude` is null, THE Property_Detail_Page SHALL display the Address_Text.
2. WHEN the Property_Detail_Page is rendered for a Property whose `latitude` is null OR whose `longitude` is null, THE Property_Detail_Page SHALL omit the Location_Map, including its map container element, any placeholder, and any loading indicator, from the rendered page.
3. WHILE a Property has a null `latitude` or a null `longitude`, THE Property_Detail_Page SHALL NOT initiate any client-side map rendering and SHALL NOT emit any client-side map error.
4. IF a Property has a null `latitude` or a null `longitude` AND its Address_Text is null or empty, THEN THE Property_Detail_Page SHALL display a message indicating that the address is unavailable in place of the Address_Text, and SHALL still omit the Location_Map.

### Requirement 4: Seed real Summerstrand coordinates for the three demonstration properties

**User Story:** As a demonstrator, I want the three existing seeded properties to show on the map, so that the feature can be shown working without creating new data.

#### Acceptance Criteria

1. THE Seed_Script SHALL provide SQL `UPDATE` statements that set non-null `latitude` and `longitude` values for propertyID 1 (The Dunes), propertyID 2 (The Gomery), and propertyID 3 (The admiralty), with each statement matching exactly one property row.
2. THE Seed_Script SHALL assign `latitude` values within the inclusive range −34.02 to −33.97 and `longitude` values within the inclusive range 25.63 to 25.69, expressed with at least 5 decimal places of precision.
3. THE Seed_Script SHALL assign to each property a coordinate pair that corresponds to a physical point located within the Summerstrand suburb of Gqeberha, South Africa, and consistent with that property's recorded address rather than arbitrary or randomly generated values.
4. THE Seed_Script SHALL assign coordinates such that every pair of the three properties differs by a great-circle distance of at least 50 meters, so that no two pins render at the same point.
5. WHEN an `UPDATE` statement matches zero rows because the target propertyID does not exist, THEN THE Seed_Script SHALL leave all existing coordinate values unchanged and surface an indication that the target property was not found.

### Requirement 5: Match the existing card design system

**User Story:** As a student, I want the map to look like the rest of the page, so that the detail page feels consistent and polished.

#### Acceptance Criteria

1. THE Location_Map container SHALL apply a corner radius equal to the `--radius` token value (12px) and a background equal to the `var(--white)` token, matching the full-width cards `.detail-description` and `.detail-vr`.
2. THE Location_Map container SHALL apply the `--shadow-md` box-shadow token, matching the shadow used by the other full-width cards on the property-detail page.
3. THE Location_Map container SHALL span the full content width using the `grid-column: 1 / -1` layout, occupying the same left and right content edges as the other full-width cards.
4. WHEN the map renders its tiles, THE Location_Map SHALL clip all tile content to the container's `--radius` (12px) rounded corners so that zero square tile edges are visible outside the card's corner radius.

### Requirement 6: Use Leaflet.js with OpenStreetMap tiles

**User Story:** As the project owner, I want the map built on a free, key-free library, so that the project incurs no cost and needs no billing setup.

#### Acceptance Criteria

1. WHEN the property location view is displayed, THE Map_Renderer SHALL render the Location_Map using the Leaflet.js library.
2. WHEN the Location_Map is rendered, THE Map_Renderer SHALL load all map tiles from OpenStreetMap tile servers.
3. THE Map_Renderer SHALL load and display the Location_Map without requiring an API key or a billing account credential.
4. WHILE the Location_Map is displayed, THE Location_Map SHALL show the visible OpenStreetMap attribution text required by the OpenStreetMap tile usage terms.
5. IF one or more OpenStreetMap tiles fail to load within 10 seconds of the render request, THEN THE Map_Renderer SHALL display a visible indication that the map could not be fully loaded and SHALL retain the map container in the view.

### Requirement 7: Preserve existing page behavior and scope

**User Story:** As a developer, I want this change confined to the property detail page, so that no other part of the application regresses.

#### Acceptance Criteria

1. THE view-property-location change SHALL modify only the following artifacts: `property-detail.html`, the Seed_Script, and the `latitude` and `longitude` column precision of the `property` table (per Requirement 1.3), and SHALL NOT create, delete, or modify any other source, configuration, or resource file.
2. WHEN the Property_Detail_Page is rendered, THE Property_Detail_Page SHALL display the existing Address_Text, whether or not the Location_Map is displayed.
3. THE view-property-location change SHALL NOT add, remove, or alter any controller route, and SHALL make `latitude` and `longitude` available to the view solely by reusing the existing `property` model attribute already added by `PropertyController.viewPropertyDetail`.
4. WHEN the Property_Detail_Page is rendered, THE Property_Detail_Page SHALL display each of the following existing sections with the same content and behavior as before the change: image gallery, info card, description, features/amenities, 360° tour, and reviews.
5. IF a property has no `latitude` or no `longitude` value, THEN THE Property_Detail_Page SHALL still render the Address_Text and all existing sections listed in criterion 4 without raising an error or leaving any section blank.

## Verification Approach

Verification for this feature is manual and visual (per project constraint — no new automated test suite):

- Load `GET /property/1`, `/property/2`, `/property/3` after running the seed/update SQL and confirm each shows
  a ~400px map with a single pin over the correct Summerstrand location, styled as a rounded card below the
  address.
- Temporarily view (or reason about) a property with `NULL` coordinates and confirm only the address text
  shows, with no map container or console error.
