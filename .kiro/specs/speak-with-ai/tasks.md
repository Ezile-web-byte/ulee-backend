# Implementation Plan: Speak with AI

## Overview

Convert the feature design into a series of prompts for a code-generation LLM that will implement each step with incremental progress. Make sure that each prompt builds on the previous prompts, and ends with wiring things together. There should be no hanging or orphaned code that isn't integrated into a previous step. Focus ONLY on tasks that involve writing, modifying, or testing code.

Implementation language: **JavaScript** for the widget's pure logic and DOM wiring (plain client-side JS, no build tooling), matching the design document's code samples. The widget is delivered as a Thymeleaf fragment (`templates/fragments/speak-with-ai.html`) included by every page template.

This feature has **no automated test suite** in this spec. It is verified **manually** by the user clicking through the widget in the browser as each stage lands. No `package.json`, Vitest, fast-check, or any other JS/MockMvc test tooling is introduced anywhere in this plan.

The plan builds the DOM-free logic module first, then the DOM wiring and styling, then the fragment itself, and finally rolls the fragment out to every page template in the application — split explicitly into the two Property_Context templates and the full enumerated list of General_Context templates.

## Tasks

- [x] 1. Implement core state-transition pure functions in `speak-with-ai-logic.js`
  - [x] 1.1 Implement `isBlank`, `appendMessage`, and `toggleOpen` in `src/main/resources/static/speak-with-ai-logic.js`, exposing the module via `window.SwaiLogic` and `module.exports`
    - _Requirements: 2.1, 2.2, 2.4, 2.5, 3.2, 3.3, 3.4, 3.6_

- [x] 2. Implement context-aware logic: `resolveContext` and `buildPlaceholderResponse`
  - [x] 2.1 Implement `resolveContext` in `speak-with-ai-logic.js` per the Property_Context/General_Context rules (requires truthy `propertyId` AND non-blank `propertyTitle` AND `isPropertyPage`)
    - _Requirements: 5.1, 5.2, 5.4, 5.5_
  - [x] 2.2 Implement `buildPlaceholderResponse` in `speak-with-ai-logic.js`, embedding the property title verbatim in Property_Context and never referencing a specific property in General_Context
    - _Requirements: 4.1, 4.2, 5.3, 5.4_

- [~] 3. Checkpoint - manually review the logic module
  - Manually review `isBlank`/`appendMessage`/`toggleOpen`/`resolveContext`/`buildPlaceholderResponse` for correctness before wiring them into the DOM. Ask the user if questions arise.

- [x] 4. Implement DOM wiring in `speak-with-ai.js`
  - [x] 4.1 Implement icon click/close-button/Escape handling and the property-title-in-header behavior, reading `data-property-id`/`data-property-title` off `#swai-root` and calling `SwaiLogic.resolveContext`/`toggleOpen`
    - _Requirements: 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 5.2_
  - [x] 4.2 Implement message submit handling (blank-guard, clear input, refocus), first-open greeting insertion, the `setTimeout`-based placeholder reply, and rendering of messages into `#swai-history` with sender-specific classes
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 4.1, 4.2, 4.3_

- [x] 5. Implement `speak-with-ai.css` with the hero design-token fallback chain
  - [x] 5.1 Implement scoped `.swai-root`/`#swai-icon`/`#swai-panel` styles in `src/main/resources/static/speak-with-ai.css` using the `--swai-*` variable chain that falls back through `--hero-*` tokens to the hard-coded hero-yellow defaults
    - _Requirements: 6.1, 6.2_

- [~] 6. Checkpoint - manually review the DOM wiring and CSS
  - Manually review the DOM wiring and stylesheet implementation for correctness before creating the Thymeleaf fragment that will host them. Ask the user if questions arise.

- [x] 7. Create the reusable Thymeleaf widget fragment
  - [x] 7.1 Implement `templates/fragments/speak-with-ai.html` with `th:fragment="speakWithAiWidget(propertyId, propertyTitle)"`, the icon/panel/history/form markup, and the `data-property-id`/`data-property-title` attributes, plus the usage/include comment block
    - _Requirements: 1.1, 5.1_

- [ ] 8. Roll out the widget include to the Property_Context templates
  - [x] 8.1 Add the fragment include (passing `${property.propertyID}`, `${property.title}`) plus the `speak-with-ai.css`/`speak-with-ai-logic.js`/`speak-with-ai.js` `<link>`/`<script>` tags to `templates/property-detail.html`
    - _Requirements: 1.1, 1.2, 5.1, 5.2, 5.3_
  - [x] 8.2 Add the fragment include (passing `${property.propertyID}`, `${property.title}`) plus the same `<link>`/`<script>` tags to `templates/vr-tour.html`
    - _Requirements: 1.1, 1.2, 5.1, 5.2, 5.3_

- [ ] 9. Roll out the widget include to every General_Context template (`null, null`)
  - [x] 9.1 Add the fragment include (`null, null`) plus the CSS/JS `<link>`/`<script>` tags to the root-level page templates: `templates/application.html`, `templates/logIn.html`, `templates/manage-properties.html`, `templates/my-applications.html`, `templates/my-property-reviews.html`, `templates/properties.html`, `templates/property-reviews.html`, `templates/register.html`, `templates/update.html`
    - _Requirements: 1.1, 1.2, 1.4, 5.4_
  - [x] 9.2 Add the fragment include (`null, null`) plus the CSS/JS tags to the landlord page templates: `templates/landlord/edit-property.html`, `templates/landlord/landlord-index.html`, `templates/landlord/listProperty.html`, `templates/landlord/manage-applications.html`, `templates/landlord/my-property-reviews.html` — excluding `templates/landlord/Review-section.html` and `templates/landlord/ReviewCard.html`, which are `th:fragment` includes rendered inside other pages, never as standalone pages, so adding the widget there would double-render it
    - _Requirements: 1.1, 1.2, 1.4, 5.4_
  - [x] 9.3 Add the fragment include (`null, null`) plus the CSS/JS tags to every admin page template: `templates/admin/admin-approved-properties.html`, `templates/admin/admin-edit-user.html`, `templates/admin/admin-index.html`, `templates/admin/admin-listing-details.html`, `templates/admin/admin-listing-not-found.html`, `templates/admin/admin-listings.html`, `templates/admin/admin-manage-users.html`, `templates/admin/admin-pending-listings.html`, `templates/admin/admin-reported-listing-detail.html`, `templates/admin/admin-reported-listings.html`, `templates/admin/admin-reviews.html`
    - _Requirements: 1.1, 1.2, 1.4, 5.4_
  - [x] 9.4 Add the fragment include (`null, null`) plus the CSS/JS tags to the remaining student page templates (other than the two Property_Context ones): `templates/student/student-dashboard.html`, `templates/student/write-review.html`
    - _Requirements: 1.1, 1.2, 1.4, 5.4_

- [~] 10. Final checkpoint - manually verify in browser that the widget opens/closes and messages send
  - Click through the icon on at least one Property_Context page (e.g. `/property/{id}`) and at least one General_Context page (e.g. student dashboard, login) to confirm: the icon opens/closes the panel, the greeting appears once on first open, the property title (or its absence) is correct for the page type, and messages send, render, and receive a placeholder reply. Ask the user if questions arise.

## Notes

- This feature has no automated test suite in this spec; all verification is manual, performed by the user clicking through the widget in the browser.
- No `package.json`, Vitest, fast-check, or any other JS/MockMvc test tooling is introduced anywhere in this plan.
- Excluded from the rollout because they are `th:fragment` includes, never standalone rendered pages: `templates/fragments/hero-nav.html`, `templates/landlord/Review-section.html`, `templates/landlord/ReviewCard.html`.
- Property_Context templates (`property-detail.html`, `vr-tour.html`) pass the real `propertyId`/`propertyTitle`; every other rendered page passes `null, null` and operates in General_Context.
- Each task references specific requirements for traceability.
- Checkpoints are manual, browser-based (or implementation-review) verification steps rather than automated test runs.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "5.1", "7.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "4.1"] },
    { "id": 3, "tasks": ["4.2"] },
    { "id": 4, "tasks": ["8.1", "8.2", "9.1", "9.2", "9.3", "9.4"] }
  ]
}
```
