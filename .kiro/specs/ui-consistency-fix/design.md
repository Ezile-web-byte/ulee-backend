# UI Consistency Fix — Bugfix Design

## Overview

Three team members built the student, landlord, and admin sections of ULEE independently, each inventing its own design language. The result is a visually fractured product: fonts, colors, button shapes, card radii, and shadows all differ from one surface to the next, and the login page even points at a background image that does not exist.

This bugfix unifies every page under the **student page design system** — the look already established in `property-detail.html`, `register.html`, and the student-facing token blocks. That system is defined by:

- **Fonts**: `Sora` (body) + `Playfair Display` (display/headings)
- **Color**: teal scale anchored on `--primary: hsl(180, 67%, 47%)` (no purple secondary)
- **Buttons**: pill-shaped, `border-radius: 50px`
- **Cards**: `border-radius: 12px` with soft, teal-tinted shadows
- **Background**: light `hsl(0, 0%, 99%)`

The fix is **pure CSS and font-link (HTML `<head>`) work**. No controller routes, Thymeleaf data binding, backend logic, or JavaScript behavior is touched. The strategy is a targeted, low-regression normalization: for files that already carry the correct teal tokens (e.g. `application.css`, `update.css`) only the font declarations change; for files built on the divergent "Academic Vitality" palette (`landlord-style.css`, `Manage properties.css`, `landlord-reviews.css`) the design tokens are remapped to the canonical values; for `admin-style.css` only the button/tab radius is pill-ified; and for `login-style.css` the fonts, background theme, and broken image reference are corrected.

To reduce future drift, the design also recommends introducing a single shared token stylesheet (`ulee-design-tokens.css`) that non-student pages can link, so the design system has one source of truth going forward.

## Glossary

- **Bug_Condition (C)**: A rendered page/element belongs to a non-student surface (landlord, admin, login/register, applications, update, reviews) and its resolved styling deviates from the design system (wrong font, non-teal palette, non-pill buttons, non-12px cards), **or** it references a stylesheet asset that does not exist (the broken login background image).
- **Property (P)**: After the fix, the element renders with `Sora`/`Playfair Display`, the canonical teal token scale, pill-shaped (`50px`) action buttons, `12px` cards with soft shadows, and every referenced asset resolves.
- **Preservation**: Behavior that must remain byte-for-byte unchanged — student page visuals, all JavaScript functionality, DOM structure/markup, controller routes, and Thymeleaf template logic.
- **Design System**: The student-page style captured canonically in `property-detail.html` / `register.html` and the `:root` token blocks that use `hsl(180, 67%, 47%)`.
- **Design Tokens**: The `:root` CSS custom properties (colors, fonts, radii, shadows) that a page's rules resolve against.
- **Academic Vitality**: The name of the divergent landlord design system (`--primary: #147592`, `--secondary: #441587`, `Plus Jakarta Sans` / `Hanken Grotesk`, `--radius-sm: 8px` / `--radius-lg: 24px`) that must be remapped onto the canonical tokens.

## Bug Details

### Bug Condition

The bug manifests whenever a user views a non-student page. The stylesheet that governs that page either (a) declares the wrong font family, (b) resolves to a non-canonical color palette (deep teal `#147592` plus royal purple `#441587`, or a flat dark `#121212` background), (c) renders rectangular buttons instead of pills, (d) uses non-standard card radii/shadows, or (e) — for the login page only — points `background: url(...)` at `image2.jpeg`, a file that does not exist (the real asset is `login-image2.jpeg`).

**Formal Specification:**
```
FUNCTION isBugCondition(element)
  INPUT: element — a rendered page element with resolved style + its owning surface
  OUTPUT: boolean

  IF element.surface == STUDENT THEN
    RETURN false            // student pages are the reference, never buggy

  stylingDeviates :=
        element.fontFamily NOT IN { 'Sora', 'Playfair Display' }
     OR element.primaryColor != hsl(180,67%,47%)
     OR usesPurpleSecondary(element)
     OR (element.role == ACTION_BUTTON AND element.borderRadius != 50px)
     OR (element.role == CARD          AND element.borderRadius != 12px)
     OR element.background == flatDark(#121212)

  brokenAsset :=
        element.backgroundImage IS REFERENCED
     AND NOT assetExists(element.backgroundImage)

  RETURN stylingDeviates OR brokenAsset
END FUNCTION
```

### Examples

- **Landlord dashboard** (`landlord-index.html` → `landlord-style.css`): headings render in `Plus Jakarta Sans`, primary buttons are `#147592` with `border-radius: 8px`, cards use `24px` radius and `rgba(20,117,146,.08)` shadows. Expected: `Playfair Display` headings, `hsl(180,67%,47%)` pill buttons (`50px`), `12px` cards with soft teal shadows.
- **Login page** (`logIn.html` → `login-style.css`): body font is `DM Sans`, background is a flat `#121212`, and `.page-overlay` requests `url("image2.jpeg")` which 404s, so no blurred backdrop shows. Expected: `Sora` body, `Playfair Display` logo, a working blurred backdrop from `url("login-image2.jpeg")` with a tasteful overlay.
- **Admin dashboard** (`admin-index.html` → `admin-style.css`): already uses `Sora` and the correct teal, but `.btn`/`.tab` use `border-radius: 7px`. Expected: pill `50px` action buttons.
- **Applications page** (`application.html` → `application.css`): teal tokens are already correct, but body font is `DM Sans`. Expected: `Sora` body font. (Edge case: only the font declaration is wrong; colors/radii already match.)
- **Update property page** (`update.html` → `update.css`): teal tokens correct, but fonts are `DM Sans` / `Fraunces`. Expected: `Sora` / `Playfair Display`.
- **Reviews page** (`my-property-reviews.html` landlord template → `landlord-reviews.css`): group titles in `Plus Jakarta Sans`, body in `Hanken Grotesk`. Expected: `Playfair Display` titles, `Sora` body.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Student pages (`property-detail.html`, `register.html`, root `my-property-reviews.html`, `properties.html`, `my-applications.html`, `student/student-dashboard.html`) must render exactly as they do today — they already embody the design system (or, for `student-dashboard.html`, use an isolated Tailwind theme that is out of scope).
- All JavaScript behavior must be identical: sidebar collapse (`admin-script.js`, `landlord-script.js`), the list-property wizard (`listProperty.js`), applications actions (`application.js`), update flows (`update.js`), login/register panel switching and password toggle (`login-script.js`).
- DOM structure and Thymeleaf markup (class names, element hierarchy, `th:*` attributes, form actions) must be preserved so JS selectors and server bindings keep working.
- Controller routes and the data rendered into each template must be unchanged.

**Scope:**
All inputs that do NOT involve non-student page styling must be completely unaffected by this fix. This includes:
- Any interaction handled by JavaScript (form submissions, filtering, modals, tabs, sidebar toggles).
- Any server round-trip (route resolution, model attributes, Thymeleaf iteration/conditionals).
- The DOM contract every script and template depends on (only CSS property values and font `<link>`s change; no class renames, no markup restructuring).

**Note:** The concrete "correct" styling is defined in the Correctness Properties section (Property 1 and Property 2). This section defines what must NOT change.

## Hypothesized Root Cause

Based on the bug analysis, the divergence has these root causes:

1. **Independent design systems, no shared tokens**: Each author defined their own `:root` block. Student/admin/applications/update pages use the canonical teal scale (`hsl(180,67%,47%)`); landlord pages use a separate "Academic Vitality" palette (`--primary: #147592`, `--secondary: #441587`).
   - `landlord-style.css`, `Manage properties.css`: `Plus Jakarta Sans` + `Hanken Grotesk`, `--radius-sm: 8px`, `--radius-lg: 24px`, teal+purple.
   - `landlord-reviews.css`: hard-coded `Plus Jakarta Sans` / `Hanken Grotesk` font families (no tokens).

2. **Wrong font families with correct colors**: `application.css` and `update.css` already carry the right teal tokens and `12px` radius but declare `DM Sans` (and `Fraunces` in update). Only the `font-family` declarations and the page `<head>` font `<link>`s are wrong.

3. **Non-pill buttons in an otherwise-correct file**: `admin-style.css` is on-palette and on-font (`Sora`) but uses `border-radius: 7px` on `.btn` and `.tab`, breaking the pill convention.

4. **Login page built on a different theme + broken asset reference**: `login-style.css` uses `DM Sans` / `DM Serif Display`, a dark `--bg: #121212`, and `.page-overlay { background: url("image2.jpeg") ... }` — but the deployed asset is `login-image2.jpeg`, so the reference resolves to nothing.

5. **Font `<link>` tags in templates load the wrong families**: even after CSS is corrected, several templates only request `Plus Jakarta Sans`/`Hanken Grotesk`/`DM Sans`/`Fraunces` from Google Fonts, so `Sora`/`Playfair Display` would fall back to a system serif/sans unless the `<link>` is updated too.

## Correctness Properties

Property 1: Bug Condition - Unified Design System Applied

_For any_ non-student page element where the bug condition holds (isBugCondition returns true due to styling deviation), the fixed stylesheets SHALL render that element using `Sora` for body text and `Playfair Display` for display/heading text, the canonical teal token scale (`--primary: hsl(180,67%,47%)`, `--primary-dark: hsl(180,67%,36%)`, `--primary-deeper: hsl(180,67%,28%)`) with no purple secondary, action buttons with `border-radius: 50px`, cards with `border-radius: 12px` and soft teal-tinted shadows, and a light background consistent with the rest of the app.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.8, 2.9, 2.10, 2.11**

Property 2: Bug Condition - Login Background Image Reference Resolves

_For any_ load of the login/register page where the bug condition holds (isBugCondition returns true due to the broken `image2.jpeg` reference), the fixed `login-style.css` SHALL reference `url("login-image2.jpeg")`, an asset that exists at the served static path, so the blurred background renders.

**Validates: Requirements 2.7**

Property 3: Preservation - Student Pages, Behavior, and Structure Unchanged

_For any_ input where the bug condition does NOT hold (isBugCondition returns false) — i.e. student-page elements, JavaScript-driven interactions, DOM structure, controller routes, and Thymeleaf logic — the fixed code SHALL produce exactly the same result as the original code, preserving all visual output on student pages and all functional behavior across the application.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Canonical Design Tokens (target values)

```
--primary:        hsl(180, 67%, 47%);
--primary-dark:   hsl(180, 67%, 36%);
--primary-deeper: hsl(180, 67%, 28%);
--primary-light:  hsl(180, 67%, 90%);
--primary-pale:   hsl(180, 67%, 96%);
--bg:             hsl(0, 0%, 99%);
--text:           #1a1a1a;
--muted:          #777;
--white:          #ffffff;
--border:         hsl(180, 20%, 88%);
--radius:         12px;   /* cards / containers */
--pill:           50px;   /* action buttons */
--font-body:      'Sora', sans-serif;
--font-display:   'Playfair Display', serif;
/* soft teal-tinted shadows, e.g. 0 2px 12px rgba(0,120,110,.06) */
```

### Recommended shared-CSS approach

Introduce a new **`src/main/resources/static/ulee-design-tokens.css`** containing the canonical `:root` tokens plus a small set of base primitives (`.pill-btn`, card base, body font). Non-student pages link it **before** their page-specific stylesheet, so the tokens have one source of truth and future pages inherit the system by default. Because every existing page CSS already redefines `:root`, the immediate low-risk fix still normalizes tokens in-place within each file; the shared file is the strategic target that page CSS can progressively delegate to. This keeps the "premium, polished, futuristic" intent centralized rather than copy-pasted.

### Changes required (per file)

**Group A — Remap "Academic Vitality" tokens onto the design system**

**File**: `src/main/resources/static/landlord-style.css`
1. `--font-display` → `'Playfair Display', serif`; `--font-body` → `'Sora', sans-serif`.
2. `--primary: #147592` → `hsl(180,67%,47%)`; `--primary-dark: #005b74` → `hsl(180,67%,36%)`; add `--primary-deeper`.
3. Remove/neutralize purple secondary usage (`--secondary`, `--secondary-container`, `--secondary-tint`) — repoint any visible use to the teal scale.
4. `--radius-sm: 8px` → `12px` for cards/containers; action buttons → `50px` pill.
5. `--shadow-surface`/`--shadow-floating` → soft teal-tinted equivalents matching student cards.

**File**: `src/main/resources/static/Manage properties.css` — identical remapping to `landlord-style.css` (same token block).

**File**: `src/main/resources/static/landlord-reviews.css`
1. Replace hard-coded `font-family: 'Plus Jakarta Sans'` on `.review-group-title` with `'Playfair Display', serif`.
2. Replace hard-coded `font-family: 'Hanken Grotesk'` occurrences with `'Sora', sans-serif`.
3. Align any hard-coded grays/teal with the canonical tokens.

**Group B — Font-only corrections (tokens already correct)**

**File**: `src/main/resources/static/application.css` — `body { font-family: 'DM Sans' ... }` → `'Sora', sans-serif`; use `Playfair Display` for headings where appropriate.

**File**: `src/main/resources/static/update.css` — `body { font-family: 'DM Sans' }` → `'Sora'`; `Fraunces` heading usage → `'Playfair Display'`.

**File**: `src/main/resources/static/listProperty.css` — `--font-body: 'Plus Jakarta Sans'` → `'Sora'`; keep `--font-display: 'Playfair Display'` (already correct); tokens otherwise fine.

**Group C — Radius-only correction**

**File**: `src/main/resources/static/admin-style.css` — change `.btn` and `.tab` `border-radius: 7px` → `50px` (pill). Leave all colors, fonts, layout, `--r: 12px` cards untouched.

**Group D — Login theme + asset fix**

**File**: `src/main/resources/static/login-style.css`
1. `body { font-family: 'DM Sans' }` → `'Sora'`; `.sa-logo { font-family: 'DM Serif Display' }` → `'Playfair Display'`.
2. Replace flat dark `--bg: #121212` with a light theme (or keep a tasteful dark overlay layered on the working background image, not a flat fill).
3. `.page-overlay { background: url("image2.jpeg") ... }` → `url("login-image2.jpeg")`.
4. Buttons (`.sa-btn-primary`, `.sa-btn-secondary`) → `border-radius: 50px` pill; align greens to `hsl(180,67%,47%)`.

**Group E — Template `<head>` font links (design-only, no logic/route/JS change)**

Update the Google Fonts `<link>` in these templates to request `Sora:wght@400;500;600;700` + `Playfair+Display:wght@400;500;700` (replacing `Plus Jakarta Sans`/`Hanken Grotesk`/`DM Sans`/`Fraunces`):
- `templates/landlord/landlord-index.html`
- `templates/landlord/listProperty.html`
- `templates/landlord/manage-applications.html`
- `templates/landlord/my-property-reviews.html`
- `templates/application.html`
- `templates/logIn.html`
- `templates/manage-properties.html`
- `templates/update.html`

**Already aligned — no change (preserved):** `property-detail.html`, `register.html`, root `my-property-reviews.html`, `landlord/edit-property.html` (inline `Sora`/`Playfair`), and `student/student-dashboard.html` (isolated Tailwind theme, out of scope).

### Pre-existing path risks to flag (observed, verify during implementation)

- `manage-applications.html` links `/applications-style.css`, but the file is `Applications-style.css` (case mismatch — can 404 on case-sensitive servers).
- `my-property-reviews.html` (landlord) links `/css/landlord-reviews.css`, but the file lives at the static root (`/landlord-reviews.css`).

These are not the target bug but sit directly on the fix path; if a corrected stylesheet still does not load, resolve the reference (without changing routes/logic).

## Testing Strategy

### Validation Approach

Because this is a styling fix, "inputs" are rendered pages/elements and "outputs" are their resolved CSS (computed styles) and asset resolution. The strategy is two-phase: first capture the divergence on the **unfixed** code (counterexamples), then verify the fix produces design-system styling for buggy surfaces while leaving student pages and all behavior untouched. Automated checks use computed-style assertions (e.g. Playwright/JSDOM reading `getComputedStyle`) plus visual regression snapshots; manual cross-page visual review complements them.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the inconsistency BEFORE implementing the fix, and confirm the root-cause map (which files/tokens are wrong). If a page turns out already-correct (e.g. admin colors), refine the hypothesis.

**Test Plan**: Load each non-student page against the UNFIXED code and read computed styles for representative elements (body font, primary button `border-radius` and `background-color`, card `border-radius`, page background), plus check that the login background image request resolves. Record the deviations.

**Test Cases**:
1. **Landlord dashboard font/color/radius**: assert body uses `Sora` and a primary button is teal + `50px` (will fail — `Plus Jakarta Sans`, `#147592`, `8px`).
2. **Login background asset**: assert `.page-overlay` background image resolves to an existing file (will fail — `image2.jpeg` 404s).
3. **Login fonts/theme**: assert body `Sora`, logo `Playfair Display`, non-`#121212` background (will fail — `DM Sans`/`DM Serif`, dark).
4. **Applications/Update fonts**: assert body `Sora` (will fail — `DM Sans`).
5. **Admin button shape** (edge case): assert `.btn` radius `50px` (will fail — `7px`), while confirming admin colors/fonts already pass.
6. **Reviews fonts**: assert group title `Playfair Display`, body `Sora` (will fail — `Plus Jakarta Sans`/`Hanken Grotesk`).

**Expected Counterexamples**:
- Non-teal primary (`#147592`), purple secondary (`#441587`), non-pill buttons, `24px`/`8px`/`7px` radii, `DM Sans`/`Hanken Grotesk`/`Plus Jakarta Sans` fonts, unresolved `image2.jpeg`.
- Possible causes: divergent `:root` tokens, wrong `font-family` declarations, wrong font `<link>`, mistyped asset path.

### Fix Checking

**Goal**: Verify that for all elements where the bug condition holds, the fixed stylesheets produce the design-system styling.

**Pseudocode:**
```
FOR ALL element WHERE isBugCondition(element) DO
  applyFixedStylesheets()
  ASSERT element.fontFamily IN { 'Sora', 'Playfair Display' }
  ASSERT element.primaryColor == hsl(180,67%,47%)
  ASSERT NOT usesPurpleSecondary(element)
  IF element.role == ACTION_BUTTON THEN ASSERT element.borderRadius == 50px
  IF element.role == CARD          THEN ASSERT element.borderRadius == 12px
  ASSERT element.background != flatDark(#121212)
  IF element.backgroundImage IS REFERENCED THEN ASSERT assetExists(element.backgroundImage)
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed code produces the same result as the original — student-page visuals, JS behavior, DOM structure, routes, and Thymeleaf logic.

**Pseudocode:**
```
FOR ALL element WHERE NOT isBugCondition(element) DO
  ASSERT render_original(element) == render_fixed(element)   // computed style + markup
END FOR
FOR ALL jsInteraction DO
  ASSERT behavior_original(jsInteraction) == behavior_fixed(jsInteraction)
END FOR
FOR ALL route DO
  ASSERT response_original(route) == response_fixed(route)     // markup/data unchanged
END FOR
```

**Testing Approach**: Property-based / snapshot testing is well suited to preservation here because it can enumerate many elements and interactions across the input domain and catch unintended spillover:
- Generate/enumerate student-page elements and diff computed styles before vs after the fix (must be identical).
- Assert DOM structure (class lists, element counts, `th:*`-produced markup) is byte-identical, since JS selectors and server bindings depend on it.
- Exercise JS interactions and confirm identical outcomes.

**Test Plan**: Snapshot student pages, DOM structure, and JS-driven outcomes on the UNFIXED code, then re-run after the fix and assert no diff.

**Test Cases**:
1. **Student visual preservation**: snapshot `property-detail.html`, `register.html`, root `my-property-reviews.html` before/after — no visual diff.
2. **JS behavior preservation**: sidebar collapse, wizard steps, applications submit/cancel, login panel switch + password toggle behave identically.
3. **DOM/markup preservation**: class names, element hierarchy, and `th:*` output unchanged so selectors/bindings still resolve.
4. **Route/data preservation**: each controller route returns the same template and model data.

### Unit Tests

- Assert corrected `:root` token values in each remapped file (`landlord-style.css`, `Manage properties.css`) equal the canonical teal scale and radii.
- Assert `font-family` declarations resolve to `Sora`/`Playfair Display` in `application.css`, `update.css`, `login-style.css`, `landlord-reviews.css`, `listProperty.css`.
- Assert `admin-style.css` `.btn`/`.tab` `border-radius == 50px`.
- Assert `login-style.css` references `login-image2.jpeg` and no longer `image2.jpeg`.

### Property-Based Tests

- Enumerate action buttons across all fixed pages and assert every one has `border-radius: 50px` and a teal background.
- Enumerate cards/containers across fixed pages and assert `border-radius: 12px` with a teal-tinted shadow.
- Across all student-page elements, assert computed style is unchanged vs the pre-fix baseline (preservation).

### Integration Tests

- Navigate the full app (student → landlord → admin → login/register → applications → update → reviews) and visually confirm one consistent design system end to end.
- Load the login page and confirm the blurred `login-image2.jpeg` backdrop renders with a tasteful overlay.
- Confirm every corrected stylesheet and font `<link>` actually loads (no 404s), including the flagged `/applications-style.css` and `/css/landlord-reviews.css` path risks.
