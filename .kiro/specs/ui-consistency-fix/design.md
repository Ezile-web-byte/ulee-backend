# UI Consistency Fix — Bugfix Design

## Overview

Three team members built the student, landlord, and admin sections of ULEE independently, each inventing its own design language. The result is a visually fractured product: fonts, colors, button shapes, card radii, and shadows all differ from one surface to the next, the login page points at a background image that does not exist, and (as newly discovered) the root `properties.html` search-results page has no styling applied to it at all.

**Revised direction — hybrid two-track design system.** Rather than unifying every non-student page under one teal system, non-student pages now split into two tracks:

1. **Dashboard/Functional track** (unchanged from the original design): landlord pages, admin pages, applications, update, reviews, and property-detail. These continue to converge on the **student page design system** already established in `property-detail.html`, `register.html`'s prior form, and the student-facing token blocks:
   - **Fonts**: `Sora` (body) + `Playfair Display` (display/headings)
   - **Color**: teal scale anchored on `--primary: hsl(180, 67%, 47%)` (no purple secondary)
   - **Buttons**: pill-shaped, `border-radius: 50px`
   - **Cards**: `border-radius: 12px` with soft, teal-tinted shadows
   - **Background**: light `hsl(0, 0%, 99%)`

2. **Landing/Marketing track** (new): `properties.html` (root template, `GET /search` — currently unstyled), `logIn.html`, and `register.html` (currently on the teal/Sora/Playfair system, or in the case of `properties.html`, no system at all). These now adopt the **"bold hero" style** already implemented and proven in `student/student-dashboard.html`'s hero section, which is the canonical visual reference and remains untouched/out of scope. That style is:
   - **Fonts**: `Plus Jakarta Sans` (display) + `Hanken Grotesk` (body), loaded via Google Fonts (`Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Hanken+Grotesk:wght@400;500;600;700`)
   - **Imagery**: full-width background photo with a dark gradient overlay
   - **Typography on photo**: bold white type, with a yellow accent color (`#ffe170`, Tailwind `tertiary-fixed`) and dark text-on-yellow (`#221b00`, `on-tertiary-fixed`) for emphasis/CTAs
   - **Glass panels**: semi-transparent white panels (`rgba(255,255,255,.1)`) with `backdrop-filter: blur(...)` and a subtle white border (`1px solid rgba(255,255,255,.2)`), `16px` container radius, fully pill (`999px`/`50px`) buttons/chips inside
   - **CTAs**: yellow pill buttons (yellow background, dark text, fully rounded)

The fix remains **pure CSS, font-link (`<head>`), and template markup/styling work** — no controller routes, Thymeleaf data-binding logic, backend logic, or JavaScript behavior is touched. `properties.html`, `logIn.html`, and `register.html` are plain Thymeleaf/HTML + CSS (not Tailwind), so the Landing/Marketing track's fix translates the Tailwind reference tokens from `student-dashboard.html` into plain CSS custom properties/classes — either a small dedicated stylesheet (for `properties.html`, which has none today) or an inline `<style>` block (for `register.html`, which already uses one).

Two stylesheet **path mismatches** — `manage-applications.html` linking the wrong-case filename, and landlord `my-property-reviews.html` linking the wrong directory — are also fixed as part of this bug, since they sit directly on the Dashboard/Functional fix path and would otherwise cause a corrected stylesheet to still 404.

To reduce future drift on the Dashboard/Functional track, the design also recommends the shared token stylesheet (`ulee-design-tokens.css`) introduced previously, so that system has one source of truth going forward. The Landing/Marketing track is small enough (three pages) that it does not yet warrant its own shared stylesheet, but the token values below should be treated as its de facto source of truth.

## Glossary

- **Bug_Condition (C)**: A rendered page/element belongs to a non-student surface and either (a) belongs to the Dashboard/Functional track and deviates from the teal design system (wrong font, non-teal palette, non-pill buttons, non-12px cards), (b) belongs to the Landing/Marketing track and deviates from the Hero style (wrong font, no photo+overlay, no yellow accent/glass/pill treatment, or — for `properties.html` — no styling at all), or (c) references a stylesheet asset/path that does not resolve (broken login background image, or a case/path-mismatched `<link>`).
- **Property (P)**: After the fix, a Dashboard/Functional element renders with `Sora`/`Playfair Display`, the canonical teal token scale, pill (`50px`) buttons, and `12px` cards; a Landing/Marketing element renders with `Plus Jakarta Sans`/`Hanken Grotesk`, the yellow-accent/glass/pill hero treatment (and, where applicable, a full-width photo + dark overlay); and every referenced stylesheet/image asset resolves.
- **Preservation**: Behavior that must remain byte-for-byte unchanged — all JavaScript functionality, DOM structure/markup, controller routes, Thymeleaf template logic, and (except for `properties.html`/`register.html`, whose visuals are intentionally changing) existing page visuals.
- **Dashboard/Functional teal system**: The teal `Sora`/`Playfair Display` design system captured canonically in `property-detail.html` and the `:root` token blocks using `hsl(180, 67%, 47%)`. Governs landlord, admin, applications, update, reviews, and property-detail pages.
- **Landing/Marketing Hero style**: The bold-hero design system — `Plus Jakarta Sans`/`Hanken Grotesk` fonts, full-width photo + dark overlay, yellow accent (`#ffe170`) CTAs, and translucent/backdrop-blur "glass panel" UI — already implemented in `student/student-dashboard.html`'s hero section and now the canonical reference for `properties.html`, `logIn.html`, and `register.html`.
- **Design Tokens**: The `:root` CSS custom properties (colors, fonts, radii, shadows) that a page's rules resolve against. Each track has its own token set (see Fix Implementation).
- **Academic Vitality**: The name of the divergent landlord design system (`--primary: #147592`, `--secondary: #441587`, `Plus Jakarta Sans` / `Hanken Grotesk`, `--radius-sm: 8px` / `--radius-lg: 24px`) that must be remapped onto the canonical Dashboard/Functional tokens. (Unrelated to the Landing/Marketing track's use of the same font names — the fonts overlap by coincidence, but the color/shape language differs completely.)

## Bug Details

### Bug Condition

The bug manifests whenever a user views a non-student page and that page's track-appropriate styling is missing or wrong. For **Dashboard/Functional** pages, the stylesheet either (a) declares the wrong font family, (b) resolves to a non-canonical color palette, (c) renders rectangular buttons instead of pills, (d) uses non-standard card radii/shadows, or (e) references a stylesheet via a mismatched path/case so it 404s. For **Landing/Marketing** pages, the page either (a) has no styling at all (`properties.html` today), (b) declares the wrong font family (`Sora`/`Playfair Display` on `register.html`, `DM Sans`/`DM Serif Display` on the login page) instead of `Plus Jakarta Sans`/`Hanken Grotesk`, (c) lacks the yellow-accent/glass-panel/pill treatment, or (d) — for the login page specifically — points `background: url(...)` at `image2.jpeg`, a file that does not exist (the real asset is `login-image2.jpeg`).

**Formal Specification:**
```
FUNCTION isBugCondition(element)
  INPUT: element — a rendered page element with resolved style + its owning surface
  OUTPUT: boolean

  IF element.surface == STUDENT_DASHBOARD_HERO THEN
    RETURN false                      // canonical Hero reference, never buggy
  IF element.surface == OTHER_STUDENT_PAGE THEN
    RETURN false                      // other student pages are the Dashboard/Functional reference

  track := classifyTrack(element.surface)
  // DASHBOARD  = landlord, admin, applications, update, reviews, property-detail
  // LANDING    = properties.html, logIn.html, register.html

  IF track == DASHBOARD THEN
    stylingDeviates :=
          element.fontFamily NOT IN { 'Sora', 'Playfair Display' }
       OR element.primaryColor != hsl(180,67%,47%)
       OR usesPurpleSecondary(element)
       OR (element.role == ACTION_BUTTON AND element.borderRadius != 50px)
       OR (element.role == CARD          AND element.borderRadius != 12px)
       OR element.background == flatDark(#121212)

    linkPathBroken :=
          element.stylesheetHref IS REFERENCED
       AND NOT assetExistsAtPath(element.stylesheetHref)   // e.g. case/dir mismatch

    RETURN stylingDeviates OR linkPathBroken

  IF track == LANDING THEN
    unstyled := hasNoStylesheetOrStyleBlockAtAll(element.surface)   // properties.html today

    stylingDeviates :=
          element.fontFamily NOT IN { 'Plus Jakarta Sans', 'Hanken Grotesk' }
       OR (element.role == HERO_SURFACE AND NOT hasPhotoWithDarkOverlay(element))
       OR (element.role == CTA_BUTTON   AND NOT usesYellowAccent(element, '#ffe170', '#221b00'))
       OR (element.role == GLASS_PANEL  AND NOT hasGlassStyling(element))   // translucent + blur + border
       OR (element.role IN { CTA_BUTTON, GLASS_CHIP } AND element.borderRadius NOT IN { '999px', '50px' })

    brokenAsset :=
          element.backgroundImage IS REFERENCED
       AND NOT assetExists(element.backgroundImage)          // login image2.jpeg

    RETURN unstyled OR stylingDeviates OR brokenAsset
END FUNCTION
```

### Examples

**Dashboard/Functional track**
- **Landlord dashboard** (`landlord-index.html` → `landlord-style.css`): headings render in `Plus Jakarta Sans`, primary buttons are `#147592` with `border-radius: 8px`, cards use `24px` radius and `rgba(20,117,146,.08)` shadows. Expected: `Playfair Display` headings, `hsl(180,67%,47%)` pill buttons (`50px`), `12px` cards with soft teal shadows.
- **Admin dashboard** (`admin-index.html` → `admin-style.css`): already uses `Sora` and the correct teal, but `.btn`/`.tab` use `border-radius: 7px`. Expected: pill `50px` action buttons.
- **Applications page** (`application.html` → `application.css`): teal tokens are already correct, but body font is `DM Sans`. Expected: `Sora` body font. (Edge case: only the font declaration is wrong; colors/radii already match.)
- **Update property page** (`update.html` → `update.css`): teal tokens correct, but fonts are `DM Sans` / `Fraunces`. Expected: `Sora` / `Playfair Display`.
- **Reviews page** (`my-property-reviews.html` landlord template → `landlord-reviews.css`): group titles in `Plus Jakarta Sans`, body in `Hanken Grotesk`. Expected: `Playfair Display` titles, `Sora` body.
- **Manage-applications stylesheet path** (`manage-applications.html`): links `/applications-style.css` (lowercase) but the file on disk is `Applications-style.css`, so the link 404s on case-sensitive servers. Expected: link resolves to the actual file.
- **Landlord reviews stylesheet path** (`my-property-reviews.html`): links `/css/landlord-reviews.css` but the file is served from the static root (`/landlord-reviews.css`). Expected: link resolves.

**Landing/Marketing track**
- **`properties.html`** (root template, `GET /search`): no linked stylesheet, no inline `<style>`, no custom classes, no font-family — plain default browser rendering (`<h1>`, unstyled `<form>`, unstyled result rows). Expected: `Plus Jakarta Sans`/`Hanken Grotesk` fonts, yellow-accent styling on the filter/search controls, and a styled results list consistent with the Hero visual language (glass-style filter bar, pill inputs/buttons, styled property cards) — a full-bleed photo hero is not required since this is a results page, not the true landing hero.
- **Login page** (`logIn.html` → `login-style.css`): body font is `DM Sans`, `.sa-logo` is `DM Serif Display`, background is a flat `#121212`, and `.page-overlay` requests `url("image2.jpeg")` which 404s. Expected: `Plus Jakarta Sans`/`Hanken Grotesk` fonts, a full-width background photo with a dark overlay (from a corrected `url("login-image2.jpeg")`), a glass-panel card, and yellow pill CTA buttons — replacing both the teal-oriented button styling and the broken image reference.
- **Register page** (`register.html`): currently uses `Sora`/`Playfair Display` fonts and teal (`hsl(180,67%,47%)`) pill buttons/toggle — already has a background photo + dark overlay structure (`linear-gradient(rgba(0,20,20,.45), rgba(0,20,20,.45)), url(...)`), which is structurally correct for the Hero pattern but needs its fonts and color tokens (teal → yellow accent) shifted to match the Landing/Marketing track. Expected: `Plus Jakarta Sans`/`Hanken Grotesk` fonts, yellow (`#ffe170`)/dark-on-yellow (`#221b00`) accents on the active role toggle and submit button, and a glass-panel treatment for the `.card` container instead of the current solid white card.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Student pages other than the ones explicitly re-scoped below (`property-detail.html`, root `my-property-reviews.html`, `my-applications.html`, `student/student-dashboard.html`) must render exactly as they do today.
- `student/student-dashboard.html`'s hero section specifically must remain unchanged — it is an isolated Tailwind theme, out of scope, and now additionally serves as the canonical visual reference for the Landing/Marketing track.
- All JavaScript behavior must be identical: sidebar collapse (`admin-script.js`, `landlord-script.js`), the list-property wizard (`listProperty.js`), applications actions (`application.js`), update flows (`update.js`), login/register panel switching and password toggle (`login-script.js`), and `register.html`'s inline `setRole()` script.
- DOM structure and Thymeleaf markup (class names, element hierarchy, `th:*` attributes, form actions, input `name`/`id` attributes) must be preserved so JS selectors and server bindings keep working — including on `properties.html` and `register.html`, whose *visuals* are changing (see below).
- Controller routes and the data rendered into each template must be unchanged.

**Scope (revised):**
`properties.html` and `register.html` are **no longer** part of the frozen-visual preservation set. Their prior "already matches a design system" classification was based on a mistaken assumption (`properties.html` was actually unstyled; `register.html` was on the wrong track's tokens). Both are now explicitly in the Landing/Marketing track's expected-behavior scope (2.12/2.13) and their *visual* output is expected to change substantially. What must still be preserved for these two pages is their **interactive contract**: JavaScript hooks (`login-script.js` panel-switch/password-toggle behavior is unaffected since it lives on the login page, not these two; `register.html`'s `setRole` script), form `action` attributes (`/register`, `/search`), and input `name`/`id` attributes — none of that changes even though the CSS around it does.

All other inputs that do NOT involve non-student page styling must be completely unaffected by this fix. This includes:
- Any interaction handled by JavaScript (form submissions, filtering, modals, tabs, sidebar toggles).
- Any server round-trip (route resolution, model attributes, Thymeleaf iteration/conditionals).
- The DOM contract every script and template depends on (only CSS property values, font `<link>`s, and — for the Landing/Marketing track — added classes/style blocks change; no class *renames* of existing JS-bound elements, no removal of existing markup that scripts depend on).

**Note:** The concrete "correct" styling for each track is defined in the Correctness Properties section (Property 1 for Dashboard/Functional, Property 4 for Landing/Marketing). This section defines what must NOT change.

## Hypothesized Root Cause

Based on the bug analysis, the divergence has these root causes:

1. **Independent design systems, no shared tokens (Dashboard/Functional track)**: Each author defined their own `:root` block. Student/admin/applications/update pages use the canonical teal scale (`hsl(180,67%,47%)`); landlord pages use a separate "Academic Vitality" palette (`--primary: #147592`, `--secondary: #441587`).
   - `landlord-style.css`, `Manage properties.css`: `Plus Jakarta Sans` + `Hanken Grotesk`, `--radius-sm: 8px`, `--radius-lg: 24px`, teal+purple.
   - `landlord-reviews.css`: hard-coded `Plus Jakarta Sans` / `Hanken Grotesk` font families (no tokens).

2. **Wrong font families with correct colors (Dashboard/Functional track)**: `application.css` and `update.css` already carry the right teal tokens and `12px` radius but declare `DM Sans` (and `Fraunces` in update). Only the `font-family` declarations and the page `<head>` font `<link>`s are wrong.

3. **Non-pill buttons in an otherwise-correct file**: `admin-style.css` is on-palette and on-font (`Sora`) but uses `border-radius: 7px` on `.btn` and `.tab`, breaking the pill convention.

4. **`properties.html` was never assigned a stylesheet**: unlike every other template, the root search-results template has no `<link>` and no `<style>` block at all — it appears the page was left as a functional scaffold and never handed to a designer, rather than being built on any (even wrong) design system.

5. **`register.html` and the login page predate the Hero style, or were built on a different reference**: `register.html` was built against the teal Dashboard/Functional tokens (an earlier or borrowed reference), and the login page (`login-style.css`) was built on a third, unrelated theme (`DM Sans` / `DM Serif Display`, flat dark background). Neither was updated when the bold-hero pattern was later established in `student/student-dashboard.html` and designated as the Landing/Marketing reference.

6. **Login page has a broken asset reference independent of the theme mismatch**: `.page-overlay { background: url("image2.jpeg") ... }` — the deployed asset is `login-image2.jpeg`, so the reference resolves to nothing regardless of which theme the page uses.

7. **Two template `<link>` hrefs don't match the actual static file path/case**: `manage-applications.html` requests `/applications-style.css` (file is `Applications-style.css`), and landlord `my-property-reviews.html` requests `/css/landlord-reviews.css` (file is at the static root). These are independent copy/typo errors, not token or font mistakes, but they sit on the fix path since a corrected stylesheet still won't load if the link itself is wrong.

8. **Font `<link>` tags in templates load the wrong families for their track**: even after CSS is corrected, several Dashboard/Functional templates only request `Plus Jakarta Sans`/`Hanken Grotesk`/`DM Sans`/`Fraunces` from Google Fonts (need `Sora`/`Playfair Display`), while the Landing/Marketing templates (`logIn.html`, `register.html`) request `DM Sans`/`Sora` families (need `Plus Jakarta Sans`/`Hanken Grotesk`) — in both cases the target font would fall back to a system default unless the `<link>` is updated too.

## Correctness Properties

Property 1: Bug Condition - Dashboard/Functional Design System Applied

_For any_ Dashboard/Functional-track page element (landlord, admin, applications, update, reviews, property-detail) where the bug condition holds (isBugCondition returns true due to styling deviation or a broken stylesheet path), the fixed stylesheets and template `<link>`s SHALL render that element using `Sora` for body text and `Playfair Display` for display/heading text, the canonical teal token scale (`--primary: hsl(180,67%,47%)`, `--primary-dark: hsl(180,67%,36%)`, `--primary-deeper: hsl(180,67%,28%)`) with no purple secondary, action buttons with `border-radius: 50px`, cards with `border-radius: 12px` and soft teal-tinted shadows, a light background consistent with the rest of the app, and every stylesheet reference for that track SHALL resolve without a 404 (including the corrected `/Applications-style.css` and static-root `/landlord-reviews.css` paths).

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.8, 2.9, 2.10, 2.11, 2.14, 2.15**

Property 2: Bug Condition - Login Background Image Reference Resolves

_For any_ load of the login page where the bug condition holds (isBugCondition returns true due to the broken `image2.jpeg` reference), the fixed `login-style.css` SHALL reference `url("login-image2.jpeg")`, an asset that exists at the served static path, so the full-width background photo and its dark overlay render.

**Validates: Requirements 2.7**

Property 3: Preservation - Student Pages, Behavior, and Structure Unchanged

_For any_ input where the bug condition does NOT hold (isBugCondition returns false) — i.e. student-page elements other than the Landing/Marketing-rescoped pages, JavaScript-driven interactions, DOM structure, controller routes, and Thymeleaf logic — the fixed code SHALL produce exactly the same result as the original code, preserving all visual output on unaffected student pages (including the `student/student-dashboard.html` hero reference) and all functional behavior across the application. For `properties.html` and `register.html` specifically, the fixed code SHALL preserve their JavaScript hooks, form `action` attributes, and input `name`/`id` attributes exactly, even though their visual styling changes substantially under Property 4.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8**

Property 4: Bug Condition - Landing/Marketing Hero Style Applied

_For any_ Landing/Marketing-track page element (`properties.html`, `logIn.html`, `register.html`) where the bug condition holds (isBugCondition returns true due to missing styling, wrong fonts, or a missing hero/glass/yellow-accent treatment), the fixed markup and stylesheets SHALL render that element using `Plus Jakarta Sans` for display text and `Hanken Grotesk` for body text, a yellow accent (`#ffe170`) with dark text-on-yellow (`#221b00`) for CTAs/emphasis, pill-shaped (`999px`/`50px`) buttons and glass-panel chips (translucent white background, backdrop-blur, subtle white border, `16px` container radius), and — where the element is a hero/background surface — a full-width background photo with a dark gradient overlay, consistent with the reference hero section in `student/student-dashboard.html`.

**Validates: Requirements 2.5, 2.6, 2.12, 2.13**

## Fix Implementation

### Dashboard/Functional Track — Canonical Design Tokens (target values)

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

### Landing/Marketing Track — Hero Reference Tokens (target values)

Translated from the Tailwind config in `student/student-dashboard.html` into plain CSS custom properties, since `properties.html`, `logIn.html`, and `register.html` are plain HTML/Thymeleaf + CSS, not Tailwind:

```
--hero-font-display:    'Plus Jakarta Sans', sans-serif;
--hero-font-body:       'Hanken Grotesk', sans-serif;
--hero-yellow:          #ffe170;              /* tertiary-fixed */
--hero-on-yellow:       #221b00;              /* on-tertiary-fixed */
--hero-primary:         #005b74;              /* primary, used in overlay gradients */
--hero-overlay-gradient: linear-gradient(to right, rgba(0,91,116,.8), transparent);
                          /* or a simple dark scrim: linear-gradient(rgba(0,20,20,.45), rgba(0,20,20,.45)) */
--hero-glass-bg:        rgba(255,255,255,0.1);
--hero-glass-border:    1px solid rgba(255,255,255,0.2);
--hero-glass-blur:      blur(24px);
--hero-radius-glass:    16px;    /* glass panel container */
--hero-radius-pill:     999px;   /* buttons / chips inside the glass panel */
```

Google Fonts link for this track:
```
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Hanken+Grotesk:wght@400;500;600;700&display=swap" rel="stylesheet" />
```

### Recommended shared-CSS approach (Dashboard/Functional track)

Introduce a new **`src/main/resources/static/ulee-design-tokens.css`** containing the canonical `:root` tokens plus a small set of base primitives (`.pill-btn`, card base, body font). Non-student Dashboard/Functional pages link it **before** their page-specific stylesheet, so the tokens have one source of truth and future pages inherit the system by default. Because every existing page CSS already redefines `:root`, the immediate low-risk fix still normalizes tokens in-place within each file; the shared file is the strategic target that page CSS can progressively delegate to. The Landing/Marketing track is only three pages and does not yet warrant an equivalent shared file — the token block above is its de facto reference.

### Changes required (per file)

**Group A — Remap "Academic Vitality" tokens onto the design system** *(implemented — unchanged by this revision)*

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

**Group B — Font-only corrections (tokens already correct)** *(implemented — unchanged by this revision)*

**File**: `src/main/resources/static/application.css` — `body { font-family: 'DM Sans' ... }` → `'Sora', sans-serif`; use `Playfair Display` for headings where appropriate.

**File**: `src/main/resources/static/update.css` — `body { font-family: 'DM Sans' }` → `'Sora'`; `Fraunces` heading usage → `'Playfair Display'`.

**File**: `src/main/resources/static/listProperty.css` — `--font-body: 'Plus Jakarta Sans'` → `'Sora'`; keep `--font-display: 'Playfair Display'` (already correct); tokens otherwise fine.

**Group C — Radius-only correction** *(implemented — unchanged by this revision)*

**File**: `src/main/resources/static/admin-style.css` — change `.btn` and `.tab` `border-radius: 7px` → `50px` (pill). Leave all colors, fonts, layout, `--r: 12px` cards untouched.

**Group D — Login page: Landing/Marketing Hero style + asset fix** *(revised — was previously "remap login to teal"; now targets the Hero style instead)*

**File**: `templates/logIn.html`
1. Change the Google Fonts `<link>` from `family=DM+Serif+Display&family=DM+Sans:wght@400;500` to the Landing/Marketing Hero fonts: `family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Hanken+Grotesk:wght@400;500;600;700`.

**File**: `src/main/resources/static/login-style.css`
1. `body { font-family: 'DM Sans' }` → `'Hanken Grotesk', sans-serif`; `.sa-logo { font-family: 'DM Serif Display' }` → `'Plus Jakarta Sans', sans-serif` (bold weight for the display treatment).
2. Replace the flat dark `--bg: #121212` fill with a full-width background photo + dark gradient overlay (the Hero pattern: `background-image: url(...)` plus `linear-gradient(to right, rgba(0,91,116,.8), transparent)` or an equivalent dark scrim), rather than either the old flat teal-light theme or a flat dark fill.
3. `.page-overlay { background: url("image2.jpeg") ... }` → `url("login-image2.jpeg")`, now composed as part of the photo + overlay treatment (the corrected asset path is unchanged from before; only the surrounding visual theme changes).
4. `.sa-card` adopts the glass-panel treatment: `background: rgba(255,255,255,.1)`, `backdrop-filter: blur(24px)`, `border: 1px solid rgba(255,255,255,.2)`, `border-radius: 16px` — replacing the previous opaque card so it reads correctly against the new photo background.
5. Buttons (`.sa-btn-primary`, `.sa-btn-secondary`) → yellow pill CTAs: `.sa-btn-primary` gets `background: #ffe170; color: #221b00; border-radius: 999px`; `.sa-btn-secondary` becomes a lighter glass/pill outline style consistent with the new theme.
6. Preserve the centered, responsive mobile login card layout skeleton (Req 3.5) and all `login-script.js` hooks (panel switching, password toggle) — only fonts/colors/background/glass styling change, not structure or behavior.

**Group E — Template `<head>` font links, Dashboard/Functional track only** *(revised — `logIn.html` removed from this list; it now belongs to Group D above)*

Update the Google Fonts `<link>` in these Dashboard/Functional templates to request `Sora:wght@400;500;600;700` + `Playfair+Display:wght@400;500;700` (replacing `Plus Jakarta Sans`/`Hanken Grotesk`/`DM Sans`/`Fraunces`):
- `templates/landlord/landlord-index.html`
- `templates/landlord/listProperty.html`
- `templates/landlord/manage-applications.html`
- `templates/landlord/my-property-reviews.html`
- `templates/application.html`
- `templates/manage-properties.html`
- `templates/update.html`

Change ONLY the font `<link>` — no `th:*` attributes, class names, element hierarchy, form actions, or routes.

**Already aligned — no change (preserved):** `property-detail.html`, root `my-property-reviews.html`, `landlord/edit-property.html` (inline `Sora`/`Playfair`), and `student/student-dashboard.html` (isolated Tailwind theme, out of scope, and now the Hero reference).

**Group F — Landing/Marketing track: `properties.html` and `register.html`** *(new)*

**File**: `templates/properties.html` (currently has no `<head>` styling of any kind)
1. Add the Landing/Marketing Google Fonts `<link>` (`Plus+Jakarta+Sans` + `Hanken+Grotesk`).
2. Add a new stylesheet (e.g. `src/main/resources/static/properties-style.css`, linked from the `<head>`) — or an inline `<style>` block, consistent with how `register.html` uses one — implementing: `Plus Jakarta Sans` for the page heading, `Hanken Grotesk` for body text, yellow-accent (`#ffe170`/`#221b00`) styling on the filter button/active filter state, pill-shaped inputs and the filter button (`border-radius: 999px`/`50px`), and card-style presentation for each result (rounded container, subtle shadow) echoing the glass-panel visual language. A full-bleed photo hero is not required — this is a results-listing page, not the true landing hero — so layout (list vs. grid of result cards) is left to implementation judgment as long as the font/color/pill/glass language is applied and the page is no longer unstyled.
3. Do not alter the `th:each` iteration over `${properties}`, the `action="/search"` form target, or the `minBedrooms`/`maxRent` input `name` attributes — only add classes/wrapper markup needed for the new styling.

**File**: `templates/register.html`
1. Change the Google Fonts `<link>` from `Sora:wght@400;500;600;700&family=Playfair+Display:wght@700` to the Landing/Marketing Hero fonts (`Plus+Jakarta+Sans` + `Hanken+Grotesk`).
2. Rework the existing inline `<style>` block's tokens: swap `--primary`/`--primary-dark` teal values for the yellow accent (`#ffe170` background / `#221b00` text) on CTA-style elements (`.role-btn.active`, `.submit-btn`), and swap `font-family: 'Sora'`/`'Playfair Display'` declarations for `'Hanken Grotesk'`/`'Plus Jakarta Sans'` respectively.
3. `.card` moves from a solid white opaque panel to the glass-panel treatment (`background: rgba(255,255,255,.1)`, `backdrop-filter: blur(24px)`, `border: 1px solid rgba(255,255,255,.2)`, `border-radius: 16px`) so it reads correctly against the existing background photo + overlay, which is already structurally correct and does not need to change.
4. Do not alter `setRole()` JS, the `action="/register"` form target, or any input `name`/`id` attributes (Req 3.7).

**Group G — Stylesheet path-mismatch fixes** *(new; promoted from prior "risks to flag" language)*

**File**: `templates/landlord/manage-applications.html`
1. Change `<link rel="stylesheet" href="/applications-style.css">` → `href="/Applications-style.css"` to match the actual file's casing on disk. Do not rename the CSS file itself, since it may already be referenced correctly elsewhere.

**File**: `templates/landlord/my-property-reviews.html`
1. Change `<link rel="stylesheet" href="/css/landlord-reviews.css">` → `href="/landlord-reviews.css"`, since the file is served from the static root, not a `/css/` subdirectory.

These are independent of the font/token/theme fixes in Groups A–F; they only correct a `<link href>` so an already-being-corrected stylesheet actually loads. No routes or backend logic are touched.

## Testing Strategy

### Validation Approach

Because this is a styling fix, "inputs" are rendered pages/elements and "outputs" are their resolved CSS (computed styles) and asset/stylesheet-path resolution. The strategy is two-phase: first capture the divergence on the **unfixed** code (counterexamples) for both tracks, then verify the fix produces the correct track-specific styling for buggy surfaces while leaving unaffected student pages and all behavior untouched. Automated checks use computed-style assertions (e.g. Playwright/JSDOM reading `getComputedStyle`) plus link/asset resolution checks (HTTP status or file-existence checks) and visual regression snapshots; manual cross-page visual review complements them.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the inconsistency BEFORE implementing the fix, and confirm the root-cause map for both tracks (which files/tokens are wrong, which links are unstyled or mismatched). If a page turns out already-correct, refine the hypothesis.

**Test Plan**: Load each non-student page against the UNFIXED code and read computed styles / stylesheet values / link resolution for representative elements. Record the deviations.

**Test Cases (Dashboard/Functional track)**:
1. **Landlord dashboard font/color/radius**: assert body uses `Sora` and a primary button is teal + `50px` (will fail — `Plus Jakarta Sans`, `#147592`, `8px`).
2. **Applications/Update fonts**: assert body `Sora` (will fail — `DM Sans`).
3. **Admin button shape** (edge case): assert `.btn` radius `50px` (will fail — `7px`), while confirming admin colors/fonts already pass.
4. **Reviews fonts**: assert group title `Playfair Display`, body `Sora` (will fail — `Plus Jakarta Sans`/`Hanken Grotesk`).
5. **Manage-applications stylesheet link**: assert `/applications-style.css` resolves (will fail — actual file is `Applications-style.css`).
6. **Landlord reviews stylesheet link**: assert `/css/landlord-reviews.css` resolves (will fail — actual file is at `/landlord-reviews.css`).

**Test Cases (Landing/Marketing track)**:
7. **`properties.html` has no styling**: assert the page has at least one linked stylesheet or `<style>` block and a non-default `font-family` (will fail — currently zero styling of any kind).
8. **`register.html` fonts/accent**: assert `Plus Jakarta Sans`/`Hanken Grotesk` fonts and a yellow (`#ffe170`) accent on the active toggle/submit button (will fail — currently `Sora`/`Playfair Display` + teal).
9. **Login fonts/theme/asset**: assert body `Hanken Grotesk`, logo `Plus Jakarta Sans`, a background photo (not a flat fill), and `.page-overlay` background image resolving to an existing file (will fail — `DM Sans`/`DM Serif Display`, flat `#121212`, `image2.jpeg` 404s).

**Expected Counterexamples**:
- Non-teal primary (`#147592`), purple secondary (`#441587`), non-pill buttons, `24px`/`8px`/`7px` radii, `DM Sans`/`Hanken Grotesk`/`Plus Jakarta Sans` fonts on Dashboard/Functional pages, and two 404ing stylesheet `<link>`s.
- Zero styling on `properties.html`; teal tokens/fonts instead of yellow/hero fonts on `register.html`; unresolved `image2.jpeg` and dark-theme mismatch on the login page.
- Possible causes: divergent `:root` tokens, wrong `font-family` declarations, wrong font `<link>`, mistyped asset/stylesheet paths, a page never assigned any stylesheet, pages built against the wrong track's reference.

### Fix Checking

**Goal**: Verify that for all elements where the bug condition holds, the fixed stylesheets/markup produce the track-appropriate styling.

**Pseudocode:**
```
FOR ALL element WHERE isBugCondition(element) DO
  applyFixedStylesheets()
  track := classifyTrack(element.surface)

  IF track == DASHBOARD THEN
    ASSERT element.fontFamily IN { 'Sora', 'Playfair Display' }
    ASSERT element.primaryColor == hsl(180,67%,47%)
    ASSERT NOT usesPurpleSecondary(element)
    IF element.role == ACTION_BUTTON THEN ASSERT element.borderRadius == 50px
    IF element.role == CARD          THEN ASSERT element.borderRadius == 12px
    ASSERT element.background != flatDark(#121212)
    IF element.stylesheetHref IS REFERENCED THEN ASSERT assetExistsAtPath(element.stylesheetHref)

  IF track == LANDING THEN
    ASSERT element.fontFamily IN { 'Plus Jakarta Sans', 'Hanken Grotesk' }
    IF element.role == HERO_SURFACE THEN ASSERT hasPhotoWithDarkOverlay(element)
    IF element.role == CTA_BUTTON   THEN ASSERT usesYellowAccent(element, '#ffe170', '#221b00')
    IF element.role == GLASS_PANEL  THEN ASSERT hasGlassStyling(element)
    IF element.role IN { CTA_BUTTON, GLASS_CHIP } THEN ASSERT element.borderRadius IN { '999px', '50px' }

  IF element.backgroundImage IS REFERENCED THEN ASSERT assetExists(element.backgroundImage)
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed code produces the same result as the original — unaffected student-page visuals, JS behavior, DOM structure, routes, and Thymeleaf logic; and for `properties.html`/`register.html` specifically, that their interactive contract (JS hooks, form actions, input names/ids) is preserved even though their visuals change.

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
FOR ALL interactiveHook IN properties_html_and_register_html DO
  // visuals are expected to differ (Property 4); only the interactive contract is asserted
  ASSERT formAction_original(interactiveHook) == formAction_fixed(interactiveHook)
  ASSERT inputNamesAndIds_original(interactiveHook) == inputNamesAndIds_fixed(interactiveHook)
  ASSERT jsHookBehavior_original(interactiveHook) == jsHookBehavior_fixed(interactiveHook)
END FOR
```

**Testing Approach**: Property-based / snapshot testing is well suited to preservation here because it can enumerate many elements and interactions across the input domain and catch unintended spillover:
- Generate/enumerate unaffected student-page elements and diff computed styles before vs after the fix (must be identical).
- Assert DOM structure (class lists, element counts, `th:*`-produced markup) is byte-identical, since JS selectors and server bindings depend on it — including on `properties.html`/`register.html`, where only styling classes/wrappers may be added, not existing bound elements removed or renamed.
- Exercise JS interactions and confirm identical outcomes.

**Test Plan**: Snapshot unaffected student pages, DOM structure, and JS-driven outcomes on the UNFIXED code, then re-run after the fix and assert no diff. Separately snapshot the *interactive contract only* (not visuals) for `properties.html`/`register.html`.

**Test Cases**:
1. **Student visual preservation**: snapshot `property-detail.html`, root `my-property-reviews.html`, `student/student-dashboard.html` before/after — no visual diff.
2. **JS behavior preservation**: sidebar collapse, wizard steps, applications submit/cancel, login panel switch + password toggle, `register.html`'s `setRole` toggle all behave identically.
3. **DOM/markup preservation**: class names, element hierarchy, and `th:*` output unchanged so selectors/bindings still resolve — including confirming `properties.html`'s `th:each`/form and `register.html`'s form/inputs are structurally intact despite new styling.
4. **Route/data preservation**: each controller route returns the same template and model data.
5. **Path-fix isolation**: confirm the `manage-applications.html` and landlord `my-property-reviews.html` link-path fixes change only the `href` attribute, with no other markup/route change.

### Unit Tests

- Assert corrected `:root` token values in each remapped Dashboard/Functional file equal the canonical teal scale and radii.
- Assert `font-family` declarations resolve to `Sora`/`Playfair Display` in Dashboard/Functional files, and to `Plus Jakarta Sans`/`Hanken Grotesk` in Landing/Marketing files (`login-style.css`, `properties.html`'s new stylesheet/style block, `register.html`'s inline style block).
- Assert `admin-style.css` `.btn`/`.tab` `border-radius == 50px`.
- Assert `login-style.css` references `login-image2.jpeg` and no longer `image2.jpeg`.
- Assert `manage-applications.html` and landlord `my-property-reviews.html` `<link href>`s resolve to files that exist at those exact paths.
- Assert `properties.html` has at least one stylesheet reference or `<style>` block (no longer unstyled).

### Property-Based Tests

- Enumerate action buttons/CTAs across all fixed Dashboard/Functional pages and assert every one has `border-radius: 50px` and a teal background.
- Enumerate cards/containers across fixed Dashboard/Functional pages and assert `border-radius: 12px` with a teal-tinted shadow.
- Enumerate CTA buttons/glass chips across the fixed Landing/Marketing pages and assert `border-radius` is `999px`/`50px` and the yellow accent (`#ffe170`/`#221b00`) is applied.
- Across all unaffected student-page elements, assert computed style is unchanged vs the pre-fix baseline (preservation).
- Across `properties.html`/`register.html` interactive elements, assert form actions and input name/id attributes are unchanged vs the pre-fix baseline, independent of the styling diff.

### Integration Tests

- Navigate the full app (student → landlord → admin → login/register → applications → update → reviews → properties search) and visually confirm the Dashboard/Functional pages share one consistent teal system, and the Landing/Marketing pages (`properties.html`, login, register) share one consistent hero/yellow system.
- Load the login page and confirm the full-width `login-image2.jpeg` backdrop with dark overlay renders, with a yellow-CTA glass-panel login card on top.
- Load `properties.html` via `GET /search` and confirm it is no longer unstyled — fonts, yellow accents, and pill/glass styling are visibly applied to the filter form and result cards.
- Confirm every corrected stylesheet and font `<link>` actually loads (no 404s), including `/Applications-style.css` and `/landlord-reviews.css`.
