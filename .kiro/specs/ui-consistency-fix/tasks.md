# Implementation Plan

This is **pure CSS, font-link (HTML `<head>`), and template markup/styling work**. No controller routes, Thymeleaf data binding, backend logic, or JavaScript behavior is touched. Tasks follow the exploratory bugfix flow: explore the bug first, capture preservation baselines, then apply the fix and re-validate.

> **Revision note**: This plan reflects the hybrid two-track design system revision (design.md / bugfix.md updated). Non-student pages now split into a **Dashboard/Functional track** (unchanged teal `Sora`/`Playfair Display` system — landlord, admin, applications, update, reviews, property-detail) and a **Landing/Marketing track** (new "bold hero" system — `Plus Jakarta Sans`/`Hanken Grotesk` + yellow accent — for `properties.html`, `logIn.html`, `register.html`). Tasks 1, 2, 4.1–4.4 are unaffected and unchanged. Task 3 stays complete as originally written, with a new follow-up task (3.1) to narrow its baseline. Tasks 4.5–4.9 are revised or new.

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

  - [x] 3.1 Revise preservation baseline for re-scoped landing pages (test-only)
    - **Property 2: Preservation** - Narrowed Interactive-Contract Baseline for properties.html and register.html
    - **IMPORTANT**: This is a test-only revision to `UiConsistencyPreservationTest.java` — no source/template/CSS changes
    - Per the revised design, `register.html` and root `properties.html` are no longer part of the frozen full-page visual snapshot — their visuals are now intentionally changing under the Landing/Marketing Hero track (design Property 4), so freezing them byte-for-byte is now the WRONG baseline
    - Remove the full computed-style/visual snapshot assertions for `register.html` and root `properties.html` from the preservation test
    - Add a narrower "interactive contract" snapshot for these two pages instead, asserting only:
      - Form `action` attributes (`/register` on `register.html`, `/search` on `properties.html`)
      - Input `name`/`id` attributes (e.g. `register.html`'s form fields; `properties.html`'s `minBedrooms`/`maxRent` filter inputs)
      - JS hook references (`register.html`'s inline `setRole()` script — assert the function and its call sites still exist unchanged)
    - Run BEFORE the Landing/Marketing track fix work (task 3.5 exploration test and task 4.7 Group F implementation), consistent with observation-first methodology — this remains a pre-fix baseline
    - Verify the narrowed snapshot still PASSES on the current (unfixed) code
    - All other snapshots in the test (student pages, Dashboard/Functional pages, JS behavior, routes) are unchanged from task 3
    - _Requirements: 3.1, 3.7_

- [x] 3.5. Write bug condition exploration test (Landing/Marketing Hero style deviation)
  - **Property 1: Bug Condition** - Landing/Marketing Hero Style Deviation
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix (task 4.7 Group F, and the login portion of task 4.5) when it passes after implementation
  - **GOAL**: Surface counterexamples demonstrating that the Landing/Marketing track pages don't yet use the Hero style
  - **Scoped PBT Approach**: The deviations are deterministic per page, so scope the property to the three concrete Landing/Marketing surfaces: `properties.html`, `register.html`, `logIn.html`
  - Load each page against the UNFIXED code and check:
    - `properties.html`: assert it has at least one linked stylesheet or `<style>` block and a non-default `font-family` — expect FAILURE (currently zero styling of any kind)
    - `register.html`: assert fonts are `Plus Jakarta Sans`/`Hanken Grotesk` and the active role-toggle/submit button uses the yellow accent (`#ffe170` background / `#221b00` text) — expect FAILURE (currently `Sora`/`Playfair Display` fonts and teal `hsl(180,67%,47%)`)
    - `logIn.html` / `login-style.css`: assert body font `Hanken Grotesk`, `.sa-logo` font `Plus Jakarta Sans`, and a background photo (not a flat fill) — expect FAILURE (currently `DM Sans`/`DM Serif Display`, flat `#121212`)
    - **Coordinate with tasks 4.5 and 4.9**: the login assertion here overlaps with the login theme fix in Group D — reuse this same test when verifying 4.9, do not write a second duplicate login test
  - The test assertions should match Property 4 from design (Plus Jakarta Sans/Hanken Grotesk, yellow accent `#ffe170`/`#221b00`, glass panels, photo+overlay hero surfaces, `999px`/`50px` pill radii)
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the Landing/Marketing deviation exists)
  - Document counterexamples found (e.g., "`properties.html` has zero stylesheet/style-block references", "`register.html` submit button resolves to teal instead of `#ffe170`", "`logIn.html` body resolves to `DM Sans` instead of `Hanken Grotesk`")
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.5, 1.6, 1.12, 1.13_

- [x] 4. Fix for UI design-system inconsistency across non-student pages

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

  - [x] 4.5 Group D — Login theme (Landing/Marketing Hero) + broken asset fix + font link
    - `templates/logIn.html`: change the Google Fonts `<link>` from `family=DM+Serif+Display&family=DM+Sans:wght@400;500` to the Hero fonts: `family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Hanken+Grotesk:wght@400;500;600;700` — **this font-link update was previously slated for Group E; it now lives here since login moved to the Landing/Marketing track**
    - `login-style.css`: `body { font-family: 'DM Sans' }` → `'Hanken Grotesk', sans-serif`; `.sa-logo { font-family: 'DM Serif Display' }` → `'Plus Jakarta Sans', sans-serif` (bold weight for the display treatment)
    - Replace the flat dark `--bg: #121212` fill with a full-width background photo + dark gradient overlay (the Hero pattern: `background-image: url(...)` plus `linear-gradient(to right, rgba(0,91,116,.8), transparent)` or an equivalent dark scrim), rather than either the old flat teal-light theme or a flat dark fill
    - `.page-overlay { background: url("image2.jpeg") ... }` → `url("login-image2.jpeg")`, now composed as part of the photo + overlay treatment (the corrected asset path is unchanged from the original fix; only the surrounding visual theme changes)
    - `.sa-card` adopts the glass-panel treatment: `background: rgba(255,255,255,.1)`, `backdrop-filter: blur(24px)`, `border: 1px solid rgba(255,255,255,.2)`, `border-radius: 16px` — replacing the previous opaque card so it reads correctly against the new photo background
    - Buttons: `.sa-btn-primary` → yellow pill CTA (`background: #ffe170; color: #221b00; border-radius: 999px`); `.sa-btn-secondary` → lighter glass/pill outline style consistent with the new theme
    - Preserve the centered, responsive mobile login card layout skeleton (Req 3.5) and all `login-script.js` hooks (panel switching, password toggle) — only fonts/colors/background/glass styling change, not structure or behavior
    - _Bug_Condition: isBugCondition(element) where element.surface == LOGIN (LANDING track) AND (element.fontFamily NOT IN {'Plus Jakarta Sans','Hanken Grotesk'} OR NOT hasPhotoWithDarkOverlay(element) OR NOT usesYellowAccent(element, '#ffe170', '#221b00') OR element.backgroundImage == "image2.jpeg")_
    - _Expected_Behavior: expectedBehavior(result) — Plus Jakarta Sans/Hanken Grotesk fonts, full-width photo + dark overlay, glass-panel card, yellow pill CTAs, working login-image2.jpeg backdrop_
    - _Preservation: login/register panel switch, password toggle, responsive layout unchanged (Req 3.5, 3.2)_
    - _Requirements: 1.5, 1.6, 1.7, 2.5, 2.6, 2.7, 3.5_

  - [x] 4.6 Group E — Template `<head>` font-link updates, Dashboard/Functional track only (design-only)
    - Update the Google Fonts `<link>` to request `Sora:wght@400;500;600;700` + `Playfair+Display:wght@400;500;700` (replacing `Plus Jakarta Sans`/`Hanken Grotesk`/`DM Sans`/`Fraunces`) in these 7 templates:
      - `templates/landlord/landlord-index.html`
      - `templates/landlord/listProperty.html`
      - `templates/landlord/manage-applications.html`
      - `templates/landlord/my-property-reviews.html`
      - `templates/application.html`
      - `templates/manage-properties.html`
      - `templates/update.html`
    - **`templates/logIn.html` is REMOVED from this list** — its font-link update now happens in task 4.5 (Group D), since login moved to the Landing/Marketing track
    - Change ONLY the font `<link>` — no `th:*` attributes, class names, element hierarchy, form actions, or routes
    - Do NOT touch already-aligned templates: `property-detail.html`, root `my-property-reviews.html`, `landlord/edit-property.html`, `student/student-dashboard.html`. `register.html` is also excluded here — its font-link update is handled separately in task 4.7 (Group F)
    - _Bug_Condition: isBugCondition(element) — correct CSS but wrong font families loaded via <link>_
    - _Expected_Behavior: expectedBehavior(result) — Sora/Playfair Display actually load (no system fallback)_
    - _Preservation: DOM structure, th:* markup, routes unchanged (Req 3.2, 3.6)_
    - _Requirements: 2.1, 2.8, 2.9, 2.10, 3.6_

  - [x] 4.7 Group F — Build Landing/Marketing Hero styling for properties.html and register.html
    - `templates/properties.html` (currently has no `<head>` styling of any kind):
      - Add the Landing/Marketing Google Fonts `<link>` (`Plus+Jakarta+Sans:wght@400;500;600;700;800` + `Hanken+Grotesk:wght@400;500;600;700`)
      - Add a new stylesheet (e.g. `src/main/resources/static/properties-style.css`, linked from the `<head>`) or an inline `<style>` block implementing: `Plus Jakarta Sans` for the page heading, `Hanken Grotesk` for body text, yellow-accent (`#ffe170`/`#221b00`) styling on the filter button/active filter state, pill-shaped inputs and the filter button (`border-radius: 999px`/`50px`), and card-style presentation for each result (rounded container, subtle shadow) echoing the glass-panel visual language
      - A full-bleed photo hero is not required (results-listing page, not the true landing hero) — layout is left to implementation judgment as long as the font/color/pill/glass language is applied
      - Do NOT alter the `th:each` iteration over `${properties}`, the `action="/search"` form target, or the `minBedrooms`/`maxRent` input `name` attributes — only add classes/wrapper markup needed for the new styling
    - `templates/register.html`:
      - Change the Google Fonts `<link>` from `Sora:wght@400;500;600;700&family=Playfair+Display:wght@700` to the Landing/Marketing Hero fonts (`Plus+Jakarta+Sans` + `Hanken+Grotesk`)
      - Rework the existing inline `<style>` block's tokens: swap `--primary`/`--primary-dark` teal values for the yellow accent (`#ffe170` background / `#221b00` text) on `.role-btn.active` and `.submit-btn`; swap `'Sora'`/`'Playfair Display'` font declarations for `'Hanken Grotesk'`/`'Plus Jakarta Sans'`
      - `.card` moves from a solid white opaque panel to the glass-panel treatment (`background: rgba(255,255,255,.1)`, `backdrop-filter: blur(24px)`, `border: 1px solid rgba(255,255,255,.2)`, `border-radius: 16px`) so it reads correctly against the existing background photo + overlay, which is already structurally correct and does not need to change
      - Do NOT alter `setRole()` JS, the `action="/register"` form target, or any input `name`/`id` attributes (Req 3.7) — this must match the narrowed preservation baseline established in task 3.1
    - _Bug_Condition: isBugCondition(element) where element.surface IN {properties.html, register.html} AND (hasNoStylesheetOrStyleBlockAtAll(element.surface) OR element.fontFamily NOT IN {'Plus Jakarta Sans','Hanken Grotesk'} OR NOT usesYellowAccent(element, '#ffe170', '#221b00') OR NOT hasGlassStyling(element))_
    - _Expected_Behavior: expectedBehavior(result) — Plus Jakarta Sans/Hanken Grotesk fonts, yellow-accent CTAs, glass-panel styling, 999px/50px pill radii, consistent with the student-dashboard hero reference_
    - _Preservation: `th:each` iteration, form actions (`/search`, `/register`), input name/id attributes, and `setRole()` JS unchanged (Req 3.1, 3.7) — per the narrowed baseline from task 3.1_
    - _Requirements: 1.12, 1.13, 2.12, 2.13, 3.1, 3.7_

  - [x] 4.8 Group G — Fix stylesheet path mismatches
    - `templates/landlord/manage-applications.html`: change `<link rel="stylesheet" href="/applications-style.css">` → `href="/Applications-style.css"` to match the actual file's casing on disk. Do not rename the CSS file itself, since it may already be referenced correctly elsewhere
    - `templates/landlord/my-property-reviews.html`: change `<link rel="stylesheet" href="/css/landlord-reviews.css">` → `href="/landlord-reviews.css"`, since the file is served from the static root, not a `/css/` subdirectory
    - These are independent of the font/token/theme fixes in Groups A–F; they only correct a `<link href>` so an already-corrected stylesheet actually loads — no routes or backend logic touched
    - _Bug_Condition: isBugCondition(element) where element.stylesheetHref IS REFERENCED AND NOT assetExistsAtPath(element.stylesheetHref)_
    - _Expected_Behavior: expectedBehavior(result) — every stylesheet reference resolves without a 404_
    - _Preservation: controller routes and backend logic unchanged_
    - _Requirements: 1.14, 1.15, 2.14, 2.15_

  - [x] 4.9 Verify bug condition exploration tests now pass
    - **Property 1: Expected Behavior** - Dashboard/Functional Design System Applied, Login Background Image Resolves, and Landing/Marketing Hero Style Applied
    - **IMPORTANT**: Re-run the SAME tests from tasks 1, 2, and 3.5 - do NOT write new tests
    - The tests from tasks 1, 2, and 3.5 encode the expected behavior for their respective tracks
    - Run the design-system deviation test (task 1), the login background asset test (task 2), and the Landing/Marketing Hero deviation test (task 3.5)
    - **EXPECTED OUTCOME**: All three tests PASS (confirms the Dashboard/Functional inconsistency is fixed, `image2.jpeg` → `login-image2.jpeg`, and the Landing/Marketing pages now use the Hero style)
    - _Requirements: Expected Behavior Properties from design (design Property 1, Property 2, Property 4 — all correspond to task-level Property 1: Bug Condition)_

  - [x] 4.10 Verify preservation tests still pass
    - **Property 2: Preservation** - Student Pages, Behavior, Structure, and Landing-Page Interactive Contract Unchanged
    - **IMPORTANT**: Re-run the SAME tests from tasks 3 and 3.1 - do NOT write new tests
    - Run the preservation property/snapshot tests from task 3, including the narrowed interactive-contract snapshot for `properties.html`/`register.html` established in task 3.1
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions — student visuals, JS, DOM, routes, Thymeleaf logic identical; and `properties.html`/`register.html` form actions, input names/ids, and `setRole()` JS identical despite their visual changes)
    - Confirm all tests still pass after the fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [x] 5. Checkpoint - Ensure all tests pass
  - Ensure all tests pass (fix checking + preservation checking) across both the Dashboard/Functional and Landing/Marketing tracks
  - Confirm every corrected stylesheet, new style block, and font `<link>` loads with no 404s (including `/Applications-style.css`, `/landlord-reviews.css`, and the new `properties.html` styling)
  - Optionally navigate the full app (student → landlord → admin → login/register → applications → update → reviews → properties search) and visually confirm: Dashboard/Functional pages share one consistent teal system, Landing/Marketing pages (`properties.html`, `logIn.html`, `register.html`) share one consistent Hero/yellow system, and the `login-image2.jpeg` backdrop renders with its dark overlay
  - Confirm `properties.html`/`register.html`'s interactive contract (form actions, input names/ids, `setRole()` JS) still matches the narrowed baseline from task 3.1
  - Ask the user if questions arise

## Task Dependency Graph

```
Task 1   (Explore: Dashboard/Functional design-system deviation)  ─┐
Task 2   (Explore: broken login asset)                            ─┤   (independent; all run on UNFIXED code)
Task 3   (Preservation baseline)                                  ─┤
  └─▶ Task 3.1 (Revise: narrow baseline for properties.html/       │
                register.html — test-only, before 4.7)            ─┤
Task 3.5 (Explore: Landing/Marketing Hero style deviation)        ─┘
        │
        ▼  (exploration + preservation baselines must exist first)
Task 4 (Fix)
   4.1 Shared tokens (recommended, enables 4.2–4.6)
        │
        ├─▶ 4.2 Group A (landlord token remap)          ┐
        ├─▶ 4.3 Group B (font-only fixes)               │  (4.2–4.6 are independent
        ├─▶ 4.4 Group C (admin radius)                  │   of each other; can run
        ├─▶ 4.5 Group D (login theme + asset + font link)│  in parallel)
        └─▶ 4.6 Group E (template <head> links, 7 pages) ┘
                     │
        4.7 Group F (Landing/Marketing: properties.html + register.html)
             ── depends on Task 3.1 (narrowed preservation baseline)
                and Task 3.5 (exploration test) existing first
                     │
                     ▼
             4.8 Group G (fix stylesheet path mismatches)
                     │
                     ▼
             4.9  Verify Bug Condition tests pass (re-run tasks 1, 2, 3.5)
             4.10 Verify Preservation tests pass (re-run tasks 3 & 3.1)
                     │
                     ▼
Task 5 (Checkpoint: all tests pass, no 404s, end-to-end visual)
```

Notes:
- Tasks 1, 2, 3, and 3.5 must complete on the UNFIXED code before Task 4's fix work begins (1, 2, and 3.5 must FAIL; 3 must PASS).
- Task 3.1 is a test-only revision and must land before task 4.7 (Group F) starts, since it defines the correct preservation baseline for the pages Group F is about to restyle.
- Task 4.1 (shared tokens) is recommended before 4.2–4.6 but each Dashboard/Functional file group (A, B, C, D, E) can be implemented independently of the others.
- Task 4.7 (Group F) and 4.8 (Group G) are independent of each other and of 4.2–4.6.
- Tasks 4.9 and 4.10 re-run the exact tests from Tasks 1/2/3.5 and 3/3.1 respectively — no new tests are written.
