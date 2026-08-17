# Requirements Document

## Introduction

"Speak with AI" adds a floating chat assistant widget to the ULEE web application. The widget consists of a persistent icon, shown in the bottom-right corner of every page, and an expandable chat panel with message history and text input. On property detail pages the widget is aware of which property is being viewed; elsewhere it operates as a general assistant.

**Scope of this spec:** UI scaffolding and open/close/message interaction only. All assistant responses in this spec are placeholder/echo text generated locally in the browser or server-side template logic — no request is made to a real AI service. Integrating an actual AI backend is explicitly out of scope and will be covered by a separate, future spec.

**Assumptions made to keep requirements unambiguous (subject to user review):**
- "Persistent across navigation" means the widget's icon is present and in its default (closed) state on every server-rendered page load — this is a multi-page Thymeleaf application, not a single-page application, so full navigations reload the DOM. It does not mean conversation history is preserved across a full page navigation/reload in this spec; that would require session or client storage design not requested here.
- The widget appears on every page rendered by the application, including authenticated and unauthenticated pages, unless a future spec states otherwise.
- "Property detail page" refers to both the existing `/property/{id}` page (`property-detail.html`) and the `/property/{id}/vr-tour` page (`vr-tour.html`) — both routes represent viewing a specific property.

## Glossary

- **Speak_With_AI_Widget**: The overall floating chat assistant feature, comprising the Chat_Icon and the Chat_Panel, implemented as a single reusable Thymeleaf fragment included by page templates.
- **Chat_Icon**: The persistent, always-visible control shown in the bottom-right corner of the viewport that toggles the Chat_Panel.
- **Chat_Panel**: The expandable/collapsible surface containing the Message_History area and the Message_Input control.
- **Message_History**: The ordered list of messages (from the user and from the Assistant_Response_Engine) displayed within the Chat_Panel for the current page view.
- **Message_Input**: The text entry control within the Chat_Panel used to compose an outgoing message.
- **Assistant_Response_Engine**: The component responsible for producing a placeholder response to a submitted message in this spec. It does not call any external AI service.
- **Property_Detail_Page**: A page rendered by the existing `/property/{id}` route (`property-detail.html`) or the `/property/{id}/vr-tour` route (`vr-tour.html`), both of which display a single property's details — the latter as an interactive virtual tour.
- **Property_Context**: The mode of operation where the Assistant_Response_Engine has access to the Property_ID and Property_Title of the property shown on the current Property_Detail_Page.
- **General_Context**: The mode of operation used on any page that is not a Property_Detail_Page, or where property information is unavailable.
- **Property_ID**: The unique identifier of the property being viewed, as used by the existing property routes.
- **Property_Title**: The display title of the property being viewed.

## Requirements

### Requirement 1: Persistent Floating Chat Icon

**User Story:** As a visitor on any page of the site, I want to see a chat assistant icon in the same place at all times, so that I can get help without losing my place or navigating away.

#### Acceptance Criteria

1. THE Speak_With_AI_Widget SHALL be implemented as a single Thymeleaf fragment that page templates include, so that its markup and behavior are defined in exactly one place.
2. WHEN a page that includes the Speak_With_AI_Widget fragment is rendered, THE Chat_Icon SHALL be visible in the bottom-right corner of the viewport.
3. WHILE the Chat_Panel is closed, THE Chat_Icon SHALL remain anchored to the bottom-right corner of the viewport regardless of page scroll position.
4. WHEN the user navigates from one page to another page that also includes the Speak_With_AI_Widget fragment, THE Chat_Icon SHALL be visible again on the newly loaded page in its default closed state.

### Requirement 2: Opening and Closing the Chat Panel

**User Story:** As a user, I want to open and close the assistant panel without leaving the page I'm on, so that I can quickly get an answer and return to what I was doing.

#### Acceptance Criteria

1. WHEN the user activates the Chat_Icon while the Chat_Panel is closed, THE Chat_Panel SHALL expand into view using a slide-up/expand transition, without navigating to a different URL.
2. WHEN the user activates the Chat_Icon while the Chat_Panel is open, THE Chat_Panel SHALL collapse and hide.
3. THE Chat_Panel SHALL provide a dedicated close control separate from the Chat_Icon.
4. WHEN the user activates the dedicated close control, THE Chat_Panel SHALL collapse and hide.
5. IF the user presses the Escape key while the Chat_Panel is open, THEN THE Chat_Panel SHALL collapse and hide.
6. WHILE the Chat_Panel is open, THE Speak_With_AI_Widget SHALL keep the underlying page content loaded and unchanged, other than visually overlaying the Chat_Panel.

### Requirement 3: Message History and Input

**User Story:** As a user chatting with the assistant, I want to type a message and see the conversation so far, so that I can follow the exchange.

#### Acceptance Criteria

1. THE Chat_Panel SHALL display a Message_Input control that accepts free-text entry.
2. THE Chat_Panel SHALL display a Message_History area that lists messages in the order they were sent.
3. WHEN the user submits the Message_Input with non-empty, non-whitespace-only text, THE Chat_Panel SHALL append that text to the Message_History as a user message and clear the Message_Input.
4. IF the user submits the Message_Input with empty or whitespace-only text, THEN THE Chat_Panel SHALL NOT append a message to the Message_History.
5. WHEN the Chat_Panel is opened for the first time during the current page view, THE Chat_Panel SHALL display an initial assistant greeting message in the Message_History.
6. THE Message_History SHALL retain all messages sent during the current page view for as long as the page remains loaded.

### Requirement 4: Placeholder Assistant Response

**User Story:** As a user testing the assistant, I want to receive a reply after sending a message, so that I can verify the send/receive interaction works end-to-end before real AI is connected.

#### Acceptance Criteria

1. WHEN the user submits a user message to the Message_History, THE Assistant_Response_Engine SHALL append one placeholder response to the Message_History within 1 second.
2. THE Assistant_Response_Engine SHALL generate the placeholder response locally, without making a request to any external AI service.
3. THE Chat_Panel SHALL visually distinguish user messages from Assistant_Response_Engine messages within the Message_History.

### Requirement 5: Property Context Awareness

**User Story:** As a user viewing a specific property, I want the assistant to know which listing I'm looking at, so that once real AI is connected it can answer questions about that property.

#### Acceptance Criteria

1. WHILE the current page is a Property_Detail_Page, THE Speak_With_AI_Widget SHALL have access to the Property_ID and Property_Title of the property being viewed.
2. WHILE the current page is a Property_Detail_Page and the Property_Title is available, THE Speak_With_AI_Widget SHALL operate in Property_Context and the Chat_Panel SHALL display the Property_Title so the user can confirm which listing the assistant is aware of.
3. WHILE the current page is a Property_Detail_Page and the Property_Title is available, THE Assistant_Response_Engine SHALL include the Property_Title within its placeholder response, so the context wiring is verifiable end-to-end.
4. WHILE the current page is not a Property_Detail_Page, THE Speak_With_AI_Widget SHALL operate in General_Context and SHALL NOT reference a specific property in the Chat_Panel or in placeholder responses.
5. IF the current page is a Property_Detail_Page but the Property_Title is unavailable, THEN THE Speak_With_AI_Widget SHALL operate in General_Context.

### Requirement 6: Visual Consistency with the Hero Design System

**User Story:** As a user, I want the assistant icon and panel to look like they belong to the rest of the site, so that the experience feels consistent rather than bolted on.

#### Acceptance Criteria

1. WHERE a page renders using the hero design system, THE Chat_Icon and Chat_Panel SHALL use the same accent color, corner rounding, and font families as that page's other hero-styled controls.
2. THE Chat_Icon SHALL use the same accent color that the hero design system uses for its primary call-to-action buttons.
