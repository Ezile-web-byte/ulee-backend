# Implementation Plan

This is **pure CSS and font-link (HTML `<head>`) work**. No controller routes, Thymeleaf data binding, backend logic, or JavaScript behavior is touched. Tasks follow the exploratory bugfix flow: explore the bug first, capture preservation baselines, then apply the fix and re-validate.

- [x] 1. Write bug condition exploration test (design-system deviation)
  - **Property 1: Bug Condition** - Unified Design System Applied
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the visual inconsistency exists across non-student pages
  - **Scoped PBT Approach**: The deviations are deterministic per page/stylesheet, so scope the property to the concrete non-student surfaces enumerated in the design (landlord, admin, login/register, applications, update, reviews) and read their resolved styles.
  - Load each non-student page against the UNFIXED code and read computed styles / stylesheet values for representative elements (per design "Bug Condition" and "Fix Checking"):
    - Landlord dashboard (`landlord-style.css`): body `font-family`, a primary button `background-color` + `border-radius`, card `border-radius` — expect `Plus Jakarta Sans`, `#147592`, `8px`, `24px`
    - Landlord manage properties (`Manage properties.css`): same "Academic Vitality" token deviations
    - Landlord reviews (`landlord-reviews.css`): group title font + body font — expect `Plus Jakarta Sans` / `Hanken Grotesk`
    - Applications (`application.css`): body font — expect `DM Sans`
    - Update property (`update.css`): body + heading fonts — expect `DM Sans` / `Fraunces`
    - List property (`listProperty.css`): `--font-body` — expect `Plus Jakarta Sans`
    - Admin dashboard (`admin-style.css`, edge case): `.btn`/`.tab` `border-radius` — expect `7px` (while confirming admin colors/fonts already pass)
  - The test assertions should match Property 1 (Sora / Playfair Display, teal `hsl(180,67%,47%)`, no purple secondary, `50px` action buttons, `12px` cards with soft teal shadows)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the inconsistency exists)
  - Document counterexamples found (e.g., "landlord primary button resolves to `#147592`/`8px` instead of teal/`50px`") to confirm the root-cause map
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 1.9, 1.10, 1.11_

- [x] 2. Write bug condition exploration test (broken login background asset)
  - **Property 1: Bug Condition** - Login Background Image Reference Resolves
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the broken reference exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **GOAL**: Demonstrate that the login `.page-overlay` background image does not resolve
  - **Scoped PBT Approach**: Deterministic single case — assert the referenced login background asset exists at the served static path.
  - Read `login-style.css` `.page-overlay` `background: url(...)` and assert the referenced asset resolves to an existing file
  - On UNFIXED code the reference is `url("image2.jpeg")`, which does not exist (the real asset is `login-image2.jpeg`)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (confirms `image2.jpeg` 404s / does not resolve)
  - Document the counterexample (e.g., "`.page-overlay` requests `image2.jpeg` which does not exist; blurred backdrop never renders")
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.7_

- [x] 3. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Student Pages, Behavior, and Structure Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs (cases where isBugCondition returns false):
    - Snapshot computed styles of student pages: `property-detail.html`, `register.html`, root `my-property-reviews.html`, `properties.html`, `my-applications.html` (record fonts, teal tokens, button radii, card radii/shadows)
    - Snapshot DOM structure (class lists, element counts, `th:*`-produced markup) for pages whose stylesheets will change, so JS selectors and server bindings can be verified unchanged
    - Record JS-driven outcomes: sidebar collapse (`admin-script.js`, `landlord-script.js`), list-property wizard (`listProperty.js`), applications submit/cancel (`application.js`), update flows (`update.js`), login/register panel switch + password toggle (`login-script.js`)
    - Record controller-route responses (template + model data) for the affected pages
  - Write property-based / snapshot tests capturing these observed patterns (from Preservation Requirements in design)
  - Property-based / snapshot testing generates many element and interaction cases for stronger guarantees that nothing outside styling changes
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms the baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 4. Fix for UI design-system inconsistency across non-student pages

  - [x] 4.1 Add shared design-token stylesheet (recommended source of truth)
    - Create `src/main/resources/static/ulee-design-tokens.css` with the canonical `:root` tokens and base primitives (`.pill-btn`, card base, body font) per design "Canonical Design Tokens"
    - Tokens: `--primary: hsl(180,67%,47%)`, `--primary-dark: hsl(180,67%,36%)`, `--primary-deeper: hsl(180,67%,28%)`, `--primary-light: hsl(180,67%,90%)`, `--primary-pale: hsl(180,67%,96%)`, `--bg: hsl(0,0%,99%)`, `--radius: 12px`, `--pill: 50px`, `--font-body: 'Sora', sans-serif`, `--font-display: 'Playfair Display', serif`, soft teal-tinted shadow (e.g. `0 2px 12px rgba(0,120,110,.06)`)
    - Non-student pages may link it before their page-specific stylesheet; existing page CSS still normalizes tokens in-place for a low-risk fix
    - _Bug_Condition: isBugCondition(element) — non-student surface with deviating styling_
    - _Expected_Behavior: expectedBehavior(result) — canonical teal tokens, Sora/Playfair, 50px pills, 12px cards_
    - _Preservation: Student page visuals, JS, DOM, routes, Thymeleaf logic unchanged_
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 4.2 Group A — Remap "Academic Vitality" tokens onto the design system
    - `landlord-style.css`: `--font-display` → `'Playfair Display', serif`; `--font-body` → `'Sora', sans-serif`; `--primary: #147592` → `hsl(180,67%,47%)`; `--primary-dark: #005b74` → `hsl(180,67%,36%)`; add `--primary-deeper: hsl(180,67%,28%)`; neutralize purple secondary (`--secondary`, `--secondary-container`, `--secondary-tint`) → teal scale; `--radius-sm: 8px` → `12px` for cards; action buttons → `50px` pill; `--shadow-surface`/`--shadow-floating` → soft teal-tinted equivalents
    - `Manage properties.css`: identical token remapping (same block as `landlord-style.css`)
    - `landlord-reviews.css`: `.review-group-title` `'Plus Jakarta Sans'` → `'Playfair Display', serif`; hard-coded `'Hanken Grotesk'` → `'Sora', sans-serif`; align hard-coded grays/teal to canonical tokens
    - _Bug_Condition: isBugCondition(element) where element.surface == LANDLORD_
    - _Expected_Behavior: expectedBehavior(result) — teal scale, no purple, Sora/Playfair, 50px/12px, soft shadows_
    - _Preservation: DOM/markup and JS selectors on landlord pages unchanged (only CSS values change)_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.10, 3.4_

  - [x] 4.3 Group B — Font-only corrections (tokens already correct)
    - `application.css`: `body { font-family: 'DM Sans' ... }` → `'Sora', sans-serif`; use `Playfair Display` for headings where appropriate
    - `update.css`: `body { font-family: 'DM Sans' }` → `'Sora'`; `Fraunces` headings → `'Playfair Display'`
    - `listProperty.css`: `--font-body: 'Plus Jakarta Sans'` → `'Sora'`; keep `--font-display: 'Playfair Display'` (already correct)
    - Do not change colors/radii in these files — they already match the design system
    - _Bug_Condition: isBugCondition(element) — correct tokens but wrong font-family_
    - _Expected_Behavior: expectedBehavior(result) — body Sora, headings Playfair Display_
    - _Preservation: teal tokens, radii, layout in these files unchanged_
    - _Requirements: 2.8, 2.9_

  - [x] 4.4 Group C — Admin button radius correction
    - `admin-style.css`: change `.btn` and `.tab` `border-radius: 7px` → `50px` (pill)
    - Leave all colors, fonts (`Sora`), layout, and `--r: 12px` cards untouched
    - _Bug_Condition: isBugCondition(element) where element.role == ACTION_BUTTON AND borderRadius != 50px_
    - _Expected_Behavior: expectedBehavior(result) — .btn/.tab border-radius == 50px_
    - _Preservation: admin colors, fonts, card radius, layout/data unchanged (Req 3.3)_
    - _Requirements: 2.11, 3.3_

  - [ ] 4.5 Group D — Login theme + broken asset fix
    - `login-style.css`: `body { font-family: 'DM Sans' }` → `'Sora'`; `.sa-logo { font-family: 'DM Serif Display' }` → `'Playfair Display'`
    - Replace flat dark `--bg: #121212` with a light theme (or a tasteful dark overlay layered on the working background image — not a flat fill)
    - `.page-overlay { background: url("image2.jpeg") ... }` → `url("login-image2.jpeg")`
    - Buttons (`.sa-btn-primary`, `.sa-btn-secondary`) → `border-radius: 50px` pill; align greens to `hsl(180,67%,47%)`
    - Preserve the centered, responsive mobile login card layout (Req 3.5)
    - _Bug_Condition: isBugCondition(element) — login surface deviating styling AND broken image2.jpeg reference_
    - _Expected_Behavior: expectedBehavior(result) — Sora/Playfair, light/tasteful theme, 50px pills, working login-image2.jpeg backdrop_
    - _Preservation: login/register panel switch, password toggle, responsive layout unchanged_
    - _Requirements: 2.5, 2.6, 2.7, 3.5_

  - [ ] 4.6 Group E — Template `<head>` font-link updates (design-only)
    - Update the Google Fonts `<link>` to request `Sora:wght@400;500;600;700` + `Playfair+Display:wght@400;500;700` (replacing `Plus Jakarta Sans`/`Hanken Grotesk`/`DM Sans`/`Fraunces`) in:
      - `templates/landlord/landlord-index.html`
      - `templates/landlord/listProperty.html`
      - `templates/landlord/manage-applications.html`
      - `templates/landlord/my-property-reviews.html`
      - `templates/application.html`
      - `templates/logIn.html`
      - `templates/manage-properties.html`
      - `templates/update.html`
    - Change ONLY the font `<link>` — no `th:*` attributes, class names, element hierarchy, form actions, or routes
    - Do NOT touch already-aligned templates: `property-detail.html`, `register.html`, root `my-property-reviews.html`, `landlord/edit-property.html`, `student/student-dashboard.html`
    - _Bug_Condition: isBugCondition(element) — correct CSS but wrong font families loaded via <link>_
    - _Expected_Behavior: expectedBehavior(result) — Sora/Playfair Display actually load (no system fallback)_
    - _Preservation: DOM structure, th:* markup, routes unchanged (Req 3.2, 3.6)_
    - _Requirements: 2.1, 2.5, 2.9, 2.10, 3.6_

  - [ ] 4.7 Resolve pre-existing stylesheet path risks (verify during implementation)
    - `manage-applications.html` links `/applications-style.css` but the file is `Applications-style.css` (case mismatch — can 404 on case-sensitive servers)
    - `my-property-reviews.html` (landlord) links `/css/landlord-reviews.css` but the file lives at the static root (`/landlord-reviews.css`)
    - If a corrected stylesheet still does not load, fix the reference WITHOUT changing routes/logic
    - _Bug_Condition: isBugCondition(element) — corrected stylesheet fails to load due to path mismatch_
    - _Expected_Behavior: expectedBehavior(result) — every corrected stylesheet resolves (no 404)_
    - _Preservation: controller routes and backend logic unchanged_
    - _Requirements: 2.1, 2.8, 2.10_

  - [ ] 4.8 Verify bug condition exploration tests now pass
    - **Property 1: Expected Behavior** - Unified Design System Applied & Login Image Resolves
    - **IMPORTANT**: Re-run the SAME tests from tasks 1 and 2 - do NOT write new tests
    - The tests from tasks 1 and 2 encode the expected behavior
    - When these tests pass, they confirm the design system is applied and the login asset resolves
    - Run the design-system deviation test (task 1) and the login background asset test (task 2)
    - **EXPECTED OUTCOME**: Tests PASS (confirms the inconsistency is fixed and image2.jpeg → login-image2.jpeg)
    - _Requirements: Expected Behavior Properties from design (Property 1, Property 2)_

  - [ ] 4.9 Verify preservation tests still pass
    - **Property 2: Preservation** - Student Pages, Behavior, and Structure Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 3 - do NOT write new tests
    - Run the preservation property/snapshot tests from task 3
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions — student visuals, JS, DOM, routes, Thymeleaf logic identical)
    - Confirm all tests still pass after the fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ] 5. Checkpoint - Ensure all tests pass
  - Ensure all tests pass (fix checking + preservation checking)
  - Confirm every corrected stylesheet and font `<link>` loads with no 404s (including the flagged `/applications-style.css` and `/css/landlord-reviews.css` path risks)
  - Optionally navigate the full app (student → landlord → admin → login/register → applications → update → reviews) and visually confirm one consistent design system end to end, and that the login `login-image2.jpeg` backdrop renders
  - Ask the user if questions arise

## Task Dependency Graph

```
Task 1 (Explore: design-system deviation)   ─┐
Task 2 (Explore: broken login asset)         ─┤   (independent; all run on UNFIXED code)
Task 3 (Preservation baseline)               ─┘
        │
        ▼  (exploration + preservation baselines must exist first)
Task 4 (Fix)
   4.1 Shared tokens (recommended, enables 4.2–4.7)
        │
        ├─▶ 4.2 Group A (landlord token remap)      ┐
        ├─▶ 4.3 Group B (font-only fixes)           │  (4.2–4.6 are independent
        ├─▶ 4.4 Group C (admin radius)              │   of each other; can run
        ├─▶ 4.5 Group D (login theme + asset)       │   in parallel)
        └─▶ 4.6 Group E (template <head> links)     ┘
                     │
                     ▼
             4.7 Resolve path risks (verify stylesheets load)
                     │
                     ▼
             4.8 Verify Property 1 passes (re-run tasks 1 & 2)
             4.9 Verify Property 2 passes (re-run task 3)
                     │
                     ▼
Task 5 (Checkpoint: all tests pass, no 404s, end-to-end visual)
```

Notes:
- Tasks 1, 2, and 3 must complete on the UNFIXED code before Task 4 begins (1 & 2 must FAIL; 3 must PASS).
- Task 4.1 (shared tokens) is recommended before 4.2–4.7 but each file group (A–E) can be implemented independently.
- Tasks 4.8 and 4.9 re-run the exact tests from Tasks 1/2 and 3 respectively — no new tests are written.
