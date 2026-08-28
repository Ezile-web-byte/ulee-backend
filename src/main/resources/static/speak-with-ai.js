// DOM wiring for the "Speak with AI" widget. Delegates all state-transition
// logic to window.SwaiLogic (speak-with-ai-logic.js), which must be loaded
// first. This script is loaded with `defer`, so it runs after the DOM
// (including the speak-with-ai fragment markup) is parsed — top-level code
// referencing #swai-* elements at load time is safe.
//
// Implements icon/close/Escape handling, the property-title-in-header
// behavior, message submission (blank-guard, clear/refocus input), the
// first-open greeting, the setTimeout-based placeholder assistant reply,
// rendering of messages into #swai-history, and — new — the category-guided
// listing matcher (General_Context only): root/rent/roomType/commute chips
// from SwaiLogic.CATEGORIES, a fetch of GET /api/listings, and result cards
// with an Apply link — all within a single IIFE sharing the `context`,
// `isOpen`, `messages`, and DOM element variables declared below.
(function () {
  'use strict';

  var root = document.getElementById('swai-root');
  var icon = document.getElementById('swai-icon');
  var panel = document.getElementById('swai-panel');
  var title = document.getElementById('swai-title');
  var closeBtn = document.getElementById('swai-close');

  // Message history/form elements, wired up below alongside the icon/close
  // handling declared earlier in this closure.
  var historyEl = document.getElementById('swai-history');
  var form = document.getElementById('swai-form');
  var input = document.getElementById('swai-input');

  if (!root || !icon || !panel || !title || !closeBtn) {
    // The fragment isn't present on this page (or markup changed) — nothing
    // to wire up.
    return;
  }

  /**
   * A dataset value counts as "present" only when it exists and isn't one of
   * the placeholder strings a null/absent Thymeleaf model value can render
   * as ("null", "undefined") or an empty string.
   */
  function isPresent(value) {
    return value != null && value !== '' && value !== 'null' && value !== 'undefined';
  }

  var rawPropertyId = root.dataset.propertyId;
  var rawPropertyTitle = root.dataset.propertyTitle;
  var rawPropertyAddress = root.dataset.propertyAddress;
  var rawPropertyCommuteType = root.dataset.propertyCommuteType;
  var rawPropertyFeatures = root.dataset.propertyFeatures;

  var isPropertyPage = isPresent(rawPropertyId) && isPresent(rawPropertyTitle);

  // Resolved once per page load. Used to build the greeting and the
  // placeholder assistant reply.
  var context = window.SwaiLogic.resolveContext({
    isPropertyPage: isPropertyPage,
    propertyId: rawPropertyId,
    propertyTitle: rawPropertyTitle,
    propertyAddress: isPresent(rawPropertyAddress) ? rawPropertyAddress : null,
    propertyCommuteType: isPresent(rawPropertyCommuteType) ? rawPropertyCommuteType : null,
    propertyFeatures: isPresent(rawPropertyFeatures) ? rawPropertyFeatures : null
  });

  if (context.mode === 'property') {
    // Requirement 5.2: panel header shows the property title in Property_Context.
    title.textContent = context.propertyTitle;
  }

  // Base URL used to build the "View property" link on a result card.
  // Override per-page with data-property-base-url="/some/other/path/" on
  // #swai-root if this ever needs to point somewhere other than the
  // standard /property/{id} detail route (PropertyController#viewPropertyDetail).
  var propertyBaseUrl = root.dataset.propertyBaseUrl || '/property/';

  // Panel open/closed state. Starts closed, matching the `hidden` attribute
  // already present on #swai-panel in the markup.
  var isOpen = false;

  // Message history for the current page view only (Requirement 3.6). Never
  // written to localStorage/sessionStorage/a cookie, so it does not survive
  // a full page navigation — matching the requirements doc's explicit
  // assumption.
  var messages = [];

  // Guards the one-time-per-page-view greeting (Requirement 3.5).
  var hasGreeted = false;

  // Lazily-fetched, cached for the life of the page view so re-opening the
  // category tree doesn't re-hit the network every time.
  var listingsPromise = null;

  function fetchListings() {
    if (listingsPromise) return listingsPromise;
    listingsPromise = fetch('/api/listings')
        .then(function (res) {
          if (!res.ok) throw new Error('bad status ' + res.status);
          return res.json();
        })
        .catch(function () {
          // No backend reachable (static preview, offline, endpoint not yet
          // deployed) — fall back to sample data so the widget still works.
          return window.SwaiLogic.SAMPLE_LISTINGS;
        });
    return listingsPromise;
  }

  /**
   * Builds a single message's DOM element and appends it to #swai-history,
   * then scrolls the history so the newest message is visible. Sender gets
   * a dedicated class (Requirement 4.3) so speak-with-ai.css can style user
   * vs. assistant messages differently.
   */
  function renderMessage(message) {
    var el = document.createElement('div');
    el.className = 'swai-msg ' + (message.sender === 'user' ? 'swai-msg-user' : 'swai-msg-assistant');
    el.textContent = message.text;
    historyEl.appendChild(el);
    historyEl.scrollTop = historyEl.scrollHeight;
  }

  /**
   * Builds a message object, appends it to the closure-scoped `messages`
   * array via SwaiLogic.appendMessage (Requirement 3.2, 3.6), and renders it.
   */
  function addMessage(sender, text) {
    var message = {
      id: Date.now() + '-' + Math.random(),
      sender: sender,
      text: text,
      ts: Date.now()
    };
    messages = window.SwaiLogic.appendMessage(messages, message);
    renderMessage(message);
  }

  // ---- Category chips (General_Context — browsing/matching) --------

  function renderCategory(key) {
    var category = window.SwaiLogic.getCategory(key);
    if (!category) return;

    addMessage('assistant', category.prompt);

    var row = document.createElement('div');
    row.className = 'swai-chip-row';

    category.options.forEach(function (option) {
      var chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'swai-chip';
      chip.textContent = option.label;
      chip.addEventListener('click', function () {
        Array.prototype.forEach.call(row.children, function (c) { c.disabled = true; });
        addMessage('user', option.label);

        if (option.next) {
          setTimeout(function () { renderCategory(option.next); }, 250);
        } else {
          setTimeout(function () { runMatch(option); }, 250);
        }
      });
      row.appendChild(chip);
    });

    historyEl.appendChild(row);
    historyEl.scrollTop = historyEl.scrollHeight;
  }

  function runMatch(option) {
    fetchListings().then(function (listings) {
      var matches = window.SwaiLogic.matchListings(listings, option);

      if (matches.length === 0) {
        addMessage('assistant', 'No approved listings match "' + option.label + '" right now — try another option below.');
        setTimeout(function () { renderCategory('root'); }, 250);
        return;
      }

      addMessage('assistant', 'Found ' + matches.length + ' approved listing' + (matches.length > 1 ? 's' : '') + ' matching "' + option.label + '":');
      matches.forEach(renderResultCard);

      setTimeout(function () {
        addMessage('assistant', 'Want to narrow it down further?');
        renderCategory('root');
      }, 300);
    });
  }

  function renderResultCard(listing) {
    var card = document.createElement('div');
    card.className = 'swai-result';

    var thumb = document.createElement('div');
    thumb.className = 'swai-result-thumb';
    if (listing.imageUrl) {
      var img = document.createElement('img');
      img.src = listing.imageUrl;
      img.alt = listing.title || '';
      thumb.appendChild(img);
    }

    var body = document.createElement('div');
    body.className = 'swai-result-body';

    var name = document.createElement('p');
    name.className = 'swai-result-name';
    name.textContent = listing.title || 'Untitled listing';

    var loc = document.createElement('p');
    loc.className = 'swai-result-loc';
    loc.textContent = [listing.address, listing.city].filter(Boolean).join(', ');

    var tags = document.createElement('div');
    tags.className = 'swai-result-tags';
    window.SwaiLogic.buildMatchTags(listing).forEach(function (t) {
      var tag = document.createElement('span');
      tag.textContent = t;
      tags.appendChild(tag);
    });

    var applyLink = document.createElement('a');
    applyLink.className = 'swai-result-apply';
    applyLink.href = propertyBaseUrl + listing.id;
    applyLink.textContent = 'View & Apply';

    body.appendChild(name);
    body.appendChild(loc);
    body.appendChild(tags);
    body.appendChild(applyLink);

    card.appendChild(thumb);
    card.appendChild(body);
    historyEl.appendChild(card);
    historyEl.scrollTop = historyEl.scrollHeight;
  }

  // ---- Category chips (Property_Context — answers about THIS listing) ----

  function renderPropertyCategory(key) {
    var category = window.SwaiLogic.PROPERTY_CATEGORIES[key];
    if (!category) return;

    addMessage('assistant', category.prompt);

    var row = document.createElement('div');
    row.className = 'swai-chip-row';

    category.options.forEach(function (option) {
      var chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'swai-chip';
      chip.textContent = option.label;
      chip.addEventListener('click', function () {
        Array.prototype.forEach.call(row.children, function (c) { c.disabled = true; });
        addMessage('user', option.label);
        setTimeout(function () {
          var answer = window.SwaiLogic.buildPropertyCategoryAnswer(option.key, context);
          addMessage('assistant', answer);
          setTimeout(function () { renderPropertyCategory('root'); }, 300);
        }, 250);
      });
      row.appendChild(chip);
    });

    historyEl.appendChild(row);
    historyEl.scrollTop = historyEl.scrollHeight;
  }

  // ---- Greeting -------------------------------------------------------

  function buildGreeting() {
    if (context.mode === 'property') {
      return 'Hi! I\'m here to help with questions about "' + context.propertyTitle + '".';
    }
    return 'Hi! I\'m your ULEE assistant. Ask me anything about browsing or applying.';
  }

  function applyOpenDom() {
    panel.removeAttribute('hidden');
    panel.setAttribute('aria-hidden', 'false');
    icon.setAttribute('aria-expanded', 'true');
  }

  function applyClosedDom() {
    panel.setAttribute('hidden', '');
    panel.setAttribute('aria-hidden', 'true');
    icon.setAttribute('aria-expanded', 'false');
  }

  icon.addEventListener('click', function () {
    isOpen = window.SwaiLogic.toggleOpen(isOpen);
    if (isOpen) {
      applyOpenDom();
      // Requirement 3.5: greet exactly once per page view, on the
      // transition into the open state — not on every open.
      if (!hasGreeted) {
        addMessage('assistant', buildGreeting());
        hasGreeted = true;
        // Category chips: browsing assistant gets the rent/type/commute
        // matcher; a property page gets questions about THIS listing only.
        if (context.mode === 'general') {
          setTimeout(function () { renderCategory('root'); }, 250);
        } else if (context.mode === 'property') {
          setTimeout(function () { renderPropertyCategory('root'); }, 250);
        }
      }
    } else {
      applyClosedDom();
    }
  });

  closeBtn.addEventListener('click', function () {
    // Requirement 2.4: the close button always forces the panel closed,
    // regardless of current contents — never just toggles.
    isOpen = false;
    applyClosedDom();
  });

  document.addEventListener('keydown', function (event) {
    // Requirement 2.5: Escape forces the panel closed while open; a no-op
    // when the panel is already closed.
    var key = event.key;
    if ((key === 'Escape' || key === 'Esc') && isOpen) {
      isOpen = false;
      applyClosedDom();
    }
  });

  if (form && input) {
    form.addEventListener('submit', function (event) {
      // Prevent a real form POST/page navigation.
      event.preventDefault();

      var value = input.value;

      if (window.SwaiLogic.isBlank(value)) {
        // Requirement 3.4: blank/whitespace-only submissions do nothing.
        return;
      }

      // Requirements 3.2, 3.3: append the user message, clear and refocus
      // the input.
      addMessage('user', value);
      input.value = '';
      input.focus();

      // Requirements 4.1, 4.2: reply locally, well under 1 second, with no
      // network call of any kind.
      setTimeout(function () {
        var reply = window.SwaiLogic.buildPlaceholderResponse(value, context);
        addMessage('assistant', reply);
      }, 700);
    });
  }
})();