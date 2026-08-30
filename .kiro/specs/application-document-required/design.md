# Application Document Required Bugfix Design

## Overview

Students can currently submit a property application without attaching any supporting document. This is a regression: the flow was designed so a document had to accompany an application, and that enforcement is no longer happening.

Grounded in the code, the defect lives in `PropertyController.applyToProperty` (`@PostMapping("/apply/{propertyId}")`). The handler accepts only `propertyId` and `Principal`, takes no uploaded file, performs no document check, and unconditionally persists a new `Application` with status `Pending`. The matching apply form in `property-detail.html` is a plain `POST` with no file input and no `enctype="multipart/form-data"`, so no file is ever sent at application time. The result is applications created with zero linked `application_document` rows, leaving landlords with incomplete submissions.

The fix restores the requirement by (1) adding a file input and `enctype="multipart/form-data"` to the apply form, and (2) changing `applyToProperty` to accept a `MultipartFile`, reject submissions with a missing or empty file (without persisting the `Application`), and, when a valid file is present, persist both the `Application` and a linked `ApplicationDocument` using the same storage mechanism already used by `/submit-documents/{applicationId}`. The fix is scoped so all other flows — duplicate prevention, the post-acceptance document flow, cancellation cleanup, and unrelated actions — behave exactly as before.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug — a student submits a new (non-duplicate) application with no valid document attached, yet the `Application` is still persisted.
- **Property (P)**: The desired behavior — when the bug condition holds, the submission is rejected, no `Application` is persisted, and the student gets feedback that a document is required; when a valid document is present, both the `Application` and a linked `ApplicationDocument` are persisted.
- **Preservation**: Existing behaviors that must remain unchanged — duplicate-application prevention, success redirect to `/my-applications`, the `/submit-documents/{applicationId}` post-acceptance flow, cancellation document cleanup, and all unrelated actions.
- **applyToProperty**: The handler in `PropertyController.java` mapped to `POST /apply/{propertyId}` that creates a student's application for a property.
- **submitDocuments**: The existing handler in `PropertyController.java` mapped to `POST /submit-documents/{applicationId}` that stores an uploaded file to disk and saves an `ApplicationDocument`. Its file-storage approach is the pattern the fix reuses.
- **isNewApplication**: True when the student has no existing `Application` for the property (i.e. `applicationRepository.findByStudentIDAndPropertyID` returns empty).
- **ApplicationDocument**: Plain JPA entity (`application_document` table) linking a stored file (`fileName`, `filePath`, `uploadedAt`) to an application via `applicationID`.

## Bug Details

### Bug Condition

The bug manifests when a student submits a new (non-duplicate) application via `POST /apply/{propertyId}` and no valid document is attached. The `applyToProperty` handler has no file parameter, performs no document validation, and unconditionally persists the `Application` with status `Pending` and no linked `ApplicationDocument`. Because the apply form is a plain POST with no file input and no `enctype="multipart/form-data"`, a document is never even transmitted at application time.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type ApplicationSubmission  // student POSTing to /apply/{propertyId}
  OUTPUT: boolean

  // Duplicate submissions are handled by existing (correct) logic and are
  // out of scope. The bug is specifically about NEW applications with no
  // valid document still being persisted.
  RETURN input.isNewApplication = true
         AND (input.document = NULL OR input.document.isEmpty = true)
END FUNCTION
```

### Examples

- A student clicks "Apply for this Property" on a property they have not applied to before. No file is attached (the form has no file field). Expected: submission rejected with "a document is required" feedback and no `Application` row created. Actual: an `Application` (status `Pending`) is created with no `ApplicationDocument`.
- A student submits the apply form with a file field present but no file chosen (empty multipart part). Expected: treated as no document, submission rejected, nothing persisted. Actual (post-form-change, without handler fix): would still persist the `Application`.
- A student submits the apply form with a valid PDF attached. Expected: `Application` (status `Pending`) plus a linked `ApplicationDocument` referencing the stored file. Actual (current): `Application` persisted, document ignored/never sent.
- Edge case — a student who already applied submits again: existing duplicate-prevention redirects to `/my-applications` without creating another `Application`. This is NOT the bug condition and must remain unchanged.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Duplicate-application prevention: a student who already has any application for a property is redirected to `/my-applications` without creating another `Application`.
- Success path: a valid application (with a document) still redirects to the My Applications view on success.
- The post-acceptance document flow `POST /submit-documents/{applicationId}` continues to store documents and to enforce that documents are only accepted for `Accepted` applications.
- Cancellation `POST /cancel-application/{applicationId}` continues to delete any associated documents and the application without error.
- Unrelated actions (favoriting a property, viewing dashboards, browsing/searching properties, writing reviews, landlord application management) continue to behave exactly as before.

**Scope:**
All inputs that do NOT satisfy the bug condition should be completely unaffected by this fix. This includes:
- Duplicate application submissions (already handled by existing redirect logic).
- New application submissions that DO include a valid, non-empty document.
- Any request to endpoints other than `POST /apply/{propertyId}`.

**Note:** The actual expected correct behavior for the bug condition is defined in the Correctness Properties section (Property 1). This section focuses on what must NOT change.

## Hypothesized Root Cause

Based on the bug analysis, the regression has two grounded, cooperating causes:

1. **Handler accepts no file and never validates a document**: `applyToProperty(@PathVariable Integer propertyId, Principal principal)` has no `MultipartFile` parameter and no document check. After the duplicate guard, it builds an `Application`, sets status `Pending`, and saves it unconditionally — never creating an `ApplicationDocument`.
   - There is no branch that rejects a submission when a document is missing.
   - There is no call to persist an `ApplicationDocument` at application time.

2. **Apply form transmits no file**: In `property-detail.html`, the apply form is `<form ... method="post">` with a single submit button. It has no `enctype="multipart/form-data"` and no `<input type="file">`, so even if the handler expected a file, none would arrive.

3. **Working document-storage pattern already exists but is only used post-acceptance**: `submitDocuments` already demonstrates the correct storage approach (resolve `uploadDir`, create directory if missing, build a unique filename, `Files.copy` the stream, then save an `ApplicationDocument`). The fix reuses this exact pattern at application time rather than inventing a new one.

## Correctness Properties

Property 1: Bug Condition - Document Required at Application Time

_For any_ input where the bug condition holds (`isBugCondition` returns true — a new application with a missing or empty document), the fixed `applyToProperty` function SHALL reject the submission: it SHALL NOT persist the `Application`, SHALL NOT persist any `ApplicationDocument`, and SHALL return the student to the application flow with feedback indicating a document is required.

**Validates: Requirements 2.1, 2.3**

Property 2: Preservation - Non-Buggy Submissions Unchanged

_For any_ input where the bug condition does NOT hold (`isBugCondition` returns false — duplicate submissions, or new submissions with a valid non-empty document), the fixed code SHALL produce the same externally observable result as the original code: duplicate submissions still redirect to `/my-applications` without creating another `Application`, and a valid submission still persists the `Application` (status `Pending`) and redirects to the My Applications view — additionally persisting the linked `ApplicationDocument` that the original flow was supposed to record.

**Validates: Requirements 2.2, 3.1, 3.2, 3.3, 3.4, 3.5**

## Fix Implementation

### Changes Required

Assuming the root cause analysis is correct:

**File**: `src/main/resources/templates/property-detail.html`

**Change**: Update the apply form so a document can be submitted.
1. **Enable file upload on the form**: Add `enctype="multipart/form-data"` to the apply `<form>`.
2. **Add a required file input**: Add `<input type="file" name="document" required>` inside the form so the student must choose a file before the browser submits. The parameter name `document` must match the handler's `@RequestParam`.
3. **Preserve the existing "already applied" branch**: Keep the `th:unless="${hasApplied}"` form and the `th:if="${hasApplied}"` message untouched, so duplicate/already-applied UI stays identical.

**File**: `src/main/java/com/ulee/ulee_backend/controller/PropertyController.java`

**Function**: `applyToProperty`

**Specific Changes**:
1. **Accept the uploaded file**: Change the signature to `applyToProperty(@PathVariable Integer propertyId, @RequestParam(value = "document", required = false) MultipartFile document, Principal principal) throws IOException`. Using `required = false` lets the handler produce its own "document required" feedback rather than a raw 400 when the part is absent.
2. **Keep the duplicate guard first and unchanged**: Retain the existing `findByStudentIDAndPropertyID` check that redirects to `/my-applications` when an application already exists, before any document handling. This preserves duplicate-prevention behavior.
3. **Validate the document (server-side)**: After the duplicate guard, if `document == null || document.isEmpty()`, return a redirect that does NOT persist anything and signals the missing document, e.g. `redirect:/property/{propertyId}?error=document-required`. This enforces the requirement even against a direct POST that bypasses the form's `required` attribute.
4. **Persist the Application, then the document, reusing the existing storage pattern**: When a valid document is present, save the `Application` (status `Pending`, current timestamp) exactly as today to obtain its generated `applicationID`, then store the file and save a linked `ApplicationDocument` using the same steps as `submitDocuments`:
   - Resolve `uploadDir` to a `Path`; create the directory if it does not exist.
   - Build a unique filename (`applicationID + "_" + System.currentTimeMillis() + "_" + document.getOriginalFilename()`).
   - `Files.copy(document.getInputStream(), filePath)`.
   - Create an `ApplicationDocument`, set `applicationID`, `fileName` (original filename), `filePath` (stored path), `uploadedAt` (now), and save via `applicationDocumentRepository`.
5. **Preserve the success redirect**: Keep the final `redirect:/my-applications?toast=applied` so the success path is unchanged.

> Note on atomicity: `applyToProperty` is not `@Transactional` today, so a save-then-store ordering matches existing project conventions (e.g. `listProperty`, `addPropertyFeature` save the parent then store files). Because the document is validated as non-empty before the `Application` is saved, the bug condition (persist without document) cannot occur. A rare I/O failure after the `Application` save would leave an application without a document, matching the pre-existing non-transactional style used elsewhere; introducing a service/transaction layer is out of scope for this bugfix.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on the unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If refuted, re-hypothesize.

**Test Plan**: Exercise `applyToProperty` (or the endpoint via MockMvc) for a student with no existing application and no document, then assert that no `Application` was persisted. Run on the UNFIXED code to observe the failure (an `Application` is created).

**Test Cases**:
1. **New application, no document**: Invoke apply for a student/property with no prior application and no file. Assert no `Application` persisted and feedback indicates a document is required (will fail on unfixed code — an `Application` is created).
2. **New application, empty file**: Invoke apply with an empty `MultipartFile`. Assert no `Application` persisted (will fail on unfixed code — the handler has no file parameter, so behavior collapses to case 1 and an `Application` is created).
3. **Form transmits no file**: Render `property-detail.html` and assert the apply form has `enctype="multipart/form-data"` and a file input (will fail on unfixed template).

**Expected Counterexamples**:
- An `Application` row (status `Pending`) is created with zero linked `application_document` rows when no document is supplied.
- Possible causes: handler has no file parameter, no document validation branch, and the form sends no file.

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := applyToProperty_fixed(input)
  ASSERT applicationNotPersisted(input.studentID, input.propertyID)
         AND noDocumentPersisted(input.studentID, input.propertyID)
         AND feedbackIndicatesDocumentRequired(result)
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT applyToProperty_original(input) ~= applyToProperty_fixed(input)
  // where ~= means "same externally observable outcome": same redirect,
  // same duplicate-prevention, same Application persistence on the valid path
  // (the fixed version additionally records the ApplicationDocument that the
  //  flow was always intended to produce).
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain.
- It catches edge cases that manual unit tests might miss.
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs.

**Test Plan**: Observe behavior on the UNFIXED code for duplicate submissions and for the other document/cancel flows, then write tests capturing that behavior and re-run against the fixed code.

**Test Cases**:
1. **Duplicate application preservation**: A student who already has an application for the property submits again — assert redirect to `/my-applications` and no new `Application` (unchanged before and after fix).
2. **Valid application success path**: A student with a valid document applies — assert `Application` persisted with status `Pending`, a linked `ApplicationDocument` persisted, and redirect to `/my-applications` (success redirect preserved).
3. **Post-acceptance document flow preservation**: `POST /submit-documents/{applicationId}` still stores a document only for `Accepted` applications and redirects appropriately (unchanged).
4. **Cancellation cleanup preservation**: `POST /cancel-application/{applicationId}` still deletes associated documents and the application without error (unchanged).
5. **Unrelated actions preservation**: Favoriting, dashboards, search, and review flows behave exactly as before.

### Unit Tests

- `applyToProperty` rejects a new application with a `null`/empty document: no `Application` saved, redirect signals document required.
- `applyToProperty` persists both `Application` (status `Pending`) and a linked `ApplicationDocument` when a valid file is supplied.
- `applyToProperty` still redirects duplicate submissions to `/my-applications` without saving.
- Stored `ApplicationDocument` has correct `applicationID`, `fileName`, `filePath`, and `uploadedAt` (matching the `submitDocuments` storage pattern).

### Property-Based Tests

- Generate random new-application submissions with missing/empty documents and assert nothing is persisted and feedback is returned (Property 1).
- Generate random non-buggy inputs (duplicates, and valid submissions with non-empty documents) and assert the fixed handler's observable outcome matches the original for duplicates and preserves the success path for valid submissions (Property 2).
- Generate varied filenames/content for valid documents and assert an `ApplicationDocument` is always created and linked to the new `Application`.

### Integration Tests

- Full apply flow via MockMvc: multipart POST to `/apply/{propertyId}` with a file — expect `Application` + `ApplicationDocument` persisted and redirect to `/my-applications?toast=applied`.
- Full apply flow via MockMvc: POST to `/apply/{propertyId}` with no/empty file — expect no persistence and redirect to the property page with a document-required error.
- End-to-end preservation: apply with document, landlord accepts, student submits additional documents via `/submit-documents/{applicationId}`, then cancels — assert each step behaves as before and cancellation cleans up documents.
