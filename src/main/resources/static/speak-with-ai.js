// DOM wiring for the "Speak with AI" widget. Delegates all state-transition
// logic to window.SwaiLogic (speak-with-ai-logic.js), which must be loaded
// first. This script is loaded with `defer`, so it runs after the DOM
// (including the speak-with-ai fragment markup) is parsed — top-level code
// referencing #swai-* elements at load time is safe.
//
// Implements icon/close/Escape handling, the property-title-in-header
// behavior, message submission (blank-guard, clear/refocus input), the
// first-open greeting, AI replies via POST /api/chat, rendering of messages
// into #swai-history, and the category-guided listing matcher (General_Context
// only): root/rent/roomType/commute chips from SwaiLogic.CATEGORIES, a fetch
// of GET /api/listings, and result cards with an Apply link — all within a
// single IIFE sharing the `context`, `isOpen`, `messages`, and DOM element
// variables declared below.
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

    // Resolved once per page load. Used to build the greeting and sent to the
    // backend so the AI knows which property (if any) the student is viewing.
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

    // Bumped every time the chat is reset. Timers and in-flight AI replies that were
    // started before a reset check it and drop their result, so nothing "appears
    // out of nowhere" in a cleared chat.
    var epoch = 0;

    function later(fn, ms) {
        var e = epoch;
        setTimeout(function () { if (e === epoch) fn(); }, ms);
    }

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

    /** Matches the "/property/12" paths the AI writes when it recommends a listing. */
    var LISTING_PATH_RE = /\/property\/(\d+)/g;

    /** Up to 3 distinct listing ids mentioned in an AI message. */
    function listingIdsIn(text) {
        var ids = [];
        var m;
        LISTING_PATH_RE.lastIndex = 0;
        while ((m = LISTING_PATH_RE.exec(String(text))) !== null) {
            if (ids.indexOf(m[1]) === -1) ids.push(m[1]);
        }
        return ids.slice(0, 3);
    }

    /** Removes "/property/12" paths (and the empty brackets left behind) from AI text. */
    function stripListingPaths(text) {
        return String(text)
            .replace(/\s*[\(\[]\s*\/property\/\d+\s*[\)\]]/g, '')
            .replace(/\s*\/property\/\d+/g, '')
            .replace(/[ \t]{2,}/g, ' ')
            .trim();
    }

    /** Plain "View & Apply" button, used if a mentioned listing isn't in the listings data. */
    function buildListingLink(id) {
        var wrap = document.createElement('div');
        wrap.style.margin = '4px 8px 10px';
        var a = document.createElement('a');
        a.className = 'swai-result-apply';
        a.href = propertyBaseUrl + id;
        a.textContent = 'View & Apply';
        wrap.appendChild(a);
        return wrap;
    }

    /** Inserts a listing card directly under `afterEl` for each listing id the AI mentioned. */
    function appendMentionedCards(afterEl, ids) {
        fetchListings().then(function (listings) {
            var anchor = afterEl;
            ids.forEach(function (id) {
                var found = null;
                (listings || []).forEach(function (l) {
                    if (String(l.id) === String(id)) found = l;
                });
                var node = found ? buildResultCard(found) : buildListingLink(id);
                if (!anchor.parentNode) return;
                anchor.parentNode.insertBefore(node, anchor.nextSibling);
                anchor = node;
            });
            historyEl.scrollTop = historyEl.scrollHeight;
        });
    }

    /**
     * Builds a single message's DOM element and appends it to #swai-history,
     * then scrolls the history so the newest message is visible. Sender gets
     * a dedicated class (Requirement 4.3) so speak-with-ai.css can style user
     * vs. assistant messages differently. Text is set with textContent (never
     * innerHTML), so model output can't inject markup. Listings the AI mentions
     * appear as cards under the message instead of as raw "/property/12" text.
     */
    function renderMessage(message) {
        var el = document.createElement('div');
        el.className = 'swai-msg ' + (message.sender === 'user' ? 'swai-msg-user' : 'swai-msg-assistant');

        var ids = [];
        if (message.sender === 'user') {
            el.textContent = message.text;
        } else {
            ids = listingIdsIn(message.text);
            el.textContent = ids.length ? stripListingPaths(message.text) : message.text;
        }

        historyEl.appendChild(el);
        historyEl.scrollTop = historyEl.scrollHeight;
        if (ids.length) appendMentionedCards(el, ids);
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
        remember({ type: 'msg', sender: sender, text: text });
    }

    // ---- Keep the conversation when the visitor changes page ------------
    // Saved in sessionStorage: it survives navigating to a listing and coming
    // back (same browser tab), and is wiped when the tab is closed.

    var STORAGE_KEY = 'swaiChat.v1';
    var MAX_ENTRIES = 80;
    var transcript = [];     // [{type:'msg', sender, text} | {type:'card', listing}]
    var restoring = false;

    function currentCtxId() {
        return context.mode === 'property' ? 'property:' + context.propertyId : 'general';
    }

    function clearSavedState() {
        try { sessionStorage.removeItem(STORAGE_KEY); } catch (e) { /* ignore */ }
    }

    function saveState() {
        if (restoring) return;
        if (transcript.length === 0) { clearSavedState(); return; }
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
                entries: transcript,
                open: isOpen,
                ctx: currentCtxId()
            }));
        } catch (e) { /* storage blocked or full: the chat just won't persist */ }
    }

    function loadState() {
        try {
            var raw = sessionStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function remember(entry) {
        if (restoring) return;
        transcript.push(entry);
        if (transcript.length > MAX_ENTRIES) transcript.shift();
        saveState();
    }

    function slimListing(l) {
        return {
            id: l.id, title: l.title, address: l.address, city: l.city,
            rent: l.rent, type: l.type, commuteType: l.commuteType, imageUrl: l.imageUrl || null
        };
    }

    /** Fresh quick-reply chips shown after a restore (the old chip rows can't be saved). */
    function renderMenuChips() {
        if (context.mode === 'general') {
            var root = window.SwaiLogic.getCategory('root');
            renderChipRow(root.options.map(function (o) {
                return { label: o.label, action: function () { renderCategory(o.next); } };
            }));
        } else if (context.mode === 'property') {
            renderPropertyCategory('root', true);
        }
    }

    // ---- Category chips (General_Context — browsing/matching) --------
    // Answers ACCUMULATE: each choice narrows the same result set
    // (e.g. Under R3 000 + Sharing + Walking distance).

    var filters = [];   // [{ category: 'rent', option: {...} }, ...]

    function usedCategoryKeys() {
        return filters.map(function (f) { return f.category; });
    }

    /** Renders a row of chips. Each item is { label, action }; clicking echoes the label as the user's message. */
    function renderChipRow(items) {
        var row = document.createElement('div');
        row.className = 'swai-chip-row';

        items.forEach(function (item) {
            var chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'swai-chip';
            chip.textContent = item.label;
            chip.addEventListener('click', function () {
                Array.prototype.forEach.call(row.children, function (c) { c.disabled = true; });
                addMessage('user', item.label);
                later(item.action, 250);
            });
            row.appendChild(chip);
        });

        historyEl.appendChild(row);
        historyEl.scrollTop = historyEl.scrollHeight;
    }

    function startOverItem() {
        return {
            label: '🔄 Start over',
            action: function () { filters = []; renderCategory('root'); }
        };
    }

    // State for the AI-powered chips below.
    var lastMatches = [];     // listings from the most recent search
    var helpUsed = false;     // "Help me choose" is offered once per result set

    function filterSummary() {
        return filters.map(function (f) { return f.option.label; }).join(' + ');
    }

    /** A chip that sends a ready-made question to the AI, shows its answer, then runs `after`. */
    function aiItem(label, getPrompt, after) {
        return {
            label: label,
            action: function () {
                askAssistant(getPrompt()).then(function (shown) { if (shown) after(); });
            }
        };
    }

    function buildHelpPrompt() {
        var names = lastMatches.slice(0, 6).map(function (l) { return l.title; }).join(', ');
        return 'I filtered by: ' + filterSummary() + '. These ULEE listings match: ' + names +
            '. Briefly compare them and tell me which one suits a student best, and why.';
    }

    function buildAlternativesPrompt() {
        return 'I filtered by: ' + filterSummary() + ' but no ULEE listing matches. ' +
            'Looking at the current ULEE listings, which are the closest alternatives? ' +
            'Say briefly what is different (price, room type or getting to campus).';
    }

    // skipPrompt: the greeting already asked the question, so only show the chips.
    function renderCategory(key, skipPrompt) {
        var category = window.SwaiLogic.getCategory(key);
        if (!category) return;

        var prompt = category.prompt;
        var items;

        if (key === 'root') {
            var used = usedCategoryKeys();
            var remaining = category.options.filter(function (o) { return used.indexOf(o.next) === -1; });

            if (used.length === 0) {
                prompt = category.prompt;
            } else if (remaining.length > 0) {
                prompt = 'Want to narrow it down further?';
            } else {
                prompt = 'That covers every filter. What next?';
            }

            items = remaining.map(function (o) {
                return { label: o.label, action: function () { renderCategory(o.next); } };
            });
            if (used.length > 0) {
                if (lastMatches.length > 1 && !helpUsed) {
                    items.push({
                        label: '💡 Help me choose',
                        action: function () {
                            helpUsed = true;
                            askAssistant(buildHelpPrompt()).then(function (shown) { if (shown) renderCategory('root'); });
                        }
                    });
                }
                items.push(startOverItem());
            }
        } else {
            items = category.options.map(function (option) {
                return {
                    label: option.label,
                    action: function () {
                        filters.push({ category: key, option: option });
                        runMatch();
                    }
                };
            });
            items.push({ label: '← Back', action: function () { renderCategory('root'); } });
        }

        if (!skipPrompt) addMessage('assistant', prompt);
        renderChipRow(items);
    }

    function runMatch() {
        fetchListings().then(function (listings) {
            var options = filters.map(function (f) { return f.option; });
            var matches = window.SwaiLogic.matchAll(listings, options);
            var summary = filters.map(function (f) { return f.option.label; }).join(' + ');
            lastMatches = matches;
            helpUsed = false;

            // Results found: show them, then offer ways to narrow further.
            if (matches.length > 0) {
                addMessage('assistant', 'Found ' + matches.length + ' available listing' + (matches.length > 1 ? 's' : '') + ' for ' + summary + ':');
                matches.forEach(renderResultCard);
                later(function () { renderCategory('root'); }, 300);
                return;
            }

            // Nothing found: explain, then offer useful next steps (not the whole menu again).
            var items = [];

            if (filters.length > 1) {
                var last = filters[filters.length - 1];
                addMessage('assistant', 'Nothing available matches ' + summary + ' together.');
                items.push({
                    label: '↩️ Drop "' + last.option.label + '"',
                    action: function () { filters.pop(); runMatch(); }
                });
            } else {
                var only = filters[0];
                addMessage('assistant', 'No available listings for ' + only.option.label + ' right now. Want to try another one?');
                window.SwaiLogic.getCategory(only.category).options.forEach(function (o) {
                    if (o !== only.option) {
                        items.push({
                            label: o.label,
                            action: function () { filters = [{ category: only.category, option: o }]; runMatch(); }
                        });
                    }
                });
            }

            var withoutAi = items.concat([startOverItem()]);
            renderChipRow(withoutAi.concat([
                aiItem('💡 Suggest alternatives', buildAlternativesPrompt, function () { renderChipRow(withoutAi); })
            ]));
        });
    }

    function buildResultCard(listing) {
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
        return card;
    }

    /** Appends a search-result card to the chat and remembers it for restore. */
    function renderResultCard(listing) {
        historyEl.appendChild(buildResultCard(listing));
        historyEl.scrollTop = historyEl.scrollHeight;
        remember({ type: 'card', listing: slimListing(listing) });
    }

    // ---- Category chips (Property_Context — answers about THIS listing) ----

    // skipPrompt: the greeting already asked the question, so only show the chips.
    /**
     * Keys of the property chips the visitor has already tapped (read from the saved
     * conversation, so it also works after a restore). Counting stops at a "Start over"
     * marker, or at the "You're now viewing" message (they moved to a different listing).
     */
    function usedPropertyKeys() {
        var options = window.SwaiLogic.PROPERTY_CATEGORIES.root.options;
        var used = [];
        for (var i = transcript.length - 1; i >= 0; i--) {
            var e = transcript[i];
            if (e.type === 'reset') break;
            if (e.type !== 'msg') continue;
            if (e.sender !== 'user') {
                if (typeof e.text === 'string' && e.text.indexOf('You\'re now viewing') === 0) break;
                continue;
            }
            options.forEach(function (o) {
                if (o.label === e.text && used.indexOf(o.key) === -1) used.push(o.key);
            });
        }
        return used;
    }

    function renderPropertyCategory(key, skipPrompt) {
        var category = window.SwaiLogic.PROPERTY_CATEGORIES[key];
        if (!category) return;

        // Chips already answered are removed, so the same question isn't offered twice.
        var used = usedPropertyKeys();
        var remaining = category.options.filter(function (o) { return used.indexOf(o.key) === -1; });

        if (remaining.length === 0) {
            if (!skipPrompt) {
                addMessage('assistant', 'That covers everything I have on this property. Type a question below if you want to know more, or start over.');
            }
            renderChipRow([{
                label: '🔄 Start over',
                action: function () {
                    remember({ type: 'reset' });   // forget which chips were used
                    renderPropertyCategory('root');
                }
            }]);
            return;
        }

        if (!skipPrompt) addMessage('assistant', category.prompt);

        var row = document.createElement('div');
        row.className = 'swai-chip-row';

        remaining.forEach(function (option) {
            var chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'swai-chip';
            chip.textContent = option.label;
            chip.addEventListener('click', function () {
                Array.prototype.forEach.call(row.children, function (c) { c.disabled = true; });
                addMessage('user', option.label);
                later(function () {
                    var answer = window.SwaiLogic.buildPropertyCategoryAnswer(option.key, context);
                    addMessage('assistant', answer);
                    later(function () { renderPropertyCategory('root'); }, 300);
                }, 250);
            });
            row.appendChild(chip);
        });

        historyEl.appendChild(row);
        historyEl.scrollTop = historyEl.scrollHeight;
    }

    // ---- Hello bubble (teaser) ---------------------------------------
    // Always visible next to the chat icon while the chat is closed. It is
    // hidden only while the chat panel is open, and comes back when the panel
    // is closed again (and on every page load / refresh).

    var teaser = document.getElementById('swai-teaser');

    function showTeaser() {
        if (teaser) teaser.hidden = false;
    }

    function hideTeaser() {
        if (teaser) teaser.hidden = true;
    }

    if (teaser) {
        showTeaser();
        teaser.addEventListener('click', function () {
            if (!isOpen) icon.click();   // clicking the bubble opens the chat
        });
    }

    // ---- Greeting -------------------------------------------------------

    function buildGreeting() {
        if (context.mode === 'property') {
            return 'Hi! I\'m here to help with questions about "' + context.propertyTitle + '". Type a question or tap an option below, and you can also ask about the area around it. What would you like to know about this property?';
        }
        return 'Hi! I\'m your ULEE assistant. Type your question or tap what you need below. I can help with rooms, applying, and life around campus.';
    }

    /** Clears the whole conversation and closes the window. The next open starts over. */
    function resetChat() {
        epoch++;
        messages = [];
        transcript = [];
        filters = [];
        lastMatches = [];
        helpUsed = false;
        hasGreeted = false;
        isOpen = false;
        while (historyEl.firstChild) historyEl.removeChild(historyEl.firstChild);
        clearSavedState();
        applyClosedDom();
        if (form) {
            var btn = form.querySelector('.swai-send');
            if (btn) btn.disabled = false;
        }
    }

    function applyOpenDom() {
        hideTeaser();
        panel.removeAttribute('hidden');
        panel.setAttribute('aria-hidden', 'false');
        icon.setAttribute('aria-expanded', 'true');
        historyEl.scrollTop = historyEl.scrollHeight;
        saveState();
    }

    function applyClosedDom() {
        showTeaser();
        panel.setAttribute('hidden', '');
        panel.setAttribute('aria-hidden', 'true');
        icon.setAttribute('aria-expanded', 'false');
        saveState();
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
                    later(function () { renderCategory('root', true); }, 250);
                } else if (context.mode === 'property') {
                    later(function () { renderPropertyCategory('root', true); }, 250);
                }
            }
        } else {
            resetChat();
        }
    });

    closeBtn.addEventListener('click', function () {
        // Requirement 2.4: the close button always forces the panel closed,
        // regardless of current contents — never just toggles.
        resetChat();
    });

    document.addEventListener('keydown', function (event) {
        // Requirement 2.5: Escape forces the panel closed while open; a no-op
        // when the panel is already closed.
        var key = event.key;
        if ((key === 'Escape' || key === 'Esc') && isOpen) {
            resetChat();
        }
    });

    // ---- Free-text questions -> AI backend -------------------------------

    /** Sends the message to POST /api/chat and shows the reply. Returns a promise. */
    function askAssistant(text) {
        // Plain-text turns only (chip rows and result cards aren't in `messages`
        // as anything but their text). Exclude the message just added; the
        // backend receives it separately as `message`.
        var myEpoch = epoch;
        var history = messages.slice(0, -1).map(function (m) {
            return { role: m.sender === 'user' ? 'user' : 'assistant', content: m.text };
        });

        // Temporary "typing" bubble
        var typing = document.createElement('div');
        typing.className = 'swai-msg swai-msg-assistant';
        typing.textContent = '…';
        historyEl.appendChild(typing);
        historyEl.scrollTop = historyEl.scrollHeight;

        var headers = { 'Content-Type': 'application/json' };
        // If Spring Security CSRF is on, add these meta tags to your layout:
        //   <meta name="_csrf" th:content="${_csrf.token}"/>
        //   <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
        var csrfToken = document.querySelector('meta[name="_csrf"]');
        var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
        if (csrfToken && csrfHeader) headers[csrfHeader.content] = csrfToken.content;

        var isProperty = context.mode === 'property';

        return fetch('/api/chat', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                message: text,
                history: history,
                propertyTitle: isProperty ? context.propertyTitle : null,
                propertyAddress: isProperty ? context.propertyAddress : null,
                propertyCommuteType: isProperty ? context.propertyCommuteType : null,
                propertyFeatures: isProperty ? context.propertyFeatures : null
            })
        })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                return (data && data.reply) || 'Sorry, I couldn\'t come up with an answer.';
            })
            .catch(function () {
                return 'Sorry, I can\'t reach the assistant right now. Please try again.';
            })
            .then(function (reply) {
                typing.remove();
                if (myEpoch !== epoch) return false;   // chat was closed/cleared while waiting
                addMessage('assistant', reply);
                return true;
            });
    }

    if (input) input.setAttribute('maxlength', '300');

    if (form && input) {
        var sendBtn = form.querySelector('.swai-send');

        form.addEventListener('submit', function (event) {
            // Prevent a real form POST/page navigation.
            event.preventDefault();

            var value = input.value;

            if (window.SwaiLogic.isBlank(value)) {
                // Requirement 3.4: blank/whitespace-only submissions do nothing.
                return;
            }

            // One request at a time.
            if (sendBtn && sendBtn.disabled) return;

            // Requirements 3.2, 3.3: append the user message, clear and refocus
            // the input.
            addMessage('user', value);
            input.value = '';
            input.focus();

            if (sendBtn) sendBtn.disabled = true;
            askAssistant(value).then(function () {
                if (sendBtn) sendBtn.disabled = false;
            });
        });
    }
    // ---- When does the chat start fresh? -----------------------------------
    //   * the page is refreshed                      -> cleared
    //   * the chat is closed (x, Escape, icon)       -> cleared (see resetChat)
    //   * any page other than a property page loads  -> cleared and closed. This covers
    //     "Back to Dashboard" and logging in (login always lands on a dashboard).
    //   * dashboard -> property page                 -> the conversation carries over, so the
    //     assistant can keep helping with that listing.

    function isPageReload() {
        try {
            var nav = performance.getEntriesByType && performance.getEntriesByType('navigation');
            if (nav && nav.length) return nav[0].type === 'reload';
            if (performance.navigation) return performance.navigation.type === 1;
        } catch (e) { /* ignore */ }
        return false;
    }

    (function restoreConversation() {
        if (isPageReload() || context.mode !== 'property') {
            clearSavedState();
            return;
        }

        var saved = loadState();
        if (!saved || !Array.isArray(saved.entries) || saved.entries.length === 0) return;

        restoring = true;
        saved.entries.forEach(function (entry) {
            if (entry.type === 'msg' && typeof entry.text === 'string') {
                var message = {
                    id: Date.now() + '-' + Math.random(),
                    sender: entry.sender === 'user' ? 'user' : 'assistant',
                    text: entry.text,
                    ts: Date.now()
                };
                messages = window.SwaiLogic.appendMessage(messages, message);
                renderMessage(message);
            } else if (entry.type === 'card' && entry.listing) {
                historyEl.appendChild(buildResultCard(entry.listing));
            }
        });
        restoring = false;

        transcript = saved.entries.slice(-MAX_ENTRIES);
        hasGreeted = true;   // never greet again mid-conversation

        // Arrived from the dashboard (or another listing): say what we're looking at now.
        if (saved.ctx !== currentCtxId()) {
            addMessage('assistant', 'You\'re now viewing "' + context.propertyTitle + '". Ask me anything about it, or about getting around from here.');
        }

        renderMenuChips();

        if (saved.open) {
            isOpen = true;
            applyOpenDom();
        }
    })();

    // Browser "Back" can restore a page from memory without reloading it: if that page is the
    // dashboard, start fresh there too.
    window.addEventListener('pageshow', function (event) {
        if (event.persisted && context.mode !== 'property') resetChat();
    });
})();