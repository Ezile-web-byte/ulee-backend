# Requirements Document

## Introduction

The Complete Accommodation Application feature replaces the current one-click Apply action with a realistic, multi-section application flow. A student is considered to have applied only after completing and validating the required details, attaching the required supporting documents, reviewing the complete application, explicitly confirming the declarations, and receiving a successful final-submission response.

The feature preserves the selected property context throughout the flow, stores a submission-time snapshot rather than forcing all answers into the existing `Application` entity, and creates the `Application` and related `ApplicationDocument` records as one atomic outcome. The feature also preserves duplicate protection across browser and direct-request scenarios, secures sensitive application data, and distinguishes initial supporting documents from any post-acceptance documents requested later.

**Assumptions made for initial review:**
- A submitted student/property pair has at most one Application across all statuses, including Cancelled; cancellation changes status rather than deleting the audit record.
- At least one Proof_of_Enrollment document is required. Funding_Evidence and Guarantor_Consent documents become required only when the selected Funding_Method makes the corresponding document applicable.
- Each attachment is no larger than 5 MB, the combined attachment payload is no larger than 9 MB, and no more than five files are attached. The 9 MB aggregate limit leaves request overhead below the existing 10 MB multipart request limit.
- Allowed initial attachment formats are PDF, JPEG, and PNG.
- Government identifier collection is limited to an identity or passport number needed to establish applicant identity; protected-characteristic information is outside the application decision data.
- Existing Application records are preserved and migrated without assuming that every pre-existing row is fake. Seeded records are distinguished through explicit provenance rather than deletion based only on student or property identifiers.

## Glossary

- **ULEE_System**: The ULEE web application and server-side components that deliver and persist accommodation applications.
- **Complete_Application_Flow**: The multi-section student workflow from selecting Apply through successful final submission.
- **Authenticated_Student**: An active authenticated User whose role is STUDENT and whose matching Student profile belongs to the authenticated account.
- **Selected_Property**: The Property for which the Authenticated_Student starts the Complete_Application_Flow.
- **Property_Summary**: The Selected_Property title, location, accommodation type, monthly rent, deposit, availability date, and primary image when an image exists.
- **Application_Draft**: Temporary, non-Application state containing entered values, attachment references, progress, and a Submission_Token before final submission.
- **Submission_Token**: An unguessable, single-flow identifier used to make final submission idempotent.
- **Application**: The existing persistent application record containing applicationID, studentID, propertyID, status, and applicationDate.
- **Application_Details**: A persistent submission-time snapshot associated with an Application and containing the completed form data and declaration audit data.
- **ApplicationDocument**: Persistent metadata for a document associated with an Application.
- **Initial_Supporting_Document**: A document attached inside the Complete_Application_Flow before final confirmation.
- **Post_Acceptance_Document**: A supplementary document requested and uploaded only after an Application reaches Accepted status; a Post_Acceptance_Document is distinct from an Initial_Supporting_Document.
- **Pending**: The initial status of a successfully submitted Application awaiting landlord review.
- **Terminal_Status**: An Application status of Rejected or Cancelled.
- **Personal_and_Contact_Details**: Legal first name, legal last name, date of birth, primary email address, mobile number, and current residential address.
- **Identity_and_Student_Details**: Identity-document type, identity or passport number, student number, and enrollment status.
- **Study_Details**: Institution, campus when applicable, programme or qualification, and year of study.
- **Occupancy_Details**: Intended move-in date, requested tenancy duration in months, and number of intended occupants covered by the application.
- **Emergency_Contact_Details**: Emergency contact name, relationship to the applicant, mobile number, and optional email address.
- **Funding_Details**: Funding_Method, maximum monthly accommodation budget, and applicable funding reference information.
- **Funding_Method**: One of NSFAS, bursary or scholarship, self-funded, family-funded, private sponsor, or guarantor-supported.
- **Guarantor_Details**: Guarantor legal name, relationship to the applicant, email address, mobile number, and confirmation that the guarantor consented to contact and verification.
- **Proof_of_Enrollment**: A current institution-issued registration letter, enrollment letter, or student card supplied as an Initial_Supporting_Document.
- **Funding_Evidence**: A funding confirmation, bursary letter, NSFAS confirmation, sponsor confirmation, or equivalent evidence supplied when required by the Funding_Method.
- **Guarantor_Consent**: Evidence that the named guarantor agreed to provide support and to be contacted, supplied when the Funding_Method is guarantor-supported.
- **Required_Declaration**: A separately presented confirmation covering information accuracy, authority to provide document and emergency-contact data, privacy processing, and acknowledgement that submission is an application rather than a tenancy agreement.
- **Review_Step**: The read-only summary step displaying all entered data, Selected_Property context, attached filenames, document categories, and Required_Declarations before final submission.
- **Explicit_Confirmation**: The Authenticated_Student's unchecked-by-default confirmation of every Required_Declaration followed by activation of the final Submit Application control.
- **Server_Side_Validation**: Validation performed by the ULEE_System independently of browser validation.
- **Field_Error**: A message identifying a field or document and the correction required.
- **Finalized_Document**: A validated Initial_Supporting_Document stored in protected permanent storage and represented by an ApplicationDocument record.
- **Staged_Document**: A temporary encrypted or access-restricted upload that has not become a Finalized_Document.
- **Authorized_Reviewer**: The applicant, a landlord who owns the Selected_Property, or an administrator with application-review permission.
- **Legacy_Application**: An Application created before deployment of the Complete_Application_Flow.
- **Seeded_Application**: A demonstrational Application inserted by an identified development or test seed process.
- **Demonstration_Mode**: An explicitly configured development or demonstration environment in which Seeded_Applications are intentionally shown in application views and counts.
- **Application_Provenance**: Metadata identifying whether an Application originated from the Complete_Application_Flow, a legacy production flow, or an identified seed process.
- **Canonical_Application**: The single active Application selected for a student/property pair when legacy duplicate rows exist.
- **Archived_Legacy_Record**: A preserved, read-only legacy duplicate excluded from active duplicate and status counts but retained for audit and document access.
- **Cross_Site_Request_Forgery_Token**: A server-issued token that binds a state-changing request to the authenticated browser session.
- **Retention_Policy**: The documented configuration specifying how long application details and documents remain stored after each Application status.
- **Verification_Suite**: Automated unit, property-based, integration, security, accessibility, and responsive-layout tests for this feature.

## Requirements

### Requirement 1: Start the Application Flow

**User Story:** As a student, I want Apply to open a complete application form, so that an application is created only after I provide the required information.

#### Acceptance Criteria

1. WHEN an Authenticated_Student activates Apply for an available Selected_Property, THE ULEE_System SHALL open the Complete_Application_Flow without creating an Application or ApplicationDocument record.
2. WHEN the Complete_Application_Flow opens, THE ULEE_System SHALL create or resume one Application_Draft for the Authenticated_Student and Selected_Property.
3. IF an unauthenticated user activates Apply, THEN THE ULEE_System SHALL require authentication and preserve the Selected_Property as the post-authentication destination.
4. IF an authenticated account without the STUDENT role requests the Complete_Application_Flow, THEN THE ULEE_System SHALL return an authorization error and create zero Application, Application_Details, ApplicationDocument, or document-storage records.
5. IF the Selected_Property does not exist or is unavailable when the flow starts, THEN THE ULEE_System SHALL display a property-specific unavailability message and create zero Application records.
6. WHEN any pre-submission step is completed, THE ULEE_System SHALL maintain zero Pending Application records for the Application_Draft.

### Requirement 2: Collect Necessary Application Information

**User Story:** As a student, I want the form to ask for information relevant to accommodation, so that the landlord can assess a complete application without collecting unrelated personal data.

#### Acceptance Criteria

1. THE Complete_Application_Flow SHALL contain separate sections for Personal_and_Contact_Details, Identity_and_Student_Details, Study_Details, Occupancy_Details, Emergency_Contact_Details, Funding_Details, Initial_Supporting_Documents, and Required_Declarations.
2. THE Complete_Application_Flow SHALL require every field in Personal_and_Contact_Details except an address line designated as optional.
3. THE Complete_Application_Flow SHALL require every field in Identity_and_Student_Details.
4. THE Complete_Application_Flow SHALL require institution, programme or qualification, year of study, and enrollment status within Study_Details.
5. WHERE the institution uses multiple campuses, THE Complete_Application_Flow SHALL require the campus within Study_Details.
6. THE Complete_Application_Flow SHALL require intended move-in date, tenancy duration, and intended occupant count within Occupancy_Details.
7. THE Complete_Application_Flow SHALL require emergency contact name, relationship, and mobile number within Emergency_Contact_Details.
8. THE Complete_Application_Flow SHALL require Funding_Method and maximum monthly accommodation budget within Funding_Details.
9. WHERE the Funding_Method is guarantor-supported, THE Complete_Application_Flow SHALL require every Guarantor_Details field.
10. THE Complete_Application_Flow SHALL omit race, ethnicity, sex, gender identity, sexual orientation, religion, marital status, health information, disability status, and political affiliation from decision data.
11. THE ULEE_System SHALL maintain separate storage and access controls for voluntary accessibility requests even when the Complete_Application_Flow does not offer an accessibility-request field.
12. WHERE a voluntary accessibility request is offered, THE ULEE_System SHALL store the request separately from landlord decision data and present a clear optional label.

### Requirement 3: Prefill, Review, and Validate Entered Data

**User Story:** As a returning student, I want existing profile information prefilled but editable and validated, so that I can submit accurate current information efficiently.

#### Acceptance Criteria

1. WHEN an Application_Draft is first created, THE ULEE_System SHALL prefill matching Personal_and_Contact_Details, Study_Details, and Funding_Details from the authenticated User and Student profiles when values exist.
2. WHEN a prefilled field is displayed, THE Complete_Application_Flow SHALL allow the Authenticated_Student to review and edit the field before submission.
3. THE Complete_Application_Flow SHALL visually identify prefilled fields as requiring review rather than treating profile data as confirmed.
4. THE ULEE_System SHALL require email addresses to use a syntactically valid email format and contain no more than 254 characters.
5. THE ULEE_System SHALL require mobile numbers to contain 7 through 15 digits after permitted spaces, hyphens, parentheses, and one leading plus sign are removed.
6. THE ULEE_System SHALL require the date of birth to be a valid past calendar date.
7. THE ULEE_System SHALL require intended move-in dates to be on or after the Selected_Property availability date and on or after the current date.
8. THE ULEE_System SHALL require tenancy duration to be an integer from 1 through 24 months.
9. THE ULEE_System SHALL require intended occupant count to be an integer from 1 through the Selected_Property capacity.
10. THE ULEE_System SHALL require year of study to be an integer from 1 through 10.
11. THE ULEE_System SHALL require maximum monthly accommodation budget to be a non-negative monetary amount with no more than two decimal places.
12. IF Server_Side_Validation rejects a field, THEN THE ULEE_System SHALL preserve valid entered values and display a Field_Error adjacent to the rejected field.
13. WHEN final submission succeeds, THE ULEE_System SHALL persist reviewed form values in Application_Details as a submission-time snapshot independent of later User or Student profile edits.

### Requirement 4: Maintain Property Context and Draft Progress

**User Story:** As a student, I want to see the selected accommodation and retain my progress, so that I can complete the correct application without losing work during normal navigation.

#### Acceptance Criteria

1. WHILE the Complete_Application_Flow is open, THE ULEE_System SHALL display the Property_Summary on every section and on the Review_Step.
2. WHEN the Authenticated_Student moves backward or forward between completed sections, THE ULEE_System SHALL restore entered values and selected document metadata from the Application_Draft.
3. WHEN the Authenticated_Student refreshes a Complete_Application_Flow page during the authenticated session, THE ULEE_System SHALL restore the latest saved Application_Draft state.
4. WHEN the Authenticated_Student returns to the same Selected_Property flow during an unexpired authenticated session, THE ULEE_System SHALL offer to resume or discard the existing Application_Draft.
5. WHEN the Authenticated_Student chooses to discard an Application_Draft, THE ULEE_System SHALL remove associated Staged_Documents and retain zero Application records for the discarded draft.
6. IF the authenticated session expires before final submission, THEN THE ULEE_System SHALL immediately hide Application_Draft data and require re-authentication before exposing Application_Draft data again.
7. IF an Application_Draft expires under the configured draft-expiry period, THEN THE ULEE_System SHALL remove associated Staged_Documents and explain that the draft expired.
8. IF Selected_Property price, deposit, availability date, capacity, or availability status changes before final submission, THEN THE ULEE_System SHALL invalidate the displayed Review_Step and prevent Explicit_Confirmation until the Authenticated_Student reviews the updated Property_Summary.

### Requirement 5: Attach and Validate Initial Supporting Documents

**User Story:** As a student, I want to attach supporting documents near the end of the form, so that the submitted application includes the evidence required for assessment.

#### Acceptance Criteria

1. WHEN the Authenticated_Student reaches the Initial_Supporting_Documents section, THE ULEE_System SHALL allow attachment of 1 through 5 files before the Review_Step.
2. THE ULEE_System SHALL require one Initial_Supporting_Document categorized as Proof_of_Enrollment.
3. WHERE the Funding_Method requires third-party funding evidence, THE ULEE_System SHALL require one Initial_Supporting_Document categorized as Funding_Evidence.
4. WHERE the Funding_Method is guarantor-supported, THE ULEE_System SHALL require one Initial_Supporting_Document categorized as Guarantor_Consent.
5. THE ULEE_System SHALL accept PDF, JPEG, and PNG Initial_Supporting_Documents whose filename extension, declared media type, and detected content signature agree.
6. THE ULEE_System SHALL limit each Initial_Supporting_Document to 5 MB and the combined attachment payload to 9 MB.
7. IF an attachment is empty, corrupted, password-protected, executable, or inconsistent with an allowed format, THEN THE ULEE_System SHALL reject the attachment and display a document-specific Field_Error.
8. WHEN an attachment passes validation, THE ULEE_System SHALL retain the original filename only as display metadata and store document content under an unguessable generated storage name.
9. WHEN an attachment passes validation, THE ULEE_System SHALL place document content outside publicly served paths as a Staged_Document until final submission succeeds.
10. IF a supplied filename contains a path component or control character, THEN THE ULEE_System SHALL remove the unsafe component from display metadata and prevent the filename from influencing the storage path.
11. WHEN the Authenticated_Student removes an attachment before submission, THE ULEE_System SHALL immediately remove the associated Staged_Document and update the Application_Draft.
12. IF attachment validation fails, THEN THE ULEE_System SHALL preserve accepted attachments and non-document form values without creating an Application record.

### Requirement 6: Review and Explicitly Confirm the Application

**User Story:** As a student, I want to review the complete application and explicitly confirm the declarations, so that I know exactly what will be submitted.

#### Acceptance Criteria

1. WHEN all preceding sections pass Server_Side_Validation, THE ULEE_System SHALL make the Review_Step available.
2. WHEN the Review_Step is displayed, THE ULEE_System SHALL show every submitted field grouped by section, the Property_Summary, and each attachment's original filename, category, format, and size.
3. THE Review_Step SHALL provide an edit action for each preceding section.
4. WHEN an edit changes any reviewed field, attachment, or Selected_Property value, THE ULEE_System SHALL invalidate the previous Explicit_Confirmation and require a refreshed Review_Step.
5. THE Review_Step SHALL present each Required_Declaration separately with an unchecked-by-default confirmation control.
6. WHILE one or more Required_Declaration controls remain unconfirmed, THE ULEE_System SHALL keep the final Submit Application control unavailable.
7. WHEN the Authenticated_Student confirms every Required_Declaration, THE ULEE_System SHALL record the confirmed declaration versions and confirmation timestamp in the Application_Draft.
8. WHEN the Authenticated_Student activates Submit Application, THE ULEE_System SHALL revalidate the complete Application_Draft, attachments, ownership, duplicate state, and Selected_Property eligibility on the server.

### Requirement 7: Persist One Complete Application Atomically

**User Story:** As a student, I want submission to be all-or-nothing, so that I never appear to have applied with missing details or documents.

#### Acceptance Criteria

1. WHEN final Server_Side_Validation and Explicit_Confirmation succeed, THE ULEE_System SHALL create exactly one Application with status Pending, one associated Application_Details record, and one ApplicationDocument record for each Initial_Supporting_Document.
2. WHEN final submission succeeds, THE ULEE_System SHALL set Application.studentID from the Authenticated_Student, Application.propertyID from the server-validated Selected_Property, and Application.applicationDate from the server submission time.
3. WHEN final submission succeeds, THE ULEE_System SHALL associate every ApplicationDocument with the newly created Application and classify each document as an Initial_Supporting_Document.
4. IF database persistence, document finalization, required submission-notification preparation, or any other required submission operation fails, THEN THE ULEE_System SHALL roll back the submission and retain zero new Pending Applications, zero new Application_Details records, zero new ApplicationDocument records, and zero new Finalized_Documents for the failed submission.
5. IF a recoverable failure occurs after Staged_Documents are created, THEN THE ULEE_System SHALL remove the failed submission's Staged_Documents before returning the failure response.
6. IF process interruption prevents immediate Staged_Document cleanup, THEN THE ULEE_System SHALL remove expired unreferenced Staged_Documents within 24 hours without removing Finalized_Documents.
7. WHEN final submission succeeds, THE ULEE_System SHALL remove the completed Application_Draft and all redundant staged copies.
8. WHEN any final-submission response is returned, THE ULEE_System SHALL satisfy the invariant that the student/property pair has either zero submitted records or one complete Pending Application with all required details and documents.

### Requirement 8: Prevent Duplicate Applications and Repeated Effects

**User Story:** As a student, I want retries to produce a clear stable result, so that repeated actions never create duplicate applications.

#### Acceptance Criteria

1. THE ULEE_System SHALL enforce at most one non-archived Application for each studentID and propertyID pair across all Application statuses.
2. WHEN the same Submission_Token is submitted more than once after a successful submission, THE ULEE_System SHALL return the original successful Application result without creating additional records or files.
3. WHEN repeated, refreshed, retried, or concurrent final-submission requests target the same studentID and propertyID, THE ULEE_System SHALL create exactly one Application and one set of associated records.
4. IF an Application already exists for the Authenticated_Student and Selected_Property, THEN THE ULEE_System SHALL block a new Application_Draft submission and display the existing Application status with a link to `/my-applications`.
5. IF a duplicate request arrives while the first request is still processing, THEN THE ULEE_System SHALL return a processing or existing-application response without creating a second Application.
6. WHEN an Application is cancelled, THE ULEE_System SHALL retain the Application with status Cancelled so that a later request does not create a second Application for the same student/property pair.
7. IF a client supplies a missing, malformed, expired, or foreign Submission_Token, THEN THE ULEE_System SHALL reject final submission and create zero Application records.

### Requirement 9: Enforce Authorization and Ownership

**User Story:** As a student, I want application data protected by account ownership, so that another user cannot submit or attach documents in my name.

#### Acceptance Criteria

1. THE ULEE_System SHALL derive studentID from the authenticated session for every Application_Draft, upload, review, and final-submission operation.
2. IF a request supplies a studentID different from the Authenticated_Student, THEN THE ULEE_System SHALL reject the request and make zero data or file changes.
3. IF an Authenticated_Student requests another student's Application_Draft, Staged_Document, Application_Details, Application, or ApplicationDocument, THEN THE ULEE_System SHALL return an authorization error without exposing existence-sensitive metadata.
4. IF a document upload references an Application_Draft owned by another student, THEN THE ULEE_System SHALL reject the upload and store zero document content or metadata.
5. WHEN an Authorized_Reviewer requests a Finalized_Document, THE ULEE_System SHALL verify current authorization before streaming the document.
6. IF a landlord requests application data for a property the landlord does not own, THEN THE ULEE_System SHALL return an authorization error.
7. WHEN a state-changing application request is submitted, THE ULEE_System SHALL validate a Cross_Site_Request_Forgery_Token.
8. IF an inactive account requests an application operation, THEN THE ULEE_System SHALL reject the operation and create zero new records or files.

### Requirement 10: Communicate Submission Outcomes

**User Story:** As a student, I want clear progress and outcome feedback, so that I know whether the application was submitted and what to correct when submission fails.

#### Acceptance Criteria

1. WHEN final submission starts, THE Complete_Application_Flow SHALL adapt the existing shared loading-state behavior to disable repeated activation and display a submitting label.
2. WHEN final submission succeeds, THE ULEE_System SHALL display the existing shared toast style with a successful-application message and redirect to `/my-applications`.
3. WHEN final submission succeeds, THE `/my-applications` page SHALL display exactly one Pending Application for the Selected_Property.
4. IF final submission fails validation, THEN THE ULEE_System SHALL return the Authenticated_Student to the relevant section with Field_Errors and preserve valid Application_Draft data.
5. IF final submission fails because the Selected_Property became unavailable, THEN THE ULEE_System SHALL display an availability-specific error and create zero Pending Applications.
6. IF final submission fails because an Application already exists, THEN THE ULEE_System SHALL display duplicate-specific feedback and a link to the existing Application.
7. IF final submission fails because of an internal persistence or storage error, THEN THE ULEE_System SHALL display a retry-safe error message without claiming that an Application was submitted.
8. WHEN a retry-safe failure response is displayed, THE Complete_Application_Flow SHALL restore the final Submit Application control after the response is processed.

### Requirement 11: Provide Accessible and Responsive Interaction

**User Story:** As a student using any supported device or assistive technology, I want an accessible application form, so that I can complete the flow independently.

#### Acceptance Criteria

1. THE Complete_Application_Flow SHALL conform to WCAG 2.1 Level AA requirements applicable to forms, navigation, status messages, focus, color contrast, and error identification.
2. THE Complete_Application_Flow SHALL associate every input with a persistent text label and every Field_Error with the corresponding input programmatically.
3. WHEN a section fails validation, THE Complete_Application_Flow SHALL move keyboard focus to an error summary that links to each invalid field.
4. WHEN a section transition or Review_Step loads, THE Complete_Application_Flow SHALL move focus to the section heading without trapping keyboard focus.
5. THE Complete_Application_Flow SHALL support completion using keyboard input without requiring pointer gestures.
6. WHEN loading, upload, validation, or submission status changes, THE Complete_Application_Flow SHALL announce the status through an assistive-technology status region.
7. WHILE the viewport width is between 320 and 1440 CSS pixels, THE Complete_Application_Flow SHALL present all controls, Property_Summary content, review content, and errors without horizontal page scrolling at 100% zoom.
8. WHILE text is enlarged to 200%, THE Complete_Application_Flow SHALL preserve content and functionality without overlapping interactive controls.
9. THE Complete_Application_Flow SHALL reuse and adapt the existing shared loading-state and toast components rather than introduce a duplicate component implementation.

### Requirement 12: Protect Sensitive Data and Documents

**User Story:** As an applicant, I want personal and supporting information handled securely and transparently, so that accommodation application data is not exposed or used unexpectedly.

#### Acceptance Criteria

1. BEFORE Explicit_Confirmation, THE ULEE_System SHALL present the purpose, recipient roles, Retention_Policy summary, and applicant rights for Application_Details and Initial_Supporting_Documents.
2. THE ULEE_System SHALL transmit application pages, form data, and document content only through authenticated HTTPS requests outside local development.
3. THE ULEE_System SHALL encrypt Application_Details, identity identifiers, emergency contact data, and document content at rest.
4. THE ULEE_System SHALL exclude identity identifiers, document content, document storage paths, Submission_Tokens, and emergency contact values from application logs and URLs.
5. THE ULEE_System SHALL serve Finalized_Documents through authorized application endpoints rather than public static URLs.
6. WHEN the Retention_Policy period for an Application expires, THE ULEE_System SHALL remove retained personal snapshot data and documents while preserving only non-sensitive audit metadata required by the Retention_Policy.
7. WHEN application data is displayed to a landlord, THE ULEE_System SHALL limit the display to fields and documents required for accommodation assessment.
8. IF uploaded content contains active executable content or a detected security threat, THEN THE ULEE_System SHALL quarantine or remove the content, create zero ApplicationDocument records for the content, and display a safe rejection message.
9. THE ULEE_System SHALL record auditable timestamps and actor identifiers for final submission, document access, status changes, and retention deletion without recording protected field values.

### Requirement 13: Migrate Existing Apply and Document Routes

**User Story:** As a user of existing links and pages, I want the upgraded flow to behave consistently, so that old entry points cannot bypass the complete application requirements.

#### Acceptance Criteria

1. WHEN the existing `/apply/{propertyId}` entry point receives a valid student request, THE ULEE_System SHALL route the Authenticated_Student to the Complete_Application_Flow and create zero Pending Applications at route entry.
2. IF a direct POST to `/apply/{propertyId}` omits complete confirmed application data, THEN THE ULEE_System SHALL redirect to or describe the Complete_Application_Flow without creating an Application.
3. WHEN existing property pages render Apply, THE ULEE_System SHALL label the action as starting an application rather than completing an application.
4. WHEN the existing `/submit-documents/{applicationId}` route is retained, THE ULEE_System SHALL accept only Post_Acceptance_Documents for an Accepted Application owned by the Authenticated_Student.
5. IF `/submit-documents/{applicationId}` receives an Initial_Supporting_Document or references a non-Accepted Application, THEN THE ULEE_System SHALL reject the upload and store zero files or ApplicationDocument records.
6. WHEN `/my-applications` displays document actions, THE ULEE_System SHALL distinguish Initial_Supporting_Documents submitted with the Application from landlord-requested Post_Acceptance_Documents.
7. IF the post-acceptance document capability is retired, THEN THE ULEE_System SHALL return a clear retired-route response from `/submit-documents/{applicationId}` without affecting Initial_Supporting_Documents.
8. WHEN existing loading states and toast messages are needed by the Complete_Application_Flow, THE ULEE_System SHALL adapt the established shared behavior and wording for the new multi-step outcome.

### Requirement 14: Preserve and Classify Existing Application Data

**User Story:** As an operator, I want legacy and seeded records handled safely, so that fake seed data does not block real students and legitimate historical applications are not deleted.

#### Acceptance Criteria

1. WHEN the feature migration runs, THE ULEE_System SHALL preserve every Legacy_Application and associated ApplicationDocument record.
2. WHEN the feature migration runs, THE ULEE_System SHALL assign Application_Provenance without classifying a record as Seeded_Application solely from studentID, propertyID, status, or applicationDate.
3. WHEN development or test seed scripts create applications after migration, THE ULEE_System SHALL mark each created record with seeded Application_Provenance.
4. WHEN an identified Seeded_Application matches a real Authenticated_Student and Selected_Property, THE ULEE_System SHALL exclude the Seeded_Application from real-user duplicate blocking and real-user application displays.
5. WHEN a Legacy_Application has no full Application_Details snapshot, THE ULEE_System SHALL display the Legacy_Application using available legacy fields and identify unavailable details without inventing values.
6. IF multiple Legacy_Applications share one studentID and propertyID pair, THEN THE ULEE_System SHALL select one Canonical_Application by a documented deterministic rule and preserve remaining rows as Archived_Legacy_Records.
7. WHEN legacy duplicates are archived, THE ULEE_System SHALL preserve status history, dates, and document associations for audit access.
8. WHILE the ULEE_System operates in Demonstration_Mode, THE ULEE_System SHALL include Seeded_Applications in application, dashboard, and landlord counts.
9. WHILE the ULEE_System operates outside Demonstration_Mode, THE ULEE_System SHALL exclude Seeded_Applications and Archived_Legacy_Records from active application, dashboard, and landlord counts.
10. IF a legacy row cannot be confidently identified as seeded, THEN THE ULEE_System SHALL preserve the row as a Legacy_Application pending authorized review.
11. WHEN uniqueness enforcement is enabled, THE ULEE_System SHALL apply the one-Application-per-student/property invariant to non-archived, non-seeded application data without deleting legitimate historical records.

### Requirement 15: Verify Correctness Properties and Critical Examples

**User Story:** As a maintainer, I want automated verification of the application invariants, so that validation, concurrency, migration, and file failures cannot silently create incomplete or duplicate applications.

#### Acceptance Criteria

1. THE Verification_Suite SHALL generate valid combinations of required fields, conditional Funding_Details, and allowed documents and pass only when each valid submission produces one complete Pending Application with an equivalent Application_Details snapshot.
2. THE Verification_Suite SHALL use generated missing, malformed, boundary, and mutually inconsistent field combinations to verify that Server_Side_Validation produces Field_Errors and zero Pending Applications.
3. THE Verification_Suite SHALL use generated allowed and disallowed filename, extension, media-type, content-signature, file-count, individual-size, and aggregate-size combinations to verify exact attachment acceptance boundaries.
4. THE Verification_Suite SHALL repeat each generated successful final-submission request between 2 and 20 times with the same Submission_Token to verify idempotent return of one Application and one document set.
5. THE Verification_Suite SHALL issue concurrent final-submission requests for the same generated student/property pair to verify the one-Application invariant.
6. THE Verification_Suite SHALL inject a failure at each database and document-storage transition to verify the zero-or-one complete-submission invariant and absence of orphan Finalized_Documents.
7. THE Verification_Suite SHALL generate path components, control characters, duplicate names, and Unicode names to verify that stored names remain unguessable and storage paths remain confined to protected storage.
8. THE Verification_Suite SHALL verify with representative integration examples that unauthenticated users, non-student users, inactive users, foreign student identifiers, foreign drafts, and non-owning landlords cannot create or access application data.
9. THE Verification_Suite SHALL verify with representative integration examples that the legacy `/apply/{propertyId}` route creates zero Pending Applications and enters the Complete_Application_Flow.
10. THE Verification_Suite SHALL verify with representative migration fixtures that legitimate Legacy_Applications remain accessible, identified Seeded_Applications do not block real submissions, and legacy duplicates remain preserved as Archived_Legacy_Records.
11. THE Verification_Suite SHALL execute and pass automated accessibility checks and keyboard-navigation examples for every section, the Review_Step, validation failures, upload status, and submission outcome.
12. THE Verification_Suite SHALL execute and pass responsive-layout examples at 320, 768, 1024, and 1440 CSS pixels and at 200% text enlargement.
13. THE Verification_Suite SHALL use mocked or in-memory document storage for high-iteration property tests and representative protected-storage integration examples for end-to-end verification.
