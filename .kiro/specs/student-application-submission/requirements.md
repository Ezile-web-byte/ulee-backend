# Requirements Document

## Introduction

The Student Application Submission feature replaces the current one-click application creation behavior with a deliberate review-and-confirm flow. Selecting Apply from an existing property detail page begins a review step but does not create an application record. An authenticated student must explicitly confirm the reviewed submission before the system creates one Pending application and presents the result through the existing My Applications experience.

The feature extends the existing Spring Boot, Spring Security, Thymeleaf, Spring Data JPA, property detail, and My Applications architecture. The current application data model remains the source of submitted applications. No draft application status or draft application row is introduced; a database-level uniqueness safeguard may be added during design if required to guarantee the duplicate-prevention requirements under concurrent requests.

## Glossary

- **Application_Submission_Feature**: The complete student-facing flow for starting, reviewing, confirming, and receiving the result of a property application submission.
- **Application_Review_Step**: The intermediate server-rendered step that presents the selected property and submission summary before final confirmation without creating an Application_Record.
- **Application_Review_Page**: The Thymeleaf page or confirmation surface that renders the Application_Review_Step.
- **Submission_Service**: The server-side component that validates a Final_Submission_Request and creates an Application_Record when every submission rule passes.
- **Authenticated_User**: A user identity established by the existing Spring Security authentication mechanism.
- **Authenticated_Student**: An Authenticated_User whose assigned role is STUDENT and whose Student_Profile exists.
- **Student_Profile**: The existing student record associated with an Authenticated_Student.
- **Student_ID**: The existing user identifier associated with an Authenticated_Student and used by Application_Record.
- **Property**: An existing property listing selected from the property detail page.
- **Property_ID**: The existing unique identifier of a Property.
- **Available_Property**: A Property whose server-side availability state permits a new application at the time of validation.
- **Application_Record**: A row in the existing `application` table containing the application identifier, Student_ID, Property_ID, status, and application date.
- **Pending_Status**: The existing `Pending` Application_Record status used for a newly submitted application awaiting landlord review.
- **Explicit_Final_Confirmation**: Activation of the dedicated final submission control on the Application_Review_Page after the submission summary is displayed.
- **Final_Submission_Request**: The server request produced by Explicit_Final_Confirmation.
- **Successful_Submission**: A Final_Submission_Request that passes authentication, authorization, property eligibility, input validation, and duplicate validation and creates one Application_Record.
- **Duplicate_Rule**: The rule that permits no more than one Application_Record for a given Student_ID and Property_ID pair, regardless of Application_Record status.
- **Validation_Message**: User-facing text that communicates a validation failure and states a recovery action when a recovery action exists.
- **Property_Detail_Page**: The existing Thymeleaf page for a single Property and the starting point for the application flow.
- **My_Applications_Page**: The existing student page that displays submitted Application_Record entries and submission feedback.

## Requirements

### Requirement 1: Deliberate Application Start

**User Story:** As a student viewing a property, I want Apply to begin a review step, so that an accidental click does not submit an application.

#### Acceptance Criteria

1. WHEN an Authenticated_Student activates the application control for an Available_Property on the Property_Detail_Page, THE Application_Submission_Feature SHALL display the Application_Review_Step with the Application_Record count unchanged.
2. WHILE the Application_Review_Step is displayed, THE Application_Review_Page SHALL identify the selected Property by Property_ID, title, location, monthly rent, deposit, and availability.
3. THE Application_Review_Page SHALL state that an Application_Record is created only after Explicit_Final_Confirmation.
4. THE Application_Review_Page SHALL provide a control that returns the Authenticated_Student to the selected Property_Detail_Page without producing a Final_Submission_Request.
5. WHEN an Authenticated_Student refreshes the Application_Review_Page before Explicit_Final_Confirmation, THE Application_Submission_Feature SHALL redisplay the Application_Review_Step with the Application_Record count unchanged.
6. WHEN an Authenticated_Student leaves the Application_Review_Step without Explicit_Final_Confirmation, THE Application_Submission_Feature SHALL keep the Application_Record count unchanged.

### Requirement 2: Authentication and Student Authorization

**User Story:** As a student, I want application submission tied to my authenticated account, so that another user cannot submit an application in my name.

#### Acceptance Criteria

1. IF an unauthenticated visitor attempts to open the Application_Review_Step, THEN THE Application_Submission_Feature SHALL direct the visitor to the existing authentication flow while preserving a read-only Application_Review_Step for the selected Property.
2. WHILE an unauthenticated visitor has a read-only Application_Review_Step, THE Application_Review_Page SHALL display a student-authentication-required message and provide no final submission control.
3. IF an unauthenticated visitor sends a Final_Submission_Request, THEN THE Submission_Service SHALL reject the request with the Application_Record count unchanged.
4. IF an Authenticated_User without the STUDENT role attempts to open the Application_Review_Step or sends a Final_Submission_Request, THEN THE Application_Submission_Feature SHALL deny the application action with the Application_Record count unchanged and display a role-specific Validation_Message.
5. IF an Authenticated_User with the STUDENT role has no Student_Profile, THEN THE Submission_Service SHALL reject the Final_Submission_Request with the Application_Record count unchanged and display a Validation_Message for completing or correcting the student account.
6. WHEN the Submission_Service processes a Final_Submission_Request, THE Submission_Service SHALL derive the Student_ID from the Authenticated_Student identity rather than from client-submitted identity data.
7. WHEN an unauthenticated visitor completes authentication as an Authenticated_Student after starting from a Property_Detail_Page, THE Application_Submission_Feature SHALL return the Authenticated_Student to the Application_Review_Step for the originally selected Property.

### Requirement 3: Explicit Final Confirmation

**User Story:** As a student, I want to confirm the application after reviewing the details, so that submission is intentional and understandable.

#### Acceptance Criteria

1. THE Application_Review_Page SHALL provide a dedicated final submission control labeled to communicate that activation submits a Pending application.
2. WHEN an Authenticated_Student activates the final submission control, THE Application_Submission_Feature SHALL send one Final_Submission_Request for the selected Property_ID.
3. WHILE Explicit_Final_Confirmation has not occurred, THE Submission_Service SHALL keep the Application_Record count for the Authenticated_Student and selected Property unchanged.
4. WHEN a Final_Submission_Request satisfies every submission rule, THE Submission_Service SHALL create exactly one Application_Record containing the Authenticated_Student's Student_ID, the selected Property_ID, Pending_Status, and a server-generated application date.
5. WHEN a Successful_Submission completes, THE Application_Submission_Feature SHALL redirect the Authenticated_Student to the My_Applications_Page.
6. WHEN the My_Applications_Page is displayed after a Successful_Submission, THE My_Applications_Page SHALL display the created Application_Record with Pending_Status and a submission-success message identifying the selected Property.

### Requirement 4: Server-Side Submission Validation

**User Story:** As a student, I want the system to validate the application at confirmation time, so that I receive a reliable result when property conditions have changed.

#### Acceptance Criteria

1. WHEN the Application_Review_Step is requested, THE Application_Submission_Feature SHALL validate the Property_ID and Available_Property state on the server.
2. IF the Property_ID does not identify an existing Property, THEN THE Application_Submission_Feature SHALL create no Application_Record for the invalid Property_ID and display a Validation_Message for the unavailable property reference.
3. IF the selected Property is not an Available_Property when the Application_Review_Step is requested, THEN THE Application_Submission_Feature SHALL display a Validation_Message for selecting another available property with the Application_Record count unchanged.
4. WHEN the Submission_Service receives a Final_Submission_Request, THE Submission_Service SHALL revalidate authentication, student authorization, Property existence, Available_Property state, and the Duplicate_Rule before creating an Application_Record.
5. IF any final submission validation fails, THEN THE Submission_Service SHALL create no Application_Record for the selected Student_ID and Property_ID pair and return a validation-failure result.
6. IF a Final_Submission_Request contains client-supplied Student_ID, status, or application date values, THEN THE Submission_Service SHALL disregard those values and use server-controlled values.
7. IF the selected Property becomes unavailable after the Application_Review_Page is displayed and before the Final_Submission_Request is processed, THEN THE Submission_Service SHALL keep the Application_Record count unchanged and display a Validation_Message explaining the availability change.

### Requirement 5: Duplicate and Repeat Protection

**User Story:** As a student, I want repeated actions to be handled safely, so that double-clicks, refreshes, or retries do not create duplicate applications.

#### Acceptance Criteria

1. THE Submission_Service SHALL enforce the Duplicate_Rule on the server independently of Property_Detail_Page controls.
2. IF an Application_Record already exists for the Authenticated_Student's Student_ID and selected Property_ID in any status, THEN THE Submission_Service SHALL keep the Application_Record count unchanged and display a duplicate-submission message with a link to the My_Applications_Page.
3. WHEN two or more valid Final_Submission_Requests for the same Student_ID and Property_ID complete without a persistence error, THE Submission_Service SHALL produce a final database state containing exactly one Application_Record for that Student_ID and Property_ID pair.
4. IF every concurrent Final_Submission_Request for the same Student_ID and Property_ID fails to persist an Application_Record, THEN THE Application_Submission_Feature SHALL return a server-error result with no submission-success message.
5. WHEN an Authenticated_Student activates the final submission control more than once for the same Property, THE Submission_Service SHALL produce a final database state containing exactly one Application_Record for the Authenticated_Student's Student_ID and selected Property_ID pair.
6. WHEN an Authenticated_Student refreshes the My_Applications_Page after a Successful_Submission, THE Application_Submission_Feature SHALL keep the Application_Record count unchanged.
7. WHEN a previously completed Final_Submission_Request is retried, THE Submission_Service SHALL keep the Application_Record count unchanged and return the existing-application result.
8. WHERE an Application_Record for the Student_ID and Property_ID has Rejected, Accepted, or Pending status, THE Submission_Service SHALL apply the Duplicate_Rule to a subsequent Final_Submission_Request.

### Requirement 6: Clear Interaction and Result Feedback

**User Story:** As a student, I want clear feedback throughout submission, so that I know whether I am reviewing, submitting, successful, or blocked.

#### Acceptance Criteria

1. WHILE the Application_Review_Page is displayed, THE Application_Review_Page SHALL distinguish the non-submitted review state from the submitted Pending_Status state.
2. WHILE a Final_Submission_Request is awaiting a response, THE Application_Review_Page SHALL present a visible submission-in-progress state and prevent additional activation of the final submission control.
3. WHEN the My_Applications_Page displays the Application_Record created by a Successful_Submission, THE Application_Submission_Feature SHALL display one success message stating that the application is Pending.
4. IF submission validation fails, THEN THE Application_Review_Page SHALL display a Validation_Message without displaying a submission-success message.
5. IF the Duplicate_Rule blocks a Final_Submission_Request, THEN THE Application_Submission_Feature SHALL display an informational existing-application result rather than a new-submission success result.
6. IF a server error prevents completion of a Final_Submission_Request, THEN THE Application_Submission_Feature SHALL display a retry message with the Application_Record count unchanged for the failed transaction.
7. WHEN the Property_Detail_Page is displayed for a Property covered by the Duplicate_Rule, THE Property_Detail_Page SHALL replace the application-start control with an existing-application indicator and a link to the My_Applications_Page.

### Requirement 7: Existing Architecture and Data Compatibility

**User Story:** As a maintainer, I want the safer flow integrated with the current application structure, so that existing property and application functionality continues to operate consistently.

#### Acceptance Criteria

1. THE Application_Submission_Feature SHALL use the existing Spring Boot, Spring Security, Thymeleaf, and Spring Data JPA application architecture.
2. THE Application_Submission_Feature SHALL begin from the existing Property_Detail_Page and complete through the existing My_Applications_Page.
3. THE Submission_Service SHALL store a Successful_Submission as an Application_Record using the existing Student_ID, Property_ID, application date data fields, and Pending_Status.
4. THE Application_Submission_Feature SHALL represent the Application_Review_Step without storing a draft Application_Record or introducing a draft application status.
5. WHEN existing Pending, Accepted, or Rejected Application_Record entries are displayed on the My_Applications_Page, THE My_Applications_Page SHALL preserve the existing status display and document-submission behavior.
6. WHEN a Successful_Submission creates an Application_Record, THE existing landlord application-management functionality SHALL make the Pending Application_Record available through the current landlord workflow.
7. IF the landlord application-management functionality cannot display a created Pending Application_Record, THEN THE Application_Submission_Feature SHALL retain the Application_Record for display after the landlord integration issue is resolved.
