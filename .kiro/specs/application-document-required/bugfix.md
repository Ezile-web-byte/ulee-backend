# Bugfix Requirements Document

## Introduction

Students can currently submit a property application without attaching any supporting document. This is a regression: previously a student had to provide a document in order to apply to a property, and that enforcement is no longer happening.

Grounded in the code, the defect lives in the application submission endpoint `applyToProperty` (`@PostMapping("/apply/{propertyId}")` in `PropertyController`). The handler accepts only `propertyId` and `Principal` — it takes no uploaded file, performs no document check, and unconditionally persists a new `Application` with status `Pending`. The matching apply form in `property-detail.html` is a plain `POST` with no file input and no `enctype="multipart/form-data"`, so no document is ever sent at application time. As a result, applications are created with zero linked `application_document` rows.

The impact is that landlords receive incomplete applications with no supporting document, undermining the review process the application flow was designed to support. This fix restores the requirement that a document must be submitted for an application to be accepted and persisted, and rejects incomplete submissions with appropriate feedback.

## Bug Analysis

### Current Behavior (Defect)

When a student submits an application, the system persists it regardless of whether a supporting document was provided.

1.1 WHEN a student submits an application to a property with no document attached THEN the system creates and persists the `Application` (status `Pending`) with no associated `ApplicationDocument`

1.2 WHEN a student submits an application to a property THEN the system does not accept, validate, or store any document at application time (the `/apply/{propertyId}` handler has no file parameter and the apply form has no file input)

1.3 WHEN a student submits an application with no document THEN the system gives no feedback indicating a document was required and silently completes the application

### Expected Behavior (Correct)

The application submission must require a supporting document. An application must not be persisted unless a valid document is provided.

2.1 WHEN a student submits an application to a property with no document attached THEN the system SHALL reject the submission, SHALL NOT persist the `Application`, and SHALL return the student to the application flow with feedback indicating a document is required

2.2 WHEN a student submits an application to a property with a valid document attached THEN the system SHALL persist the `Application` (status `Pending`) AND SHALL persist a linked `ApplicationDocument` referencing the stored file

2.3 WHEN a student submits an application with an empty or missing file (e.g. a file field present but no file chosen) THEN the system SHALL treat it as no document provided and SHALL reject the submission without persisting the `Application`

### Unchanged Behavior (Regression Prevention)

Behavior for submissions and flows that are not part of the missing document-at-application-time defect must be preserved.

3.1 WHEN a student who already has an application for a property attempts to apply again THEN the system SHALL CONTINUE TO prevent the duplicate and redirect to `/my-applications` without creating another `Application`

3.2 WHEN a student submits a valid application with a document THEN the system SHALL CONTINUE TO redirect to the My Applications view on success

3.3 WHEN a landlord has accepted an application and the student submits documents via `/submit-documents/{applicationId}` THEN the system SHALL CONTINUE TO store the document and enforce that documents are only accepted for `Accepted` applications

3.4 WHEN a student cancels an application via `/cancel-application/{applicationId}` THEN the system SHALL CONTINUE TO delete any associated documents and the application without error

3.5 WHEN a student performs unrelated actions (favoriting a property, viewing dashboards, browsing properties) THEN the system SHALL CONTINUE TO behave exactly as before

## Deriving the Bug Condition

### Bug Condition Function

```pascal
FUNCTION isBugCondition(X)
  INPUT: X of type ApplicationSubmission   // student applying via POST /apply/{propertyId}
  OUTPUT: boolean

  // The bug triggers when a student submits a NEW (non-duplicate) application
  // and no valid document is attached, yet the application is still persisted.
  RETURN X.isNewApplication = true
         AND (X.document = NULL OR X.document.isEmpty = true)
END FUNCTION
```

### Property Specification (Fix Checking)

```pascal
// Property: Fix Checking - Document required at application time
FOR ALL X WHERE isBugCondition(X) DO
  result <- applyToProperty'(X)
  ASSERT applicationNotPersisted(X.studentID, X.propertyID)
         AND feedbackIndicatesDocumentRequired(result)
END FOR
```

### Preservation Goal (Preservation Checking)

```pascal
// Property: Preservation Checking - non-buggy submissions unchanged
FOR ALL X WHERE NOT isBugCondition(X) DO
  ASSERT applyToProperty(X) = applyToProperty'(X)
END FOR
```

Where **F** = `applyToProperty` (current, unfixed handler) and **F'** = the fixed handler that requires a document and persists both the `Application` and its `ApplicationDocument`.
