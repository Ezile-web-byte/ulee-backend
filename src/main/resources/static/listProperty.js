const STEPS = [
  { label: 'Basic Info' },
  { label: 'Pricing & Location' },
  { label: 'Description' },
  { label: 'Amenities' },
  { label: 'Photos' },
  { label: 'Features' },
  { label: 'Review' }
];

let currentStep = 0;
let accumulatedImages = [];
let customFeatures = [];
let selectedAmenityIds = new Set(); // persists across steps, unlike DOM checkboxes

// Real categories/amenities injected by Thymeleaf (see list-property.html).
// Shape: { "Room": [{amenityID:1, name:"Furnished", category:"Room"}, ...], "Kitchen & Bathroom": [...] }
const AMENITY_CATEGORIES = window.AMENITY_CATEGORIES || {};

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

function renderTracker() {
  const tracker = document.getElementById('stepsTracker');
  const fill = document.getElementById('progressFill');
  if (!tracker || !fill) return;

  const pct = ((currentStep + 1) / STEPS.length) * 100;
  fill.style.width = `${pct}%`;

  tracker.innerHTML = STEPS.map((s, i) => `
    <div class="step-node ${i === currentStep ? 'active' : ''} ${i < currentStep ? 'completed' : ''}" onclick="jumpToStep(${i})">
      <div class="node-circle">${i < currentStep ? '✓' : i + 1}</div>
      <div class="node-label">${s.label}</div>
    </div>
  `).join('');
}

function renderStepContent() {
  const panel = document.getElementById('stepPanel');
  if (!panel) return;

  switch(currentStep) {
    case 0:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Basic Info & Accommodation Type</h2>
          <p>Start by giving your listing a title and selecting the room setup.</p>
        </div>
        <div class="f-grid">
          <div class="field f-col-2">
            <label>Property Title</label>
            <input type="text" name="title" placeholder="e.g. The Dunes Student Residence" value="${escapeHtml(state.title)}" required />
          </div>
          <div class="field f-col-2">
            <label>Room Type</label>
            <div class="tile-selector">
              <label class="tile-option ${state.type === 'Single Room' ? 'selected' : ''}" onclick="selectTile(this, 'type')">
                <input type="radio" name="type" value="Single Room" ${state.type === 'Single Room' ? 'checked' : ''} />
                <span class="tile-icon">🛏️</span>
                <span class="tile-text">
                  <span class="tile-title">Single Room</span>
                  <span class="hint-text">Private bedroom for one student</span>
                </span>
              </label>
              <label class="tile-option ${state.type === 'Sharing' ? 'selected' : ''}" onclick="selectTile(this, 'type')">
                <input type="radio" name="type" value="Sharing" ${state.type === 'Sharing' ? 'checked' : ''} />
                <span class="tile-icon">👥</span>
                <span class="tile-text">
                  <span class="tile-title">Sharing Room</span>
                  <span class="hint-text">Shared bedroom arrangement</span>
                </span>
              </label>
            </div>
          </div>
          <div class="field f-col-2">
            <label>Capacity (number of students this listing can hold)</label>
            <input type="number" name="capacity" min="1" step="1" placeholder="e.g. 4" value="${escapeHtml(state.capacity)}" required />
            <p class="hint-text">This caps how many student applications you can accept for this property.</p>
          </div>
        </div>`;
      break;

    case 1:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Pricing & Location Details</h2>
          <p>Set rent amounts and help students know where you are located.</p>
        </div>
        <div class="f-grid f-grid-2">
          <div class="field">
            <label>Monthly Rent (R)</label>
            <input type="number" step="0.01" name="rent" placeholder="4500.00" value="${escapeHtml(state.rent)}" />
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
            <label>Full Address</label>
            <input type="text" name="address" placeholder="Full street address" value="${escapeHtml(state.address)}" />
          </div>
          <div class="field f-col-2">
            <label>Getting to Campus</label>
            <div class="tile-selector">
              <label class="tile-option ${state.commuteType === 'Walking distance' ? 'selected' : ''}" onclick="selectTile(this, 'commuteType')">
                <input type="radio" name="commuteType" value="Walking distance" ${state.commuteType === 'Walking distance' ? 'checked' : ''} />
                <span class="tile-icon">🚶</span>
                <span class="tile-text">
                  <span class="tile-title">Walking Distance</span>
                  <span class="hint-text">Close enough to walk to campus</span>
                </span>
              </label>
              <label class="tile-option ${state.commuteType === 'Shuttle required' ? 'selected' : ''}" onclick="selectTile(this, 'commuteType')">
                <input type="radio" name="commuteType" value="Shuttle required" ${state.commuteType === 'Shuttle required' ? 'checked' : ''} />
                <span class="tile-icon">🚌</span>
                <span class="tile-text">
                  <span class="tile-title">Shuttle Required</span>
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
          <h2>Property Description</h2>
          <p>Highlight key selling points, house rules, and close universities.</p>
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
          <h2>Property Amenities</h2>
          <p>Tick everything included with this accommodation. The first 3 you select
             (in the order shown) are the ones that appear as badges on the property card.</p>
        </div>
        <div id="amenitiesContainer">
          ${renderAmenityCategories()}
        </div>`;
      wireAmenityCheckboxes();
      break;

    case 4:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Property Photos</h2>
          <p>Upload a cover photo and additional images of the rooms.</p>
        </div>
        <div class="f-grid">
          <div class="field f-col-2">
            <label>Cover Photo (Main Display Image)</label>
            <input type="file" id="coverImageInput" name="coverImage" accept="image/*" />
          </div>
          <div class="field f-col-2" style="margin-top:10px;">
            <label>Additional Photos</label>
            <div class="upload-box" id="dropZone">
              <div class="upload-icon">📸</div>
              <div class="upload-title">Drag & drop photos here, or <b>browse files</b></div>
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
          <h2>Special Features & Highlights</h2>
          <p>Add tags for unique highlights like Braai areas, Study hubs, or Game rooms.
             You can attach photos to each one afterwards from the Manage Property page.</p>
        </div>
        <div class="f-grid">
          <div class="field f-col-2">
            <label>Add Custom Special Feature</label>
            <div style="display:flex; gap:10px;">
              <input type="text" id="featureInput" placeholder="e.g. Braai Area, Quiet Study Lounge" />
              <button type="button" class="btn btn-secondary" onclick="addFeatureTag()">+ Add</button>
            </div>
            <div class="feature-tag-wrap" id="featureTagList"></div>
          </div>
        </div>`;
      renderFeatureTags();
      break;

    case 6:
      panel.innerHTML = `
        <div class="panel-header">
          <h2>Review Listing Details</h2>
          <p>Double-check all information before publishing your property.</p>
        </div>
        <div class="review-table">
          <div class="review-row"><span class="review-key">Title</span><span class="review-val">${escapeHtml(state.title) || '—'}</span></div>
          <div class="review-row"><span class="review-key">Room Type</span><span class="review-val">${escapeHtml(state.type) || '—'}</span></div>
          <div class="review-row"><span class="review-key">Capacity</span><span class="review-val">${escapeHtml(state.capacity) || '—'} student(s)</span></div>
          <div class="review-row"><span class="review-key">Monthly Rent</span><span class="review-val price">R${escapeHtml(state.rent || '0')} / mo</span></div>
          <div class="review-row"><span class="review-key">Deposit</span><span class="review-val">R${escapeHtml(state.deposit || '0')}</span></div>
          <div class="review-row"><span class="review-key">Location</span><span class="review-val">${escapeHtml(state.address)}, ${escapeHtml(state.city)}</span></div>
          <div class="review-row"><span class="review-key">Commute</span><span class="review-val">${escapeHtml(state.commuteType) || '—'}</span></div>
          <div class="review-row"><span class="review-key">Amenities</span><span class="review-val">${selectedAmenityIds.size} selected</span></div>
          <div class="review-row"><span class="review-key">Special Features</span><span class="review-val">${customFeatures.length ? customFeatures.map(escapeHtml).join(', ') : '—'}</span></div>
          <div class="review-row"><span class="review-key">Additional Photos</span><span class="review-val">${accumulatedImages.length} attached</span></div>
        </div>`;
      break;
  }

  updateNavButtons();
}

// ── Amenities: built from the real DB categories passed in by Thymeleaf ──
function renderAmenityCategories() {
  const categoryNames = Object.keys(AMENITY_CATEGORIES);
  if (categoryNames.length === 0) {
    return `<p style="color:#999;">No amenities configured yet. Ask an admin to add some to the "amenity" table.</p>`;
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
  // special features (Braai Area, Study Hub, etc.) before deciding whether
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
    alert('Please complete step 1 basic info first.');
    return;
  }
  currentStep = idx;
  renderTracker();
  renderStepContent();
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
      nextBtn.textContent = '🎉 Publish Listing';
      nextBtn.onclick = () => {
        setSubmitAction('submit');
        const form = document.getElementById('listPropertyForm');
        if (form) form.requestSubmit(); // fires the 'submit' listener, unlike form.submit()
      };
    } else {
      nextBtn.textContent = 'Continue →';
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
}

document.addEventListener('DOMContentLoaded', () => {
  renderTracker();
  renderStepContent();

  const form = document.getElementById('listPropertyForm');
  if (form) {
    // Native 'submit' fires for BOTH the Draft button (type=submit) and the
    // Publish button once it becomes type=submit on the final step — so this
    // one listener covers every real way the form can be sent.
    form.addEventListener('submit', function(e) {
      prepareFormForSubmit(this);
    });
  }
});