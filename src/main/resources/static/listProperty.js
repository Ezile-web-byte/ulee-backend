// ── Profile dropdown ──
const profileArea = document.getElementById('profileArea');
const profileTrigger = document.getElementById('profileTrigger');
profileTrigger.addEventListener('click', e => {
  e.stopPropagation();
  profileArea.classList.toggle('open');
});
document.addEventListener('click', () => profileArea.classList.remove('open'));

// ── Fav toggle ──
function toggleFav(btn) { btn.classList.toggle('liked'); }

// ── Form open/close ──
const formBox = document.getElementById('formBox');
const addBtn = document.getElementById('addBtn');
const closeForm = document.getElementById('closeForm');

addBtn.addEventListener('click', () => {
  resetWizard();
  formBox.classList.add('visible');
  renderTrack(); renderPanel();
  formBox.scrollIntoView({ behavior: 'smooth', block: 'start' });
});
closeForm.addEventListener('click', () => formBox.classList.remove('visible'));

// ── Steps ──
const STEPS = [
  { label: 'Basic Info' },
  { label: 'Details' },
  { label: 'Amenities' },
  { label: 'Images' },
  { label: 'Description' },
  { label: 'Review' }
];

const AMENITIES = [
  { icon: '📶', label: 'WiFi' },
  { icon: '🅿️', label: 'Parking' },
  { icon: '🛋️', label: 'Furnished' },
  { icon: '🔒', label: 'Security' },
  { icon: '🧺', label: 'Laundry' },
  { icon: '🍳', label: 'Kitchen' },
  { icon: '🌿', label: 'Garden' },
  { icon: '📦', label: 'Storage' }
];

const PROPERTY_TYPES = ['Studio Apartment', 'Shared House', 'En-suite Room', 'Purpose-built Block', 'Terraced House'];

let currentStep = 0;
let checkedAmenities = new Set();

// ── One shared object that actually holds what the user typed ──
let formData = {
  title: '',
  type: PROPERTY_TYPES[0],
  address: '',
  city: '',
  bedrooms: 2,
  bathrooms: 1,
  rent: '',
  availableFrom: '',
  description: ''
};
let uploadedFiles = []; // real File objects, not just preview thumbnails

function resetWizard() {
  currentStep = 0;
  checkedAmenities = new Set();
  uploadedFiles = [];
  formData = {
    title: '',
    type: PROPERTY_TYPES[0],
    address: '',
    city: '',
    bedrooms: 2,
    bathrooms: 1,
    rent: '',
    availableFrom: '',
    description: ''
  };
}
let mainImageIndex = 0; // tracks which uploadedFiles[] index is the front photo

function renderThumbs() {
  const strip = document.getElementById('thumbStrip');
  if (!strip) return;
  strip.innerHTML = '';
  uploadedFiles.forEach((f, idx) => {
    const reader = new FileReader();
    reader.onload = e => {
      const wrap = document.createElement('div');
      wrap.style.cssText = 'position:relative;display:inline-block;';

      const img = document.createElement('img');
      img.className = 'thumb-img';
      img.src = e.target.result;
      if (idx === mainImageIndex) img.style.outline = '3px solid var(--primary)';
      wrap.appendChild(img);

      const mainBtn = document.createElement('button');
      mainBtn.type = 'button';
      mainBtn.textContent = idx === mainImageIndex ? '★ Main' : '☆';
      mainBtn.title = 'Set as front/main photo';
      mainBtn.style.cssText = 'position:absolute;bottom:-6px;left:0;right:0;font-size:10px;background:var(--primary);color:#fff;border:none;border-radius:4px;cursor:pointer;padding:2px 0;';
      mainBtn.onclick = () => { mainImageIndex = idx; renderThumbs(); };
      wrap.appendChild(mainBtn);

      const rm = document.createElement('button');
      rm.type = 'button';
      rm.textContent = '×';
      rm.style.cssText = 'position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;background:#c0392b;color:#fff;border:none;cursor:pointer;font-size:12px;';
      rm.onclick = () => {
        uploadedFiles.splice(idx, 1);
        if (mainImageIndex === idx) mainImageIndex = 0;
        else if (mainImageIndex > idx) mainImageIndex--;
        renderThumbs();
      };
      wrap.appendChild(rm);

      strip.appendChild(wrap);
    };
    reader.readAsDataURL(f);
  });
}

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// ── Reads whatever's currently on screen into formData before we navigate away ──
function saveCurrentStepData() {
  switch (currentStep) {
    case 0:
      formData.title = (document.getElementById('in_title')?.value || '').trim();
      formData.type = document.getElementById('in_type')?.value || formData.type;
      formData.city = (document.getElementById('in_city')?.value || '').trim();
      formData.address = (document.getElementById('in_address')?.value || '').trim();
      break;
    case 1:
      formData.bedrooms = document.getElementById('in_bedrooms')?.value || formData.bedrooms;
      formData.bathrooms = document.getElementById('in_bathrooms')?.value || formData.bathrooms;
      formData.rent = document.getElementById('in_rent')?.value || formData.rent;
      formData.availableFrom = document.getElementById('in_availableFrom')?.value || formData.availableFrom;
      break;
    case 4:
      formData.description = (document.getElementById('in_description')?.value || '').trim();
      break;
      // step 2 (amenities) and step 3 (images) already write straight into
      // checkedAmenities / uploadedFiles as you interact with them, so nothing to grab here
  }
}

const PANELS = [
  // 0 – Basic info (single panel — the duplicate has been removed)
  () => `
    <div class="f-grid f-grid-2">
      <div class="field f-col">
        <label>Property Title</label>
        <input type="text" id="in_title" placeholder="e.g. Riverside Student Studio" value="${escapeHtml(formData.title)}" />
      </div>
      <div class="field">
        <label>Property Type</label>
        <select id="in_type">
          ${PROPERTY_TYPES.map(t => `<option ${formData.type === t ? 'selected' : ''}>${t}</option>`).join('')}
        </select>
      </div>
      <div class="field">
        <label>City</label>
        <input type="text" id="in_city" placeholder="e.g. Gqeberha" value="${escapeHtml(formData.city)}" />
      </div>
      <div class="field f-col">
        <label>Full Address</label>
        <input type="text" id="in_address" placeholder="14 Campus Road, Gqeberha" value="${escapeHtml(formData.address)}" />
      </div>
    </div>`,

  // 1 – Details
  () => `
    <div class="f-grid f-grid-3">
      <div class="field">
        <label>Bedrooms</label>
        <input type="number" id="in_bedrooms" min="1" max="20" value="${formData.bedrooms}" />
      </div>
      <div class="field">
        <label>Bathrooms</label>
        <input type="number" id="in_bathrooms" min="1" max="10" value="${formData.bathrooms}" />
      </div>
      <div class="field">
        <label>Monthly Rent (R)</label>
        <input type="number" id="in_rent" placeholder="4500" value="${escapeHtml(String(formData.rent))}" />
      </div>
      <div class="field f-col">
        <label>Available From</label>
        <input type="date" id="in_availableFrom" value="${formData.availableFrom}" />
      </div>
    </div>`,

  // 2 – Amenities
  () => `
    <div class="amenity-grid">
      ${AMENITIES.map((a, i) => `
        <label class="amenity-tile ${checkedAmenities.has(i) ? 'checked' : ''}" id="am${i}" onclick="toggleAm(${i})">
          <input type="checkbox" ${checkedAmenities.has(i) ? 'checked' : ''}>
          <span class="a-icon">${a.icon}</span>
          <span class="a-lbl">${a.label}</span>
        </label>`).join('')}
    </div>`,

  // 3 – Images
  () => `
    <div class="drop-zone" id="dropZone" onclick="document.getElementById('fileIn').click()">
      <div class="drop-icon-big">🖼️</div>
      <div class="drop-text">
        Drag &amp; drop photos here, or <b onclick="event.stopPropagation();document.getElementById('fileIn').click()">browse files</b><br>
        <span style="font-size:12px;color:#aaa;margin-top:4px;display:block">PNG, JPG, WEBP — up to 10 files</span>
      </div>
      <input type="file" id="fileIn" multiple accept="image/*" style="display:none" onchange="previewFiles(this.files)">
    </div>
    <div class="thumb-strip" id="thumbStrip"></div>`,

  // 4 – Description
  () => `
    <div class="f-grid">
      <div class="field f-col">
        <label>Property Description</label>
        <textarea id="in_description" rows="7" placeholder="Describe your property — highlight key features, proximity to universities, transport links, house rules, and what makes this a great home for students…">${escapeHtml(formData.description)}</textarea>
      </div>
    </div>`,

  // 5 – Review (built from real data)
  () => {
    const amenityLabels = [...checkedAmenities].map(i => AMENITIES[i].label).join(', ') || 'None selected';
    return `
    <div class="review-card">
      <div class="review-row"><span class="rv-key">Title</span><span class="rv-val">${escapeHtml(formData.title) || '—'}</span></div>
      <div class="review-row"><span class="rv-key">Type</span><span class="rv-val">${escapeHtml(formData.type)}</span></div>
      <div class="review-row"><span class="rv-key">City</span><span class="rv-val">${escapeHtml(formData.city) || '—'}</span></div>
      <div class="review-row"><span class="rv-key">Address</span><span class="rv-val">${escapeHtml(formData.address) || '—'}</span></div>
      <div class="review-row"><span class="rv-key">Beds / Baths</span><span class="rv-val">${formData.bedrooms} bed · ${formData.bathrooms} bath</span></div>
      <div class="review-row"><span class="rv-key">Monthly Rent</span><span class="rv-val price">R${escapeHtml(String(formData.rent || '0'))} / month</span></div>
      <div class="review-row"><span class="rv-key">Available From</span><span class="rv-val">${formData.availableFrom || '—'}</span></div>
      <div class="review-row"><span class="rv-key">Amenities</span><span class="rv-val">${amenityLabels}</span></div>
      <div class="review-row"><span class="rv-key">Photos</span><span class="rv-val">${uploadedFiles.length} uploaded</span></div>
      <div class="review-row"><span class="rv-key">Description</span><span class="rv-val" style="font-style:italic;font-size:12.5px;color:#888">${escapeHtml(formData.description) || '—'}</span></div>
    </div>`;
  }
];

function renderTrack() {
  const t = document.getElementById('stepTrack');
  t.innerHTML = STEPS.map((s, i) => `
    <div class="step-unit">
      <div class="step-col">
        <div class="step-node ${i < currentStep ? 'done' : i === currentStep ? 'active' : ''}">
          ${i < currentStep ? '✓' : i + 1}
        </div>
        <div class="step-lbl ${i === currentStep ? 'active' : ''}">${s.label}</div>
      </div>
      ${i < STEPS.length - 1 ? `<div class="step-line ${i < currentStep ? 'done' : ''}"></div>` : ''}
    </div>`).join('');
}

function renderPanel() {
  document.getElementById('panelWrap').innerHTML = PANELS[currentStep]();
  document.getElementById('prevBtn').style.display = currentStep === 0 ? 'none' : 'inline-flex';
  const nb = document.getElementById('nextBtn');
  nb.textContent = currentStep === STEPS.length - 1 ? '🎉 Publish Listing' : 'Continue →';

  // drag-drop events for step 3
  if (currentStep === 3) {
    renderThumbs(); // redraw any photos already added on a previous visit to this step
    const dz = document.getElementById('dropZone');
    dz && dz.addEventListener('dragover', e => { e.preventDefault(); dz.classList.add('dragover'); });
    dz && dz.addEventListener('dragleave', () => dz.classList.remove('dragover'));
    dz && dz.addEventListener('drop', e => {
      e.preventDefault(); dz.classList.remove('dragover');
      previewFiles(e.dataTransfer.files);
    });
  }
}

function goStep(dir) {
  // capture whatever's currently typed before we navigate anywhere, forward or back
  saveCurrentStepData();

  if (dir > 0 && currentStep === STEPS.length - 1) {
    publishListing();
    return;
  }

  currentStep = Math.max(0, Math.min(STEPS.length - 1, currentStep + dir));
  renderTrack(); renderPanel();
}

function toggleAm(i) {
  if (checkedAmenities.has(i)) checkedAmenities.delete(i);
  else checkedAmenities.add(i);
  const el = document.getElementById('am' + i);
  if (el) el.classList.toggle('checked');
}

// ── Images: keep the real File objects (not just previews) so they survive step navigation ──
function previewFiles(files) {
  const room = 10 - uploadedFiles.length;
  if (room <= 0) return;
  Array.from(files).slice(0, room).forEach(f => uploadedFiles.push(f));
  renderThumbs();
}

function renderThumbs() {
  const strip = document.getElementById('thumbStrip');
  if (!strip) return;
  strip.innerHTML = '';
  uploadedFiles.forEach((f, idx) => {
    const reader = new FileReader();
    reader.onload = e => {
      const wrap = document.createElement('div');
      wrap.style.position = 'relative';
      wrap.style.display = 'inline-block';

      const img = document.createElement('img');
      img.className = 'thumb-img';
      img.src = e.target.result;
      wrap.appendChild(img);

      const rm = document.createElement('button');
      rm.type = 'button';
      rm.textContent = '×';
      rm.title = 'Remove photo';
      rm.style.cssText = 'position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;background:#c0392b;color:#fff;border:none;cursor:pointer;font-size:12px;line-height:1;';
      rm.onclick = () => { uploadedFiles.splice(idx, 1); renderThumbs(); };
      wrap.appendChild(rm);

      strip.appendChild(wrap);
    };
    reader.readAsDataURL(f);
  });
}

// ── Real submission to the Spring Boot backend (POST /list-property) ──
// Uses a native hidden <form> instead of fetch() so the browser handles the
// multipart file upload correctly and follows the server's redirect on its own.
function publishListing() {
  if (!formData.title || !formData.rent) {
    alert('Please fill in at least a property title and monthly rent before publishing.');
    currentStep = 0;
    renderTrack(); renderPanel();
    return;
  }

  const furnished = [...checkedAmenities].some(i => AMENITIES[i].label === 'Furnished');

  const form = document.createElement('form');
  form.method = 'POST';
  form.action = '/list-property';
  form.enctype = 'multipart/form-data';
  form.style.display = 'none';

  const addField = (name, value) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  };

  addField('title', formData.title);
  addField('type', formData.type);
  addField('city', formData.city);
  addField('address', formData.address);
  addField('bedrooms', formData.bedrooms);
  addField('bathrooms', formData.bathrooms);
  addField('rent', formData.rent);
  addField('availableFrom', formData.availableFrom);
  addField('description', formData.description);
  addField('furnished', furnished);

  if (uploadedFiles.length > 0) {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.name = 'images';
    fileInput.multiple = true;

    const dt = new DataTransfer();
    uploadedFiles.forEach(f => dt.items.add(f));
    fileInput.files = dt.files;

    form.appendChild(fileInput);
  }

  document.body.appendChild(form);
  form.submit(); // navigates the browser to /list-property, backend redirects to /landlord-index on success
}

// init
renderTrack(); renderPanel();