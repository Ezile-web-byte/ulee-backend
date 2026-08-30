# Implementation Plan

## Task Dependency Graph

```
Task 1 (Bug Condition exploration test — MUST FAIL on unfixed code)
        │
Task 2 (Preservation baseline — MUST PASS on unfixed code)
        │
        ▼
Task 3 (Apply the fix)
   ├── 3.1 property-detail.html: form enctype + required file input
   ├── 3.2 PropertyController.applyToProperty: accept MultipartFile, validate, persist Application + ApplicationDocument
   ├── 3.3 property-detail.html: surface document-required error message
   ├── 3.4 Verify Task 1 exploration test now PASSES (Property 1: Expected Behavior)
   └── 3.5 Verify Task 2 preservation baseline still PASSES (Property 2: Preservation)
        │
        ▼
Task 4 (Checkpoint — all tests pass)
```

Tasks 1 and 2 are written and run against the UNFIXED code before any implementation. Task 3 depends on the understanding gained in Tasks 1 and 2. Sub-tasks 3.1–3.3 implement the fix; sub-tasks 3.4–3.5 re-run the SAME tests from Tasks 1 and 2. Task 4 is the final gate.

---

- [ ] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Document Required at Application Time
  - **CRITICAL**: This test MUST FAIL on unfixed code — failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails** at this stage
  - **NOTE**: This test encodes the expected behavior — it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate a new (non-duplicate) application with a missing/empty document is still persisted
  - **Scoped PBT Approach**: This bug is deterministic. Scope the property to the concrete failing case(s) for reproducibility: a student with no prior `Application` for a property submits with `document = null` (and, as a variant, an empty `MultipartFile`).
  - Bug Condition (from design `isBugCondition`): `input.isNewApplication = true AND (input.document = NULL OR input.document.isEmpty = true)` — i.e. `applicationRepository.findByStudentIDAndPropertyID(studentID, propertyID)` returns empty and no valid document is attached.
  - Test implementation (MockMvc against `POST /apply/{propertyId}`, or a direct call to `PropertyController.applyToProperty`):
    - Arrange a student and property with NO existing `Application` (verify `findByStudentIDAndPropertyID` is empty).
    - Invoke apply with no document (and, as a scoped variant, an empty multipart part).
    - **Assert (Expected Behavior — Property 1)**: no `Application` is persisted for that `studentID`/`propertyID`, no linked `ApplicationDocument` is persisted, and feedback indicates a document is required (redirect to `/property/{propertyId}?error=document-required`).
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct — today the handler has no file parameter and unconditionally persists the `Application` with status `Pending`)
  - Document counterexamples found, e.g. "apply(studentID, propertyID) with no document creates an Application (status Pending) with zero linked application_document rows instead of rejecting"
  - Mark task complete when the test is written, run against unfixed code, and the failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.3_

- [ ] 2. Write preservation baseline property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Buggy Submissions and Flows Unchanged
  - **IMPORTANT**: Follow observation-first methodology — run the UNFIXED code for non-bug-condition inputs, record the actual outputs, then write tests asserting those observed outputs
  - Cover the cases where `isBugCondition` returns false (from Preservation Requirements in design):
    - **Duplicate application prevention**: student who already has an `Application` for the property submits again. Observe on unfixed code → redirect to `/my-applications` and NO new `Application`. Assert this outcome. _(Req 3.1)_
    - **Valid application success redirect**: student with a valid document applies. On unfixed code observe the redirect to the My Applications view (`redirect:/my-applications?toast=applied`) and that an `Application` (status `Pending`) is persisted. Capture the redirect + `Application` persistence as the baseline. _(Req 2.2, 3.2)_ — Note: the linked `ApplicationDocument` is the additional behavior the fix restores; assert `Application` + redirect here, and add the `ApplicationDocument` assertion in 3.5 after the fix.
    - **Post-acceptance document flow**: `POST /submit-documents/{applicationId}` stores a document only for `Accepted` applications and redirects appropriately. Observe and assert unchanged. _(Req 3.3)_
    - **Cancellation cleanup**: `POST /cancel-application/{applicationId}` deletes associated documents and the application without error. Observe and assert unchanged. _(Req 3.4)_
    - **Unrelated actions**: favoriting a property, viewing dashboards, browsing/searching properties behave exactly as before. Observe and assert unchanged. _(Req 3.5)_
  - Write property-based tests where practical (e.g. generate varied duplicate/valid submissions) to strengthen the "for all non-buggy inputs" guarantee
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms the baseline behavior that must be preserved)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 3. Fix: require a supporting document at application time

  - [ ] 3.1 Update the apply form in property-detail.html to transmit a document
    - Add `enctype="multipart/form-data"` to the apply `<form>` (the `th:unless="${hasApplied}"` form) so files are sent
    - Add a required file input inside the form: `<input type="file" name="document" required>` — the `name` must match the handler's `@RequestParam` value `document`
    - Preserve the existing "already applied" branch: keep the `th:unless="${hasApplied}"` form and the `th:if="${hasApplied}"` message untouched so duplicate/already-applied UI stays identical
    - _Bug_Condition: isBugCondition(input) — new application with document = NULL or empty (form currently sends no file)_
    - _Preservation: hasApplied branch and already-applied message unchanged (Req 3.1)_
    - _Requirements: 1.2, 2.2_

  - [ ] 3.2 Update PropertyController.applyToProperty to require and store the document
    - Change the signature to `applyToProperty(@PathVariable Integer propertyId, @RequestParam(value = "document", required = false) MultipartFile document, Principal principal) throws IOException` (`required = false` lets the handler return its own document-required feedback instead of a raw 400)
    - Keep the duplicate guard FIRST and unchanged: retain the `applicationRepository.findByStudentIDAndPropertyID(...)` check that redirects to `/my-applications` when an application already exists, before any document handling
    - Validate the document server-side: if `document == null || document.isEmpty()`, return `redirect:/property/{propertyId}?error=document-required` WITHOUT persisting any `Application` or `ApplicationDocument` (enforces the requirement even against a direct POST bypassing the form's `required` attribute)
    - When a valid document is present, persist the `Application` (status `Pending`, current timestamp) exactly as today to obtain its generated `applicationID`, then store the file and save a linked `ApplicationDocument` reusing the EXACT storage pattern from the existing `/submit-documents/{applicationId}` handler:
      - Resolve `uploadDir` to a `Path`; create the directory if it does not exist (`Files.createDirectories` / mkdir as in `submitDocuments`)
      - Build a unique filename: `applicationID + "_" + System.currentTimeMillis() + "_" + document.getOriginalFilename()`
      - `Files.copy(document.getInputStream(), filePath)`
      - Create an `ApplicationDocument`, set `applicationID`, `fileName` (original filename), `filePath` (stored path), `uploadedAt` (now); save via `applicationDocumentRepository` (plain JPA entity, `@Autowired` field injection, no service layer)
    - Preserve the success redirect: keep the final `redirect:/my-applications?toast=applied`
    - _Bug_Condition: isBugCondition(input) where input.isNewApplication = true AND (input.document = NULL OR input.document.isEmpty = true)_
    - _Expected_Behavior: reject without persisting when bug condition holds; persist Application + linked ApplicationDocument when a valid document is present (Property 1, expectedBehavior from design)_
    - _Preservation: duplicate guard first/unchanged, success redirect unchanged, /submit-documents storage pattern reused (Req 3.1, 3.2, 3.3)_
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 3.3 Surface the document-required error in the apply UI
    - In `property-detail.html`, read the `?error=document-required` request param and display a message near the apply form indicating a supporting document is required (e.g. `th:if="${param.error != null and param.error[0] == 'document-required'}"`)
    - Keep the message scoped to the apply form so no other UI is affected
    - _Expected_Behavior: feedbackIndicatesDocumentRequired(result) from design (Property 1)_
    - _Requirements: 1.3, 2.1_

  - [ ] 3.4 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Document Required at Application Time
    - **IMPORTANT**: Re-run the SAME test from Task 1 — do NOT write a new test
    - The test from Task 1 encodes the expected behavior; when it passes it confirms the expected behavior is satisfied
    - Run the bug condition exploration test from Task 1 against the fixed code
    - **EXPECTED OUTCOME**: Test PASSES — no `Application`/`ApplicationDocument` persisted for the missing/empty-document case, and the document-required feedback is returned (confirms the bug is fixed)
    - _Requirements: 2.1, 2.3 (Expected Behavior / Property 1 from design)_

  - [ ] 3.5 Verify preservation baseline tests still pass
    - **Property 2: Preservation** - Non-Buggy Submissions and Flows Unchanged
    - **IMPORTANT**: Re-run the SAME tests from Task 2 — do NOT write new tests
    - Run the preservation baseline property tests from Task 2 against the fixed code
    - Additionally assert the restored behavior on the valid success path: a linked `ApplicationDocument` is now persisted alongside the `Application` (the behavior the flow was always intended to produce)
    - **EXPECTED OUTCOME**: Tests PASS — duplicate prevention, success redirect, `/submit-documents` flow, cancellation cleanup, and unrelated actions are unchanged (no regressions)
    - _Requirements: 2.2, 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Run the full test suite: Task 1 exploration test (now PASSES), Task 2 preservation baseline (still PASSES), and any unit/integration tests
  - Confirm the fix resolves the bug condition and preserves all non-buggy behavior
  - Ensure all tests pass; ask the user if questions arise
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5_
