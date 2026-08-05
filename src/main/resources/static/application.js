const AVATARS = ['#2DA8A8','#5B8AF0','#E8834A','#8B5CF6','#E85A7A'];

const applications = [
  {
    id: 1,
    name: 'Thandiwe Nkosi',
    initials: 'TN',
    email: 'thandiwe.nkosi@mandela.ac.za',
    phone: '074 312 8890',
    year: '2nd Year',
    funding: 'NSFAS',
    fundingClass: 'funding-nsfas',
    property: 'Greenfields Residence',
    location: 'North End, Port Elizabeth',
    moveIn: '1 Feb 2026',
    duration: '12 months',
    occupants: 1,
    status: 'pending',
    message: "I am a diligent student studying Social Work and have maintained my NSFAS funding in good standing. I would love the opportunity to stay at Greenfields as it is close to campus and I have no transport.",
    notes: "Prefers a quiet environment. Currently staying with relatives in KwaDwesi and commuting daily. Available to move in from 1 February.",
    avatarColor: '#2DA8A8',
  },
  {
    id: 2,
    name: 'Lethiwe Dlamini',
    initials: 'LD',
    email: 'lethiwe.d@mandela.ac.za',
    phone: '082 567 4321',
    year: '1st Year',
    funding: 'Bursary',
    fundingClass: 'funding-bursary',
    property: 'Vista Student Flats',
    location: 'Summerstrand, PE',
    moveIn: '15 Jan 2026',
    duration: '10 months',
    occupants: 2,
    status: 'accepted',
    message: "I recently received a merit bursary from my faculty and I am looking for a safe and comfortable place to stay with a fellow student. Vista is ideally located and we are both responsible tenants.",
    notes: "Will be sharing with Mpho Khumalo (also an NMU student). Both are non-smokers and keep tidy living spaces.",
    avatarColor: '#5B8AF0',
  },
  {
    id: 3,
    name: 'Sipho Mthembu',
    initials: 'SM',
    email: 'sipho.mthembu@mandela.ac.za',
    phone: '063 988 1122',
    year: 'Final Year',
    funding: 'Self-funded',
    fundingClass: 'funding-self',
    property: 'Summerstrand Suites',
    location: 'Summerstrand, PE',
    moveIn: '1 Mar 2026',
    duration: '8 months',
    occupants: 1,
    status: 'rejected',
    message: "As a final year Engineering student I need stable accommodation for my last semester and exam period. I work part-time and can cover rent independently.",
    notes: "Part-time employed at a local engineering firm. Looking for month-to-month flexibility after 8 months.",
    avatarColor: '#E8834A',
  },
  {
    id: 4,
    name: 'Ayanda Cele',
    initials: 'AC',
    email: 'ayanda.cele@mandela.ac.za',
    phone: '071 203 5544',
    year: '3rd Year',
    funding: 'Private',
    fundingClass: 'funding-private',
    property: 'Greenfields Residence',
    location: 'North End, Port Elizabeth',
    moveIn: '1 Feb 2026',
    duration: '12 months',
    occupants: 1,
    status: 'pending',
    message: "My parents are sponsoring my studies and accommodation. I study Information Technology and spend most of my time on campus or studying at home. I am a very neat and responsible tenant.",
    notes: "Parents will co-sign the lease. Student is from Durban and needs accommodation for the full academic year.",
    avatarColor: '#8B5CF6',
  },
  {
    id: 5,
    name: 'Nokwanda Zulu',
    initials: 'NZ',
    email: 'nokwanda.z@mandela.ac.za',
    phone: '065 441 9900',
    year: '2nd Year',
    funding: 'NSFAS',
    fundingClass: 'funding-nsfas',
    property: 'Vista Student Flats',
    location: 'Summerstrand, PE',
    moveIn: '1 Feb 2026',
    duration: '12 months',
    occupants: 1,
    status: 'pending',
    message: "I am currently on the NSFAS accommodation allowance and looking for a place that is safe and well-connected to NMU's main campus. I maintain a clean record and have references from previous accommodation.",
    notes: "Has a reference from previous landlord. Studying Education — attends campus every day.",
    avatarColor: '#E85A7A',
  },
];

let currentModalId = null;

function fundingLabel(app) {
  return `<span class="funding-tag ${app.fundingClass}">${app.funding}</span>`;
}

function statusBadge(status) {
  const map = {
    pending: 'badge-pending',
    accepted: 'badge-accepted',
    rejected: 'badge-rejected',
  };
  const labels = { pending: 'Pending', accepted: 'Accepted', rejected: 'Rejected' };
  return `<span class="status-badge ${map[status]}">${labels[status]}</span>`;
}

function buildCard(app) {
  const disabled = app.status !== 'pending' ? 'disabled' : '';
  return `
  <div class="app-card" data-id="${app.id}" data-status="${app.status}" data-property="${app.property}" data-name="${app.name.toLowerCase()}">
    <div class="card-top">
      <div class="student-avatar" style="background:${app.avatarColor}">${app.initials}</div>
      <div class="student-info">
        <div class="student-name">
          ${app.name}
          ${statusBadge(app.status)}
        </div>
        <div class="student-meta">
          <span>
            <svg viewBox="0 0 16 16"><path d="M2 4l6 5 6-5"/><rect x="2" y="3" width="12" height="10" rx="1.5"/></svg>
            ${app.email}
          </span>
          <span>
            <svg viewBox="0 0 16 16"><path d="M4 2h2l1 3-1.5 1.5a9 9 0 004 4L11 9l3 1v2a1 1 0 01-1 1A13 13 0 013 3a1 1 0 011-1z"/></svg>
            ${app.phone}
          </span>
        </div>
      </div>
    </div>
    <div class="card-body">
      <div class="info-block">
        <div class="info-label">Year of study</div>
        <div class="info-value">${app.year}</div>
      </div>
      <div class="info-block">
        <div class="info-label">Funding</div>
        <div class="info-value">${fundingLabel(app)}</div>
      </div>
      <div class="info-block">
        <div class="info-label">Move-in date</div>
        <div class="info-value">${app.moveIn}</div>
      </div>
      <div class="info-block">
        <div class="info-label">Duration</div>
        <div class="info-value">${app.duration}</div>
      </div>
      <div class="info-block">
        <div class="info-label">Occupants</div>
        <div class="info-value">${app.occupants} person${app.occupants > 1 ? 's' : ''}</div>
      </div>
    </div>
    <div class="msg-preview">${app.message.substring(0, 110)}${app.message.length > 110 ? '…' : ''}</div>
    <div class="card-footer">
      <div class="prop-info">
        <svg viewBox="0 0 16 16"><path d="M2 14V6l6-4 6 4v8"/><path d="M6 14v-4h4v4"/></svg>
        <strong>${app.property}</strong> &mdash; ${app.location}
      </div>
      <div class="card-actions">
        <button class="btn btn-view" onclick="openModal(${app.id})">View details</button>
        <button class="btn btn-accept" ${disabled} onclick="updateStatus(${app.id},'accepted')">Accept</button>
        <button class="btn btn-reject" ${disabled} onclick="updateStatus(${app.id},'rejected')">Reject</button>
      </div>
    </div>
  </div>`;
}

function renderCards() {
  const list = document.getElementById('cardsList');
  list.innerHTML = applications.map(buildCard).join('');
  updateStats();
}

function applyFilters() {
  const search   = document.getElementById('searchInput').value.toLowerCase();
  const property = document.getElementById('propertyFilter').value;
  const status   = document.getElementById('statusFilter').value;

  const cards = document.querySelectorAll('.app-card');
  let visible = 0;

  cards.forEach(card => {
    const nameMatch = card.dataset.name.includes(search);
    const propMatch = !property || card.dataset.property === property;
    const statMatch = !status  || card.dataset.status  === status;

    const show = nameMatch && propMatch && statMatch;
    card.style.display = show ? '' : 'none';
    if (show) visible++;
  });

  document.getElementById('emptyState').style.display = visible === 0 ? 'block' : 'none';
}

function updateStats() {
  document.getElementById('countPending').textContent  = applications.filter(a => a.status === 'pending').length;
  document.getElementById('countAccepted').textContent = applications.filter(a => a.status === 'accepted').length;
  document.getElementById('countRejected').textContent = applications.filter(a => a.status === 'rejected').length;
}

function updateStatus(id, newStatus) {
  const app = applications.find(a => a.id === id);
  if (!app || app.status !== 'pending') return;
  app.status = newStatus;
  renderCards();
  applyFilters();
  showToast(newStatus === 'accepted'
    ? `✓ Application accepted for ${app.name}`
    : `✕ Application rejected for ${app.name}`,
    newStatus);
  if (currentModalId === id) closeModalDirect();
}

function openModal(id) {
  const app = applications.find(a => a.id === id);
  if (!app) return;
  currentModalId = id;

  document.getElementById('modalTitle').textContent = `${app.name}'s Application`;

  const acceptBtn = document.getElementById('modalAcceptBtn');
  const rejectBtn = document.getElementById('modalRejectBtn');
  acceptBtn.disabled = app.status !== 'pending';
  rejectBtn.disabled = app.status !== 'pending';

  document.getElementById('modalBody').innerHTML = `
    <div class="modal-student-row">
      <div class="student-avatar" style="background:${app.avatarColor};width:48px;height:48px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:17px;font-weight:600;color:white;flex-shrink:0;">${app.initials}</div>
      <div>
        <div class="modal-student-name">${app.name} ${statusBadge(app.status)}</div>
        <div class="modal-student-sub">${app.email} &middot; ${app.phone}</div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Academic &amp; funding</div>
      <div class="modal-grid">
        <div class="modal-field">
          <div class="modal-field-label">Year of study</div>
          <div class="modal-field-value">${app.year}</div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Funding source</div>
          <div class="modal-field-value">${fundingLabel(app)}</div>
        </div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Move-in preferences</div>
      <div class="modal-grid">
        <div class="modal-field">
          <div class="modal-field-label">Move-in date</div>
          <div class="modal-field-value">${app.moveIn}</div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Duration</div>
          <div class="modal-field-value">${app.duration}</div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Occupants</div>
          <div class="modal-field-value">${app.occupants} person${app.occupants > 1 ? 's' : ''}</div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Property</div>
          <div class="modal-field-value">${app.property}</div>
        </div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Student message</div>
      <div class="modal-message">${app.message}</div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Additional notes</div>
      <div class="modal-notes">${app.notes}</div>
    </div>
  `;

  document.getElementById('modalOverlay').classList.add('open');
}

function closeModal(e) {
  if (e.target === document.getElementById('modalOverlay')) closeModalDirect();
}
function closeModalDirect() {
  document.getElementById('modalOverlay').classList.remove('open');
  currentModalId = null;
}

function modalAction(type) {
  if (currentModalId) updateStatus(currentModalId, type === 'accept' ? 'accepted' : 'rejected');
}

let toastTimer;
function showToast(msg, type) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast show ' + (type || '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.className = 'toast', 3200);
}

renderCards();