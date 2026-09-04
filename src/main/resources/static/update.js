// edit-property.js — Manage Property, free-navigation editor.
//
// Unlike the creation wizard (listProperty.js), this page's HTML is fully
// server-rendered by Thymeleaf in one pass: every field for every step is
// already present in the DOM as a real named <input>/<select>/<textarea>
// from the moment the page loads. This script never removes or rebuilds
// those elements — it only toggles which .edit-step container is visible.
// That means there's no need to track a separate `state` object or
// re-inject hidden fields before submit (the pattern listProperty.js uses):
// every field the landlord has ever touched, on any step, is already
// sitting in the <form> and gets submitted together automatically on Save,
// no matter which step happens to be showing at the time.

const EDIT_STEPS = [
  { label: 'Review' },
  { label: 'Basic Info' },
  { label: 'Pricing & Location' },
  { label: 'Description' },
  { label: 'Amenities' },
  { label: 'Photos' }
];

let editCurrentStep = 0;

function renderEditTracker() {
  const tracker = document.getElementById('editStepsTracker');
  const fill = document.getElementById('editProgressFill');
  if (!tracker) return;

  if (fill) {
    const pct = ((editCurrentStep + 1) / EDIT_STEPS.length) * 100;
    fill.style.width = `${pct}%`;
  }

  // No "completed" checkmarks here (unlike the creation wizard) — this
  // editor is non-linear, so only the current step is highlighted.
  tracker.innerHTML = EDIT_STEPS.map((s, i) => `
    <div class="step-node ${i === editCurrentStep ? 'active' : ''}" onclick="goToEditStep(${i})">
      <div class="node-circle">${i + 1}</div>
      <div class="node-label">${s.label}</div>
    </div>
  `).join('');
}

// The only navigation function the rest of the page needs — tracker nodes,
// Review row clicks, and the optional Back/Next buttons all just call this.
function goToEditStep(idx) {
  editCurrentStep = Math.max(0, Math.min(EDIT_STEPS.length - 1, idx));

  document.querySelectorAll('.edit-step').forEach(el => {
    el.style.display = (parseInt(el.dataset.step, 10) === editCurrentStep) ? '' : 'none';
  });

  if (editCurrentStep === 0) renderReviewSummary();

  renderEditTracker();
  updateEditNavButtons();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function updateEditNavButtons() {
  const prevBtn = document.getElementById('editPrevBtn');
  const nextBtn = document.getElementById('editNextBtn');
  if (prevBtn) prevBtn.style.visibility = editCurrentStep === 0 ? 'hidden' : 'visible';
  if (nextBtn) nextBtn.style.visibility = editCurrentStep === EDIT_STEPS.length - 1 ? 'hidden' : 'visible';
}

// Same interaction as the creation wizard's tile selector: clicking a tile
// checks its radio and toggles the "selected" class among its siblings.
// Native :checked styling isn't used by listProperty.css, so this class
// toggle is required for the visual state to update.
function selectEditTile(element, fieldName) {
  const container = element.closest('.tile-selector');
  container.querySelectorAll('.tile-option').forEach(el => {
    el.classList.remove('selected');
    el.style.borderColor = '';
    el.style.background = '';
  });
  element.classList.add('selected');
  element.style.borderColor = '#005b74';
  element.style.background = '#eaf6f8';
  const input = element.querySelector('input');
  if (input) input.checked = true;
  if (editCurrentStep === 0) renderReviewSummary();
}

// Builds the live Review summary by reading straight off the real form
// fields — so it always reflects whatever the landlord has typed so far,
// even before Save is clicked. Each row is clickable and jumps to the step
// that field lives on.
function renderReviewSummary() {
  const container = document.getElementById('reviewSummaryBody');
  if (!container) return;

  const form = document.getElementById('editPropertyForm');
  const val = (name) => {
    const el = form.querySelector(`[name="${name}"]`);
    return el ? (el.value || '') : '';
  };
  const checkedRadioValue = (name) => {
    const el = form.querySelector(`input[name="${name}"]:checked`);
    return el ? el.value : '';
  };
  const selectedOptionText = (name) => {
    const el = form.querySelector(`select[name="${name}"]`);
    if (!el || el.selectedIndex < 0) return '';
    return el.options[el.selectedIndex].text || '';
  };

  const amenityLabels = Array.from(form.querySelectorAll('input[name="amenityIds"]:checked'))
      .map(cb => {
        const label = cb.closest('.amenity-card-item')?.querySelector('label');
        return label ? label.textContent.trim() : cb.value;
      });

  const photoCount = document.querySelectorAll('.gallery-item').length;

  const rows = [
    { label: 'Title', value: val('title') || '—', step: 1 },
    { label: 'Room Type', value: checkedRadioValue('type') || '—', step: 1 },
    { label: 'Monthly Rent', value: val('rent') ? `R${val('rent')}` : '—', step: 2 },
    { label: 'Deposit', value: val('deposit') ? `R${val('deposit')}` : '—', step: 2 },
    { label: 'Location', value: [val('address'), val('city')].filter(Boolean).join(', ') || '—', step: 2 },
    { label: 'Commute', value: checkedRadioValue('commuteType') || '—', step: 2 },
    { label: 'Available From', value: selectedOptionText('availableFrom') || 'Not set', step: 2 },
    { label: 'Description', value: val('description') ? (val('description').length > 80 ? val('description').slice(0, 80) + '…' : val('description')) : '—', step: 3 },
    { label: 'Amenities', value: amenityLabels.length ? `${amenityLabels.length} selected` : 'None selected', step: 4 },
    { label: 'Photos', value: `${photoCount} uploaded`, step: 5 }
  ];

  container.innerHTML = rows.map(r => `
    <div class="review-row" onclick="goToEditStep(${r.step})" title="Click to edit">
      <span class="review-key">${escapeHtmlEdit(r.label)}</span>
      <span class="review-val">${escapeHtmlEdit(r.value)}</span>
    </div>
  `).join('');
}

function escapeHtmlEdit(str) {
  if (!str) return '';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', () => {
  renderEditTracker();
  goToEditStep(0); // land on Review by default, per spec

  // Keep the Review view live: if the landlord jumps to a step, edits a
  // field, then jumps straight back to Review without visiting every step
  // in order, the summary should still reflect the edit.
  const form = document.getElementById('editPropertyForm');
  if (form) {
    form.addEventListener('input', () => {
      if (editCurrentStep === 0) renderReviewSummary();
    });
    form.addEventListener('change', () => {
      if (editCurrentStep === 0) renderReviewSummary();
    });
  }
});