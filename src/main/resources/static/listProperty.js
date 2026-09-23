const STEPS = [
  { label: 'Basic info' },
  { label: 'Pricing & location' },
  { label: 'Description' },
  { label: 'Amenities' },
  { label: 'Photos' },
  { label: 'Features' },
  { label: 'Review' }
];

let currentStep = 0;
let accumulatedImages = [];
let coverImageFile = null;          // kept so the preview card can show it
let customFeatures = [];
let selectedAmenityIds = new Set(); // persists across steps, unlike DOM checkboxes

// Real categories/amenities injected by Thymeleaf (see list-property.html).
// Shape: { "Room": [{amenityID:1, name:"Furnished", category:"Room"}, ...], "Kitchen & Bathroom": [...] }
const AMENITY_CATEGORIES = window.AMENITY_CATEGORIES || {};

// Flat id -> name map, so the preview card can render amenity BADGES
// (names) rather than just a count of how many are ticked.
const AMENITY_NAMES = (() => {
  const map = {};
  Object.values(AMENITY_CATEGORIES).forEach(list => {
    (list || []).forEach(a => { map[a.amenityID] = a.name; });
  });
  return map;
})();

const state = {
  title: '',
  type: '',
  capacity: '1',
  city: '',
  address: '',
  commuteType: '',
  rent: '',
  deposit: '',
  availableFrom: '',
  description: ''
};

// ── Required-field validation ──
// Fields that MUST be filled with a valid value before a listing can be
// Published. If any of these fail validation when the landlord hits
// "Publish listing", the submission is silently redirected to Draft instead
// of being blocked outright — see updateNavButtons() below. The readiness
// checklist in the right column surfaces this BEFORE they get there.
const REQUIRED_FIELDS = [
  { key: 'title', label: 'Property title', step: 0, validate: v => !!(v && v.trim().length > 0) },
  { key: 'capacity', label: 'Capacity', step: 0, validate: v => !!(v && Number(v) >= 1) },
  // Mirrors MIN_RENT in PropertyController. Keep the two in step.
  { key: 'rent', label: 'Monthly rent (at least R1 000)', step: 1, validate: v => !!(v && Number(v) >= 1000) },
  { key: 'address', label: 'Full address', step: 1, validate: v => !!(v && v.trim().length > 0) }
];

// Only start showing red borders once the landlord has actually tried to
// publish once — avoids painting every field red on a blank, untouched form.
let validationAttempted = false;

function isRequiredFieldInvalid(key) {
  const field = REQUIRED_FIELDS.find(f => f.key === key);
  if (!field) return false;
  return validationAttempted && !field.validate(state[key]);
}

function invalidClass(key) {
  return isRequiredFieldInvalid(key) ? 'field-invalid' : '';
}

function getInvalidRequiredFields() {
  syncStateFromDOM();
  return REQUIRED_FIELDS.filter(f => !f.validate(state[f.key]));
}

// Clears the red border live as soon as the landlord fixes a field, and
// re-applies it if they empty it back out again after a failed attempt.
// Also repaints the preview card on every keystroke — that live link is
// the whole point of the right-hand column.
function wireValidationListeners() {
  const stepFieldNames = ['title', 'capacity', 'rent', 'address', 'city', 'type', 'commuteType', 'deposit', 'description'];

  stepFieldNames.forEach(name => {
    const el = document.querySelector(`[name="${name}"]`);
    if (!el) return;
    const evt = el.tagName === 'SELECT' ? 'change' : 'input';
    el.addEventListener(evt, () => {
      state[name] = el.value;

      const rule = REQUIRED_FIELDS.find(f => f.key === name);
      if (rule) {
        if (rule.validate(el.value)) {
          el.classList.remove('field-invalid');
        } else if (validationAttempted) {
          el.classList.add('field-invalid');
        }
      }

      renderPreview();
      renderReadiness();
    });
  });
}

function injectValidationStyles() {
  if (document.getElementById('requiredFieldValidationStyles')) return;
  const style = document.createElement('style');
  style.id = 'requiredFieldValidationStyles';
  style.textContent = `
    .field-invalid { border: 2px solid #dc2626 !important; background: #fef2f2 !important; }
    .required-asterisk { color: #dc2626; font-weight: 700; margin-left: 3px; }
  `;
  document.head.appendChild(style);
}

function syncStateFromDOM() {
  const getVal = (name) => {
    const el = document.querySelector(`[name="${name}"]`);
    return el ? el.value : (state[name] || '');
  };
  state.title = getVal('title');
  state.type = getVal('type');
  state.capacity = getVal('capacity');
  state.city = getVal('city');
  state.address = getVal('address');
  state.commuteType = getVal('commuteType');
  state.rent = getVal('rent');
  state.deposit = getVal('deposit');
  state.availableFrom = getVal('availableFrom');
  state.description = getVal('description');
}

// ── Tracker: numbered dots only. The step's NAME is printed once, in the
//    caption above, instead of seven uppercase labels fighting the panel
//    heading right below them. ──
function renderTracker() {
  const tracker = document.getElementById('stepsTracker');
  const fill = document.getElementById('progressFill');
  const count = document.getElementById('stepCount');
  const name = document.getElementById('stepName');

  if (count) count.textContent = `Step ${currentStep + 1} of ${STEPS.length}`;
  if (name) name.textContent = STEPS[currentStep].label;

  if (fill) fill.style.width = `${((currentStep + 1) / STEPS.length) * 100}%`;
  if (!tracker) return;

  tracker.innerHTML = STEPS.map((s, i) => `
    <button type="button"
            class="step-dot ${i === currentStep ? 'active' : ''} ${i < currentStep ? 'completed' : ''}"
            onclick="jumpToStep(${i})"
            aria-label="${s.label}"
            title="${s.label}"
            ${i === currentStep ? 'aria-current="step"' : ''}>
      ${i < currentStep ? '✓' : i + 1}
    </button>
  `).join('');
}

function renderStepContent() {
  const panel = document.getElementById('stepPanel');
  if (!panel) return;

  switch(currentStep) {
    case 0:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Basic info &amp; accommodation type</h2>
          <p>Start by giving your listing a title and selecting the room setup.</p>
        </div>
        <div class="f-grid">
          <div class="field f-col-2">
            <label>Property title<span class="required-asterisk">*</span></label>
            <input type="text" name="title" placeholder="e.g. The Dunes Student Residence" value="${escapeHtml(state.title)}" required class="${invalidClass('title')}" />
          </div>
          <div class="field f-col-2">
            <label>Room type</label>
            <div class="tile-selector">
              <label class="tile-option ${state.type === 'Single Room' ? 'selected' : ''}" onclick="selectTile(this, 'type')">
                <input type="radio" name="type" value="Single Room" ${state.type === 'Single Room' ? 'checked' : ''} />
                <span class="tile-icon">🛏️</span>
                <span class="tile-text">
                  <span class="tile-title">Single room</span>
                  <span class="hint-text">Private bedroom for one student</span>
                </span>
              </label>
              <label class="tile-option ${state.type === 'Sharing' ? 'selected' : ''}" onclick="selectTile(this, 'type')">
                <input type="radio" name="type" value="Sharing" ${state.type === 'Sharing' ? 'checked' : ''} />
                <span class="tile-icon">👥</span>
                <span class="tile-text">
                  <span class="tile-title">Sharing room</span>
                  <span class="hint-text">Shared bedroom arrangement</span>
                </span>
              </label>
              <label class="tile-option ${state.type === 'Commune' ? 'selected' : ''}" onclick="selectTile(this, 'type')">
                <input type="radio" name="type" value="Commune" ${state.type === 'Commune' ? 'checked' : ''} />
                <span class="tile-icon">🏠</span>
                <span class="tile-text">
                  <span class="tile-title">Commune</span>
                  <span class="hint-text">Shared house, typically 4–5 students</span>
                </span>
              </label>
            </div>
          </div>
          <div class="field f-col-2">
            <label>Capacity<span class="required-asterisk">*</span></label>
            <input type="number" name="capacity" min="1" step="1" placeholder="e.g. 4" value="${escapeHtml(state.capacity)}" required class="${invalidClass('capacity')}" />
            <p class="hint-text">How many students this listing can hold. This caps how many applications you can accept.</p>
          </div>
        </div>`;
      break;

    case 1:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Pricing &amp; location</h2>
          <p>Students filter by price and distance, so these two do most of the work.</p>
        </div>
        <div class="f-grid f-grid-2">
          <div class="field">
            <label>Monthly rent (R)<span class="required-asterisk">*</span></label>
            <input type="number" step="0.01" min="1000" name="rent" placeholder="4500.00" value="${escapeHtml(state.rent)}" required class="${invalidClass('rent')}" />
            <p class="hint-text">Minimum R1 000 to publish. Below that it can only be saved as a draft.</p>
          </div>
          <div class="field">
            <label>Deposit (R)</label>
            <input type="number" step="0.01" name="deposit" placeholder="4500.00" value="${escapeHtml(state.deposit)}" />
          </div>
          <div class="field">
            <label>Suburb</label>
            <input type="text" name="city" placeholder="e.g. Summerstrand" value="${escapeHtml(state.city)}" />
          </div>
          <div class="field">
            <label>Full address<span class="required-asterisk">*</span></label>
            <input type="text" name="address" placeholder="Full street address" value="${escapeHtml(state.address)}" required class="${invalidClass('address')}" />
          </div>
          <div class="field f-col-2">
            <label>Getting to campus</label>
            <div class="tile-selector">
              <label class="tile-option ${state.commuteType === 'Walking distance' ? 'selected' : ''}" onclick="selectTile(this, 'commuteType')">
                <input type="radio" name="commuteType" value="Walking distance" ${state.commuteType === 'Walking distance' ? 'checked' : ''} />
                <span class="tile-icon">🚶</span>
                <span class="tile-text">
                  <span class="tile-title">Walking distance</span>
                  <span class="hint-text">Close enough to walk to campus</span>
                </span>
              </label>
              <label class="tile-option ${state.commuteType === 'Shuttle required' ? 'selected' : ''}" onclick="selectTile(this, 'commuteType')">
                <input type="radio" name="commuteType" value="Shuttle required" ${state.commuteType === 'Shuttle required' ? 'checked' : ''} />
                <span class="tile-icon">🚌</span>
                <span class="tile-text">
                  <span class="tile-title">Shuttle required</span>
                  <span class="hint-text">Transport required to reach campus</span>
                </span>
              </label>
            </div>
          </div>
        </div>`;
      break;

    case 2:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Description</h2>
          <p>Highlight key selling points, house rules, and nearby universities.</p>
        </div>
        <div class="f-grid">
          <div class="field f-col-2">
            <label>Description</label>
            <textarea name="description" placeholder="Tell students what makes this place worth renting...">${escapeHtml(state.description)}</textarea>
          </div>
        </div>`;
      break;

    case 3:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Amenities</h2>
          <p>Tick everything included. The first three appear as badges on your listing card — watch them land on the right.</p>
        </div>
        <div id="amenitiesContainer">
          ${renderAmenityCategories()}
        </div>`;
      wireAmenityCheckboxes();
      break;

    case 4:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Photos</h2>
          <p>Your cover photo fills the empty slot in the preview. Listings with five or more photos get seen more.</p>
        </div>
        <div class="f-grid">
          <div class="field f-col-2">
            <label>Cover photo</label>
            <input type="file" id="coverImageInput" name="coverImage" accept="image/*" />
            <p class="hint-text">This is the single image students see on the browse grid.</p>
          </div>
          <div class="field f-col-2" style="margin-top:10px;">
            <label>Additional photos</label>
            <div class="upload-box" id="dropZone">
              <div class="upload-icon">📸</div>
              <div class="upload-title">Drag photos here, or <b>browse files</b></div>
            </div>
            <input type="file" id="additionalPhotosInput" accept="image/*" multiple style="display:none;" />
            <div class="photo-grid" id="photosPreview"></div>
          </div>
        </div>`;
      setupImageHandlers();
      renderPhotoPreviews();
      break;

    case 5:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Special features</h2>
          <p>Unique highlights like braai areas, study hubs, or game rooms. You can attach photos to each one later from Manage Property.</p>
        </div>
        <div class="f-grid">
          <div class="field f-col-2">
            <label>Add a feature</label>
            <div style="display:flex; gap:10px;">
              <input type="text" id="featureInput" placeholder="e.g. Braai area, quiet study lounge" />
              <button type="button" class="btn btn-secondary" onclick="addFeatureTag()">Add</button>
            </div>
            <div class="feature-tag-wrap" id="featureTagList"></div>
          </div>
        </div>`;
      renderFeatureTags();
      break;

    case 6:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Review</h2>
          <p>Check everything below, then publish.</p>
        </div>
        <div class="review-table">
          <div class="review-row"><span class="review-key">Title</span><span class="review-val">${escapeHtml(state.title) || '—'}</span></div>
          <div class="review-row"><span class="review-key">Room type</span><span class="review-val">${escapeHtml(state.type) || '—'}</span></div>
          <div class="review-row"><span class="review-key">Capacity</span><span class="review-val">${escapeHtml(state.capacity) || '—'} student(s)</span></div>
          <div class="review-row"><span class="review-key">Monthly rent</span><span class="review-val price">R${escapeHtml(state.rent || '0')} / mo</span></div>
          <div class="review-row"><span class="review-key">Deposit</span><span class="review-val">R${escapeHtml(state.deposit || '0')}</span></div>
          <div class="review-row"><span class="review-key">Location</span><span class="review-val">${escapeHtml(state.address)}${state.city ? ', ' + escapeHtml(state.city) : ''}</span></div>
          <div class="review-row"><span class="review-key">Commute</span><span class="review-val">${escapeHtml(state.commuteType) || '—'}</span></div>
          <div class="review-row"><span class="review-key">Amenities</span><span class="review-val">${selectedAmenityIds.size} selected</span></div>
          <div class="review-row"><span class="review-key">Special features</span><span class="review-val">${customFeatures.length ? customFeatures.map(escapeHtml).join(', ') : '—'}</span></div>
          <div class="review-row"><span class="review-key">Photos</span><span class="review-val">${accumulatedImages.length + (coverImageFile ? 1 : 0)} attached</span></div>
        </div>`;
      break;
  }

  wireValidationListeners();
  updateNavButtons();
  renderPreview();
  renderReadiness();
}

// ── The live listing card. This is the same shape students see on the
//    browse grid, so the landlord is never guessing what they're building. ──
function renderPreview() {
  syncStateFromDOM();

  const setText = (id, text) => {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
  };

  const titleEl = document.getElementById('previewTitle');
  if (titleEl) {
    titleEl.textContent = state.title.trim() || 'Untitled listing';
    titleEl.classList.toggle('is-placeholder', !state.title.trim());
  }

  const rentEl = document.getElementById('previewRent');
  if (rentEl) {
    const rent = Number(state.rent);
    rentEl.textContent = rent > 0 ? `R${rent.toLocaleString('en-ZA')} / month` : 'R— / month';
    rentEl.classList.toggle('is-placeholder', !(rent > 0));
  }

  const locEl = document.getElementById('previewLocation');
  if (locEl) {
    const parts = [state.address.trim(), state.city.trim()].filter(Boolean);
    locEl.textContent = parts.length ? parts.join(', ') : 'Address not set';
    locEl.classList.toggle('is-placeholder', parts.length === 0);
  }

  const typeEl = document.getElementById('previewType');
  if (typeEl) {
    const bits = [state.type, state.capacity ? `${state.capacity} student(s)` : ''].filter(Boolean);
    typeEl.textContent = bits.length ? bits.join(' · ') : 'Room type';
    typeEl.classList.toggle('is-placeholder', bits.length === 0);
  }

  // Amenity badges — the first three, matching the real card's behaviour.
  const badgeWrap = document.getElementById('previewBadges');
  if (badgeWrap) {
    const names = [...selectedAmenityIds].map(id => AMENITY_NAMES[id]).filter(Boolean).slice(0, 3);
    const extra = selectedAmenityIds.size - names.length;
    badgeWrap.innerHTML = names.map(n => `<span class="listing-badge">${escapeHtml(n)}</span>`).join('')
        + (extra > 0 ? `<span class="listing-badge listing-badge--more">+${extra}</span>` : '');
  }

  // Cover photo fills the empty slot once it exists.
  const img = document.getElementById('previewImage');
  const empty = document.getElementById('previewMediaEmpty');
  if (img && empty) {
    if (coverImageFile) {
      img.src = URL.createObjectURL(coverImageFile);
      img.classList.add('is-visible');
      empty.style.display = 'none';
    } else {
      img.classList.remove('is-visible');
      img.removeAttribute('src');
      empty.style.display = '';
    }
  }
}

// ── Readiness checklist: makes the silent draft-downgrade rule visible
//    before the landlord hits Publish, instead of after. ──
function renderReadiness() {
  const list = document.getElementById('readinessList');
  const countEl = document.getElementById('readinessCount');
  const titleEl = document.getElementById('readinessTitle');
  const noteEl = document.getElementById('readinessNote');
  const wrap = document.getElementById('readiness');
  if (!list) return;

  syncStateFromDOM();

  const done = REQUIRED_FIELDS.filter(f => f.validate(state[f.key]));
  const allDone = done.length === REQUIRED_FIELDS.length;

  list.innerHTML = REQUIRED_FIELDS.map(f => {
    const ok = f.validate(state[f.key]);
    return `
      <li class="readiness-item ${ok ? 'is-done' : ''}">
        <button type="button" onclick="jumpToStep(${f.step})">
          <span class="readiness-mark" aria-hidden="true">${ok ? '✓' : ''}</span>
          <span>${f.label}</span>
        </button>
      </li>`;
  }).join('');

  if (countEl) countEl.textContent = `${done.length} of ${REQUIRED_FIELDS.length}`;
  if (wrap) wrap.classList.toggle('is-ready', allDone);
  if (titleEl) titleEl.textContent = allDone ? 'Ready to publish' : 'Needed to publish';
  if (noteEl) {
    noteEl.textContent = allDone
        ? 'You can publish from the review step whenever you\'re ready.'
        : 'Anything still missing saves as a draft instead of going live.';
  }
}

// ── Amenities: built from the real DB categories passed in by Thymeleaf ──
function renderAmenityCategories() {
  const categoryNames = Object.keys(AMENITY_CATEGORIES);
  if (categoryNames.length === 0) {
    return `<p class="hint-text">No amenities configured yet. Ask an admin to add some to the amenity table.</p>`;
  }
  return categoryNames.map(cat => `
    <div class="amenity-cat">
      <div class="amenity-cat-title">${escapeHtml(cat)}</div>
      <div class="amenity-grid-list">
        ${AMENITY_CATEGORIES[cat].map(a => `
          <label class="amenity-card-item">
            <input type="checkbox" class="amenity-checkbox" value="${a.amenityID}" ${selectedAmenityIds.has(a.amenityID) ? 'checked' : ''} />
            ${escapeHtml(a.name)}
          </label>`).join('')}
      </div>
    </div>`).join('');
}

function wireAmenityCheckboxes() {
  document.querySelectorAll('.amenity-checkbox').forEach(cb => {
    cb.addEventListener('change', () => {
      const id = parseInt(cb.value, 10);
      if (cb.checked) selectedAmenityIds.add(id);
      else selectedAmenityIds.delete(id);
      renderPreview(); // badges land on the card as they tick
    });
  });
}

function selectTile(element, fieldName) {
  const container = element.closest('.tile-selector');
  container.querySelectorAll('.tile-option').forEach(el => el.classList.remove('selected'));
  element.classList.add('selected');
  const input = element.querySelector('input');
  input.checked = true;
  state[fieldName] = input.value;
  renderPreview();
}

function navigateStep(dir) {
  syncStateFromDOM();

  if (dir === 1 && currentStep === 0 && (!state.title || !state.title.trim())) {
    alert('Please enter a property title before continuing.');
    return;
  }

  if (dir === 1 && currentStep === 0 && (!state.capacity || Number(state.capacity) < 1)) {
    alert('Please enter how many students this listing can hold (at least 1).');
    return;
  }

  // After the Photos step, ask whether the landlord wants to add any
  // special features (braai area, study hub, etc.) before deciding whether
  // to show the Features step or skip straight to Review.
  if (dir === 1 && currentStep === 4) {
    openSpecialFeatureModal();
    return;
  }

  currentStep = Math.max(0, Math.min(STEPS.length - 1, currentStep + dir));
  renderTracker();
  renderStepContent();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function jumpToStep(idx) {
  syncStateFromDOM();
  if (idx > currentStep && (!state.title || !state.title.trim())) {
    alert('Please complete the basic info step first.');
    return;
  }
  currentStep = idx;
  renderTracker();
  renderStepContent();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function openSpecialFeatureModal() {
  const modal = document.getElementById('specialFeatureModal');
  if (modal) modal.classList.add('active');
}

// wantsFeatures = true  -> go to the Features step (index 5)
// wantsFeatures = false -> skip straight to Review (index 6)
function closeSpecialModal(wantsFeatures) {
  const modal = document.getElementById('specialFeatureModal');
  if (modal) modal.classList.remove('active');
  currentStep = wantsFeatures ? 5 : 6;
  renderTracker();
  renderStepContent();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// IMPORTANT: nextBtn's type attribute is NEVER changed to "submit" here.
// Flipping a button's type to "submit" from inside its own click handler is
// a known browser quirk — some browsers finish processing that same click
// as a form submission once the type changes mid-event, which is exactly
// what was causing Publish to fire itself the instant you landed on the
// Review step. Instead, the button stays type="button" permanently, and on
// the final step we submit explicitly and deliberately via requestSubmit().
function updateNavButtons() {
  const prevBtn = document.getElementById('prevBtn');
  const nextBtn = document.getElementById('nextBtn');

  if (prevBtn) prevBtn.style.visibility = currentStep === 0 ? 'hidden' : 'visible';
  if (nextBtn) {
    if (currentStep === STEPS.length - 1) {
      nextBtn.textContent = 'Publish listing';
      nextBtn.onclick = () => {
        const invalid = getInvalidRequiredFields();
        const form = document.getElementById('listPropertyForm');

        if (invalid.length > 0) {
          // At least one required field is missing or invalid — don't block
          // the landlord, but don't publish either: fall back to saving as a
          // draft, jump them to the first problem field, and say what's needed.
          validationAttempted = true;
          setSubmitAction('draft');
          currentStep = invalid[0].step;
          renderTracker();
          renderStepContent();
          window.scrollTo({ top: 0, behavior: 'smooth' });
          alert('These still need valid values:\n\n' +
              invalid.map(f => '• ' + f.label).join('\n') +
              '\n\nSaving as a draft for now — fill these in and click Publish listing again.');
          if (form) form.requestSubmit();
          return;
        }

        setSubmitAction('submit');
        if (form) form.requestSubmit(); // fires the 'submit' listener, unlike form.submit()
      };
    } else {
      nextBtn.textContent = 'Continue';
      nextBtn.onclick = () => navigateStep(1);
    }
  }
}

function setSubmitAction(val) {
  const actionEl = document.getElementById('formAction');
  if (actionEl) actionEl.value = val;
}

function setupImageHandlers() {
  const dropZone = document.getElementById('dropZone');
  const fileInput = document.getElementById('additionalPhotosInput');
  const coverInput = document.getElementById('coverImageInput');

  // Cover photo drives the preview card's media slot.
  if (coverInput) {
    coverInput.onchange = () => {
      coverImageFile = coverInput.files && coverInput.files[0] ? coverInput.files[0] : null;
      renderPreview();
    };
  }

  if (dropZone && fileInput) {
    dropZone.onclick = () => fileInput.click();
    fileInput.onchange = () => {
      for (const file of fileInput.files) accumulatedImages.push(file);
      fileInput.value = '';
      renderPhotoPreviews();
    };
    dropZone.ondragover = (e) => { e.preventDefault(); dropZone.classList.add('dragover'); };
    dropZone.ondragleave = () => dropZone.classList.remove('dragover');
    dropZone.ondrop = (e) => {
      e.preventDefault();
      dropZone.classList.remove('dragover');
      for (const file of e.dataTransfer.files) accumulatedImages.push(file);
      renderPhotoPreviews();
    };
  }
}

function renderPhotoPreviews() {
  const container = document.getElementById('photosPreview');
  if (!container) return;
  container.innerHTML = '';
  accumulatedImages.forEach((file, index) => {
    const item = document.createElement('div');
    item.className = 'photo-item';

    const img = document.createElement('img');
    img.src = URL.createObjectURL(file);
    item.appendChild(img);

    const rm = document.createElement('button');
    rm.type = 'button';
    rm.className = 'rm-btn';
    rm.innerHTML = '×';
    rm.onclick = () => {
      accumulatedImages.splice(index, 1);
      renderPhotoPreviews();
    };
    item.appendChild(rm);
    container.appendChild(item);
  });
}

function addFeatureTag() {
  const input = document.getElementById('featureInput');
  if (!input) return;
  const val = input.value.trim();
  if (val && !customFeatures.includes(val)) {
    customFeatures.push(val);
    input.value = '';
    renderFeatureTags();
  }
}

function removeFeatureTag(idx) {
  customFeatures.splice(idx, 1);
  renderFeatureTags();
}

function renderFeatureTags() {
  const wrap = document.getElementById('featureTagList');
  if (!wrap) return;
  wrap.innerHTML = customFeatures.map((f, i) => `
    <div class="feature-chip">${escapeHtml(f)} <span onclick="removeFeatureTag(${i})">×</span></div>
  `).join('');
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ── The single place that turns everything the user has entered across every
//    step into real <input> elements inside the <form>, right before it's
//    actually sent. This is what fixes "draft/publish not working": the
//    wizard only ever renders ONE step's inputs into the DOM at a time, so
//    without this step, most fields (and all amenities) never reach Spring. ──
function prepareFormForSubmit(form) {
  syncStateFromDOM();

  // Wipe any hidden inputs we injected on a previous attempt, so re-submits
  // (e.g. clicking Draft, going back, then Publish) don't duplicate fields.
  form.querySelectorAll('.js-injected-field').forEach(el => el.remove());

  const addHidden = (name, value) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value != null ? value : '';
    input.className = 'js-injected-field';
    form.appendChild(input);
  };

  // Only inject a hidden field for a given name if the currently-rendered
  // step doesn't already have a live input with that name (avoids sending
  // the same field twice when the user submits while sitting on that step).
  const hasLiveField = (name) => !!form.querySelector(`[name="${name}"]:not(.js-injected-field)`);

  Object.keys(state).forEach(key => {
    if (!hasLiveField(key)) addHidden(key, state[key]);
  });

  selectedAmenityIds.forEach(id => addHidden('amenityIds', id));
  customFeatures.forEach(name => addHidden('featureNames', name));

  // Additional photos.
  const dataTransfer = new DataTransfer();
  accumulatedImages.forEach(file => dataTransfer.items.add(file));
  let hiddenImagesInput = document.getElementById('imagesSubmitInput');
  if (!hiddenImagesInput) {
    hiddenImagesInput = document.createElement('input');
    hiddenImagesInput.type = 'file';
    hiddenImagesInput.id = 'imagesSubmitInput';
    hiddenImagesInput.name = 'images';
    hiddenImagesInput.multiple = true;
    hiddenImagesInput.style.display = 'none';
    form.appendChild(hiddenImagesInput);
  }
  hiddenImagesInput.files = dataTransfer.files;

  // Cover photo — the live input only exists while the Photos step is
  // rendered, so re-attach it from memory when submitting from any other step.
  if (coverImageFile && !hasLiveField('coverImage')) {
    let hiddenCover = document.getElementById('coverSubmitInput');
    if (!hiddenCover) {
      hiddenCover = document.createElement('input');
      hiddenCover.type = 'file';
      hiddenCover.id = 'coverSubmitInput';
      hiddenCover.name = 'coverImage';
      hiddenCover.style.display = 'none';
      form.appendChild(hiddenCover);
    }
    const coverTransfer = new DataTransfer();
    coverTransfer.items.add(coverImageFile);
    hiddenCover.files = coverTransfer.files;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  injectValidationStyles();

  renderTracker();
  renderStepContent();

  const form = document.getElementById('listPropertyForm');
  if (form) {
    // Native 'submit' fires for BOTH the Draft button (type=submit) and the
    // Publish path via requestSubmit() — so this one listener covers every
    // real way the form can be sent.
    form.addEventListener('submit', function() {
      prepareFormForSubmit(this);
    });
  }
});