// edit-property.js — behaviour for edit-property.html
//
// Builds the step tracker, shows one section at a time, fills the Review
// summary from the live form fields, handles tile selection (Room Type /
// Getting to Campus), and previews newly chosen photos.
//
// Works together with edit-property-validation.js, which clicks the
// tracker nodes (#editStepsTracker .step-node) to jump to a step.
(function () {
    var STEPS = [
        { label: 'Review' },
        { label: 'Basics' },
        { label: 'Pricing' },
        { label: 'Description' },
        { label: 'Amenities' },
        { label: 'Photos' }
    ];

    var current = 0;

    // Make sure only the current step is visible, whatever the other
    // stylesheets say about .edit-step.
    var style = document.createElement('style');
    style.textContent =
        '.edit-step{display:none}' +
        '.edit-step.active{display:block}';
    document.head.appendChild(style);

    function $(sel, root) { return (root || document).querySelector(sel); }
    function $all(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }
    function field(name) { return $('#editPropertyForm [name="' + name + '"]'); }

    // ── Tracker ──────────────────────────────────────────────
    function buildTracker() {
        var row = $('#editStepsTracker');
        if (!row) return;
        row.innerHTML = '';
        STEPS.forEach(function (step, i) {
            var node = document.createElement('div');
            node.className = 'step-node';
            node.setAttribute('role', 'button');
            node.setAttribute('tabindex', '0');
            node.innerHTML =
                '<div class="node-circle">' + (i + 1) + '</div>' +
                '<div class="node-label">' + step.label + '</div>';
            node.addEventListener('click', function () { showStep(i); });
            node.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); showStep(i); }
            });
            row.appendChild(node);
        });
    }

    function showStep(index) {
        if (index < 0 || index >= STEPS.length) return;
        current = index;

        $all('.edit-step').forEach(function (el) {
            el.classList.toggle('active', Number(el.getAttribute('data-step')) === index);
        });
        $all('#editStepsTracker .step-node').forEach(function (node, i) {
            node.classList.toggle('active', i === index);
        });

        var fill = $('#editProgressFill');
        if (fill) fill.style.width = ((index + 1) / STEPS.length * 100) + '%';

        if (index === 0) buildReview();

        var tracker = $('.tracker-card');
        if (tracker && tracker.scrollIntoView) {
            tracker.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    // ── Review summary (step 0) ──────────────────────────────
    function val(name) {
        var el = field(name);
        return el ? String(el.value || '').trim() : '';
    }

    function checkedVal(name) {
        var el = $('#editPropertyForm input[name="' + name + '"]:checked');
        return el ? el.value : '';
    }

    function buildReview() {
        var body = $('#reviewSummaryBody');
        if (!body) return;

        var availSel = field('availableFrom');
        var availText = availSel && availSel.selectedIndex > 0
            ? availSel.options[availSel.selectedIndex].text : '';

        var amenityCount = $all('#editPropertyForm input[name="amenityIds"]:checked').length;
        var existingPhotos = $all('.gallery-item').length;
        var coverInput = $('#coverImageInput');
        var moreInput = $('#additionalImagesInput');
        var newPhotos =
            (coverInput && coverInput.files ? coverInput.files.length : 0) +
            (moreInput && moreInput.files ? moreInput.files.length : 0);

        var rent = val('rent');
        var deposit = val('deposit');
        var desc = val('description');

        var rows = [
            { key: 'Property title',   value: val('title'),                       step: 1 },
            { key: 'Room type',        value: checkedVal('type'),                 step: 1 },
            { key: 'Capacity',         value: val('capacity') ? val('capacity') + ' students' : '', step: 1 },
            { key: 'Monthly rent',     value: rent ? 'R ' + rent : '',            step: 2 },
            { key: 'Deposit',          value: deposit ? 'R ' + deposit : '',      step: 2 },
            { key: 'Suburb',           value: val('city'),                        step: 2 },
            { key: 'Full address',     value: val('address'),                     step: 2 },
            { key: 'Getting to campus',value: checkedVal('commuteType'),          step: 2 },
            { key: 'Available from',   value: availText,                          step: 2 },
            { key: 'Description',      value: desc,                               step: 3 },
            { key: 'Amenities',        value: amenityCount ? amenityCount + ' selected' : '', step: 4 },
            { key: 'Photos',           value: (existingPhotos + newPhotos) ? (existingPhotos + newPhotos) + ' photo(s)' : '', step: 5 }
        ];

        body.innerHTML = '';
        rows.forEach(function (r) {
            var row = document.createElement('div');
            row.className = 'review-row';
            row.setAttribute('role', 'button');
            row.setAttribute('tabindex', '0');

            var k = document.createElement('span');
            k.className = 'review-key';
            k.textContent = r.key;

            var v = document.createElement('span');
            v.className = 'review-val';
            v.textContent = r.value || 'Not set';
            if (!r.value) v.style.opacity = '.55';

            row.appendChild(k);
            row.appendChild(v);
            row.addEventListener('click', function () { showStep(r.step); });
            row.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') showStep(r.step);
            });
            body.appendChild(row);
        });
    }

    // ── Tile selection (Room Type / Getting to Campus) ──────
    window.selectEditTile = function (label, name) {
        var input = label.querySelector('input[type="radio"]');
        if (!input) return;
        input.checked = true;
        $all('#editPropertyForm input[name="' + name + '"]').forEach(function (r) {
            var tile = r.closest('.tile-option');
            if (tile) tile.classList.toggle('selected', r.checked);
        });
    };

    // ── Photo previews ───────────────────────────────────────
    function thumb(src, size) {
        var img = document.createElement('img');
        img.src = src;
        img.alt = 'Preview';
        img.style.cssText =
            'width:' + size + 'px;height:' + size + 'px;object-fit:cover;' +
            'border-radius:8px;border:1.5px solid #e1e6e6;';
        return img;
    }

    window.previewCoverImage = function (input) {
        var box = $('#coverImagePreview');
        if (!box) return;
        box.innerHTML = '';
        if (input.files && input.files[0]) {
            box.appendChild(thumb(URL.createObjectURL(input.files[0]), 120));
        }
    };

    window.previewAdditionalImages = function (input) {
        var box = $('#additionalImagesPreview');
        if (!box) return;
        box.innerHTML = '';
        Array.prototype.forEach.call(input.files || [], function (file) {
            box.appendChild(thumb(URL.createObjectURL(file), 88));
        });
    };

    // ── Init ─────────────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', function () {
        buildTracker();
        showStep(0);

        // Keep the Review summary fresh as fields change.
        var form = $('#editPropertyForm');
        if (form) {
            form.addEventListener('input', function () { if (current === 0) buildReview(); });
            form.addEventListener('change', function () { if (current === 0) buildReview(); });
        }
    });
})();