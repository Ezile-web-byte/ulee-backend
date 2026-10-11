// Required-field validation for Save Changes on edit-property.html.
// Extracted from an inline <script> block.
//
// "Save Changes" is BLOCKED entirely if any required field is missing
// or invalid — the click is cancelled, a popup lists what's missing, and
// once it's closed the landlord is jumped to the step containing the first
// unanswered required field, which is highlighted red and scrolled into
// view. "Save Draft" (elsewhere on this page) skips the required-field
// validation — it always saves whatever's filled in.
//
// Both Save Changes and Save Draft ask the server whether the title is
// already used by another property, and show a popup if it is. The page
// also shows a popup when the server bounces a save back with ?error=...
//
// NEW in this version:
//  - Pressing Enter inside a text box / dropdown no longer submits the form
//    (it used to "click" the first photo's delete button).
//  - Deleting a photo returns you to the same scroll position.
//  - The page remembers which property you were editing, so My Properties
//    can scroll back to it (see landlord-index.html).
(function () {
    const REQUIRED_FIELDS_EDIT = [
        { name: 'title', label: 'Property Title', step: 1, validate: v => !!(v && v.trim().length > 0) },
        { name: 'capacity', label: 'Capacity', step: 1, validate: v => !!(v && Number(v) >= 1) },
        { name: 'rent', label: 'Monthly Rent', step: 2, validate: v => !!(v && Number(v) > 0) },
        { name: 'address', label: 'Full Address', step: 2, validate: v => !!(v && v.trim().length > 0) }
    ];

    // What to tell the landlord when the server rejects a save (?error=...).
    const SERVER_ERRORS = {
        'duplicate-title': { title: 'That property name is already taken', message: 'Every property needs its own name. Please choose a different title. Nothing was saved.', field: 'title' },
        'title': { title: 'Property title needed', message: 'Please enter a title before saving.', field: 'title' },
        'capacity': { title: 'Capacity needed', message: 'Enter how many students this listing can hold (at least 1).', field: 'capacity' },
        'rent-missing': { title: 'Monthly rent needed', message: 'Please enter the monthly rent before saving.', field: 'rent' },
        'rent-too-low': { title: 'Rent is too low', message: 'Monthly rent must be at least R1 000 to save changes. You can use Save Draft to keep your progress.', field: 'rent' },
        'rent-invalid': { title: 'Rent is not a valid number', message: 'Please enter the monthly rent as a number.', field: 'rent' },
        'address': { title: 'Address needed', message: 'Please enter the full address before saving.', field: 'address' }
    };

    // sessionStorage keys (shared with landlord-index.html for LAST_PROPERTY_KEY).
    const SCROLL_KEY = 'ulee:editScroll';
    const LAST_PROPERTY_KEY = 'ulee:lastProperty';

    function fieldEl(name) {
        return document.querySelector(`#editPropertyForm [name="${name}"]`);
    }

    function getInvalidFields() {
        return REQUIRED_FIELDS_EDIT.filter(f => {
            const el = fieldEl(f.name);
            const val = el ? el.value : '';
            return !f.validate(val);
        });
    }

    function highlightInvalid(fields) {
        document.querySelectorAll('#editPropertyForm .field-invalid').forEach(el => el.classList.remove('field-invalid'));
        fields.forEach(f => {
            const el = fieldEl(f.name);
            if (el) el.classList.add('field-invalid');
        });
    }

    // Reveals the given step. Tries the REAL step-tracker's own click
    // handler first (whatever edit-property.js wired it to), so the
    // page's internal "current step" state stays in sync instead of
    // this script fighting it. Falls back to toggling the .active
    // class directly (same pattern the creation wizard uses) if no
    // tracker nodes are found.
    function revealStep(stepIndex) {
        const trackerNodes = document.querySelectorAll('#editStepsTracker .step-node');
        if (trackerNodes && trackerNodes.length > stepIndex) {
            trackerNodes[stepIndex].click();
            return;
        }
        document.querySelectorAll('.edit-step').forEach(el => {
            el.classList.toggle('active', Number(el.dataset.step) === stepIndex);
        });
    }

    function focusField(name) {
        setTimeout(() => {
            const el = fieldEl(name);
            if (el) {
                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                el.focus();
            }
        }, 60);
    }

    // The id of the property being edited, read from the form's action
    // (/update-property/{id}), so the title check can skip this property.
    function currentPropertyId() {
        const form = document.getElementById('editPropertyForm');
        const m = form && form.getAttribute('action') ? form.getAttribute('action').match(/update-property\/(\d+)/) : null;
        return m ? m[1] : null;
    }

    // ── Remember where the landlord was (used when a photo is deleted) ──
    function rememberScroll() {
        try {
            const main = document.querySelector('.main');
            sessionStorage.setItem(SCROLL_KEY, JSON.stringify({
                id: currentPropertyId(),
                y: window.scrollY,
                mainY: main ? main.scrollTop : 0,
                t: Date.now()
            }));
        } catch (e) { /* storage blocked: the page just reloads at the top */ }
    }

    // Puts the page back where it was after a photo delete reloaded it.
    // Only used right after a delete (the saved value is thrown away after
    // one use, and ignored if it's older than 20 seconds).
    function restoreScroll() {
        let raw = null;
        try {
            raw = sessionStorage.getItem(SCROLL_KEY);
            sessionStorage.removeItem(SCROLL_KEY);
        } catch (e) { return; }
        if (!raw) return;

        let saved;
        try { saved = JSON.parse(raw); } catch (e) { return; }
        if (!saved || String(saved.id) !== String(currentPropertyId())) return;
        if (Date.now() - saved.t > 20000) return;

        const go = function () {
            window.scrollTo({ top: saved.y || 0, left: 0, behavior: 'instant' });
            const main = document.querySelector('.main');
            if (main) main.scrollTop = saved.mainY || 0;
        };
        go();                                  // as soon as the page exists
        window.addEventListener('load', go);   // again once photos have loaded and the height is final
    }

    // Tells My Properties which card to scroll back to.
    function rememberLastProperty() {
        const id = currentPropertyId();
        if (!id) return;
        try {
            sessionStorage.setItem(LAST_PROPERTY_KEY, JSON.stringify({ id: id, t: Date.now() }));
        } catch (e) { /* ignore */ }
    }

    // Asks the server whether another property already uses this title.
    // If the request fails, answers "not taken": the server checks again
    // when saving, so this is just the friendly early warning.
    async function isTitleTaken(title) {
        const t = (title || '').trim();
        if (!t) return false;
        const id = currentPropertyId();
        const url = '/check-property-title?title=' + encodeURIComponent(t) + (id ? '&excludeId=' + id : '');
        try {
            const res = await fetch(url, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' });
            if (!res.ok) return false;
            const data = await res.json();
            return data.taken === true;
        } catch (e) {
            return false;
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        restoreScroll();
        rememberLastProperty();

        // ── Popup when the server rejected a save (?error=...) ──
        const errorParam = new URLSearchParams(window.location.search).get('error');
        if (errorParam && SERVER_ERRORS[errorParam]) {
            const info = SERVER_ERRORS[errorParam];
            window.history.replaceState({}, document.title, window.location.pathname);
            UleeModal.alert({ tone: 'warn', title: info.title, message: info.message, okText: 'Got it' })
                .then(() => {
                    const el = fieldEl(info.field);
                    if (el) el.classList.add('field-invalid');
                    const rule = REQUIRED_FIELDS_EDIT.find(f => f.name === info.field);
                    if (rule) revealStep(rule.step);
                    focusField(info.field);
                });
        }

        const saveChangesBtn = document.getElementById('saveChangesBtn');
        if (saveChangesBtn) {
            saveChangesBtn.addEventListener('click', async function (e) {
                const invalid = getInvalidFields();
                if (invalid.length > 0) {
                    e.preventDefault(); // block the save — do not submit
                    e.stopImmediatePropagation();
                    highlightInvalid(invalid);

                    // Tell the landlord what's wrong. The jump to the field happens
                    // AFTER the popup closes, so focus isn't stolen from it.
                    await UleeModal.alert({
                        tone: 'warn',
                        title: 'A few required fields need attention',
                        message: 'These fields (marked *) still need valid values. Fill them in to save changes, or use "Save Draft" to keep your progress without publishing yet.',
                        list: invalid.map(f => f.label),
                        okText: 'Fix them'
                    });

                    const firstInvalid = invalid[0];
                    revealStep(firstInvalid.step);

                    // Give the step a moment to actually become visible
                    // before scrolling/focusing the field inside it.
                    focusField(firstInvalid.name);
                } else {
                    highlightInvalid([]);
                    // Not forced invalid — let the click proceed normally.
                    // formAction stays whatever it already is ("update").
                }
            });
        }

        const form = document.getElementById('editPropertyForm');
        if (form) {
            // ── Enter must not submit anything ──
            // Pressing Enter in a text box makes the browser "click" the first
            // submit button in the form, which on this page is the × on the first
            // photo. Block Enter in text boxes and dropdowns. The Description box
            // (a textarea) still gets its new line, and buttons still work with Enter.
            form.addEventListener('keydown', function (e) {
                if (e.key !== 'Enter') return;
                const t = e.target;
                if (!t) return;
                if (t.tagName === 'SELECT') { e.preventDefault(); return; }
                if (t.tagName === 'INPUT') {
                    const type = (t.getAttribute('type') || 'text').toLowerCase();
                    if (type === 'submit' || type === 'button' || type === 'file') return;
                    e.preventDefault();
                }
            });

            // ── Unique-name check on Save Changes AND Save Draft ──
            // The first submit is cancelled while the server is asked about the
            // title; if it's free, the form is submitted again with a flag set.
            // The Delete and delete-photo buttons carry their own formaction, so
            // they skip the title check.
            form.addEventListener('submit', function (e) {
                const submitter = e.submitter;

                // Deleting a photo: remember the scroll position so the reloaded
                // page can put the landlord back where they were.
                if (submitter && (submitter.getAttribute('formaction') || '').indexOf('delete-property-image') !== -1) {
                    rememberScroll();
                    return;
                }

                if (!submitter || submitter.hasAttribute('formaction')) return;

                if (form.dataset.titleChecked === '1') {
                    form.dataset.titleChecked = '';
                    return; // checked already — let it through
                }

                const titleEl = fieldEl('title');
                const title = titleEl ? titleEl.value.trim() : '';
                if (!title) return;

                e.preventDefault();
                isTitleTaken(title).then(function (taken) {
                    if (taken) {
                        // Save Draft sets the hidden action to "draft" on click;
                        // put it back so a later Save Changes isn't saved as a draft.
                        const actionEl = document.getElementById('formAction');
                        if (actionEl) actionEl.value = 'update';

                        if (titleEl) titleEl.classList.add('field-invalid');
                        UleeModal.alert({
                            tone: 'warn',
                            title: 'That property name is already taken',
                            message: 'Every property needs its own name. Please choose a different title.',
                            okText: 'Change name'
                        }).then(() => {
                            revealStep(1);
                            focusField('title');
                        });
                        return;
                    }
                    form.dataset.titleChecked = '1';
                    form.requestSubmit(submitter);
                });
            });
        }

        // Live-clear the red border the moment a field becomes valid again.
        REQUIRED_FIELDS_EDIT.forEach(f => {
            const el = fieldEl(f.name);
            if (el) {
                el.addEventListener('input', () => {
                    if (f.validate(el.value)) el.classList.remove('field-invalid');
                });
            }
        });
    });
})();