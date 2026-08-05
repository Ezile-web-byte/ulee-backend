  /* ── Sidebar toggle ── */
  const sidebar  = document.getElementById('sidebar');
  const mainWrap = document.getElementById('mainWrap');
  const topbar   = document.getElementById('topbar');

  function toggleSidebar() {
    sidebar.classList.toggle('collapsed');
    const w = sidebar.classList.contains('collapsed')
      ? 'var(--sidebar-w-col)' : 'var(--sidebar-w)';
    mainWrap.style.marginLeft = w;
    topbar.style.left = w;
  }

  function navClick(el) {
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    el.classList.add('active');
  }

  /* ── Notification dropdown ── */
  const notifBtn = document.getElementById('notifBtn');
  const notifDD  = document.getElementById('notifDropdown');
  function toggleNotif() { notifBtn.classList.toggle('notif-open'); }
  document.addEventListener('click', e => {
    if (!notifBtn.contains(e.target)) notifBtn.classList.remove('notif-open');
  });

  /* ── Property data ── */
  const properties = [
    { name: 'Summerstrand Studio',      type: 'Studio Apartment',       loc: 'Summerstrand, Port Elizabeth', price: 4500, beds: 1, baths: 1, occ: 2, date: '2025-07-01', status: 'available', desc: 'Cozy studio apartment perfect for NMU students, located just 5 minutes from the main campus. Features high-speed WiFi, backup power, and a secure gate. The unit comes with a kitchenette, private bathroom, and study desk. Bills are included in the rent. Safe neighbourhood with 24/7 security cameras.' },
    { name: 'North End 2-Bed Apartment', type: '2-Bedroom Apartment',   loc: 'North End, Port Elizabeth',   price: 7200, beds: 2, baths: 1, occ: 4, date: '2025-08-01', status: 'rented',    desc: 'Spacious two-bedroom apartment in North End, close to transport links and NMU. Fully furnished with all major appliances. Shared kitchen and lounge area. Secure complex with parking.' },
    { name: 'Walmer Heights Room',       type: 'Room in Shared House',   loc: 'Walmer, Port Elizabeth',      price: 3100, beds: 1, baths: 1, occ: 1, date: '2025-06-15', status: 'available', desc: 'Single room in a quiet shared house in Walmer. Ideal for focused students. Comes with a study desk, wardrobe, and shared kitchen. Shuttle service nearby.' },
    { name: 'Central Campus Flat',       type: '1-Bedroom Apartment',    loc: 'Central, Port Elizabeth',     price: 5800, beds: 1, baths: 1, occ: 2, date: '2025-07-15', status: 'available', desc: 'Modern 1-bedroom flat centrally located. Walking distance to NMU. High-speed internet, backup power, and laundry facilities on site.' },
    { name: 'Newton Park Bachelor',      type: 'Bachelor Flat',          loc: 'Newton Park, Port Elizabeth', price: 2900, beds: 0, baths: 1, occ: 1, date: '2025-06-01', status: 'rented',    desc: 'Affordable bachelor flat great for first-year students. All utilities included. Close to public transport.' },
    { name: 'Greenacres Garden Unit',    type: '1-Bedroom Apartment',    loc: 'Greenacres, Port Elizabeth',  price: 6400, beds: 1, baths: 1, occ: 2, date: '2025-08-15', status: 'available', desc: 'Beautiful garden unit in Greenacres. Private garden patio, fully furnished, with study area and secure parking.' },
  ];

  let currentIdx = -1;

  function selectCard(el, idx) {
    /* Deselect all */
    document.querySelectorAll('.prop-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
    currentIdx = idx;
    loadForm(idx);
  }

  function loadForm(idx) {
    const p = properties[idx];
    document.getElementById('formTitle').textContent = 'Edit: ' + p.name;
    document.getElementById('formSubtitle').textContent = p.loc;
    document.getElementById('propTitle').value  = p.name;
    document.getElementById('propLoc').value    = p.loc;
    document.getElementById('propPrice').value  = p.price;
    document.getElementById('bedrooms').value   = p.beds;
    document.getElementById('bathrooms').value  = p.baths;
    document.getElementById('maxOcc').value     = p.occ;
    document.getElementById('availDate').value  = p.date;
    document.getElementById('propDesc').value   = p.desc;

    /* Set type */
    const sel = document.getElementById('propType');
    for (let i = 0; i < sel.options.length; i++) {
      if (sel.options[i].text === p.type) { sel.selectedIndex = i; break; }
    }

    /* Status toggle */
    setStatus(p.status);

    /* Show form */
    const wrap = document.getElementById('editFormWrap');
    wrap.classList.add('visible');
    setTimeout(() => wrap.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
  }

  function closeForm() {
    document.getElementById('editFormWrap').classList.remove('visible');
    document.querySelectorAll('.prop-card').forEach(c => c.classList.remove('selected'));
    currentIdx = -1;
  }

  /* ── Status toggle ── */
  function setStatus(val) {
    const a = document.getElementById('toggleAvail');
    const r = document.getElementById('toggleRented');
    a.className = 'toggle-opt' + (val === 'available' ? ' active-available' : '');
    r.className = 'toggle-opt' + (val === 'rented'    ? ' active-rented'    : '');
  }

  /* ── Amenity chips ── */
  function toggleChip(el) { el.classList.toggle('selected'); }

  /* ── Remove image ── */
  function removeImg(id) {
    const el = document.getElementById(id);
    if (el) { el.style.opacity = '0'; setTimeout(() => el.remove(), 200); }
  }

  /* ── Preview new images ── */
  function previewImages(e) {
    const grid = document.getElementById('imagesGrid');
    Array.from(e.target.files).forEach(file => {
      const reader = new FileReader();
      reader.onload = ev => {
        const uid = 'img_' + Date.now() + Math.random().toString(36).slice(2);
        const div = document.createElement('div');
        div.className = 'img-thumb'; div.id = uid;
        div.innerHTML = `<img src="${ev.target.result}" alt="new"/><div class="img-remove" onclick="removeImg('${uid}')">✕</div>`;
        grid.appendChild(div);
      };
      reader.readAsDataURL(file);
    });
  }

  /* ── Save changes ── */
  function saveChanges() {
    if (currentIdx < 0) return;
    properties[currentIdx].name  = document.getElementById('propTitle').value;
    properties[currentIdx].loc   = document.getElementById('propLoc').value;
    properties[currentIdx].price = document.getElementById('propPrice').value;
    properties[currentIdx].desc  = document.getElementById('propDesc').value;

    const now = new Date().toLocaleTimeString();
    document.getElementById('saveStatus').textContent = 'Last saved: ' + now;

    /* Update card visually */
    const cards = document.querySelectorAll('.prop-card');
    if (cards[currentIdx]) {
      cards[currentIdx].querySelector('.prop-card-title').textContent = properties[currentIdx].name;
      cards[currentIdx].querySelector('.prop-card-loc').innerHTML     = '📍 ' + properties[currentIdx].loc;
      cards[currentIdx].querySelector('.prop-price').innerHTML        = 'R' + Number(properties[currentIdx].price).toLocaleString() + ' <span>/month</span>';
    }

    showToast('✅ Property updated successfully!');
  }

  function showToast(msg) {
    const t = document.getElementById('toast');
    t.textContent = msg;
    t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 3000);
  }