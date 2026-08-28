/* ── Sidebar toggle ─────────────────────── */
const sidebar = document.getElementById('sidebar');
document.getElementById('sidebar-toggle').addEventListener('click', () => {
  sidebar.classList.toggle('collapsed');
});

/* ── Navigation ─────────────────────────── */
const topbarTitles = {
  dashboard: 'Dashboard <span>— Overview</span>',
  'review-properties': 'Review Properties <span>— Pending Listings</span>',
  listings:  'Listings <span>— All Properties</span>',
  users:     'Users <span>— All Accounts</span>',
  reviews:   'Reviews <span>— Moderation</span>',
  settings:  'Settings <span>— Platform Config</span>',
};

function activateSection(section) {
  if (!section) return;
  const navItem = document.querySelector(`.nav-item[data-section="${section}"]`);
  const sec = document.getElementById('sec-' + section);
  if (!navItem || !sec) return; // e.g. "listings" only exists as its own page, not a section here
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  navItem.classList.add('active');
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  sec.classList.add('active');
  const titleEl = document.getElementById('topbar-title');
  if (titleEl) titleEl.innerHTML = topbarTitles[section] || 'Dashboard';
}

document.querySelectorAll('.nav-item').forEach(item => {
  item.addEventListener('click', () => {
    const section = item.dataset.section;
    if (!section) return;
    activateSection(section);
    // Keep the URL hash in sync so a page reload (e.g. after submitting
    // the Review Properties filter form) lands back on this section
    // instead of resetting to the Dashboard.
    history.replaceState(null, '', '#' + section);
  });
});

// On page load, honor a #section hash in the URL (set by the filter form,
// or by links like "Back" that point to /admin-index#review-properties).
if (window.location.hash) {
  activateSection(window.location.hash.slice(1));
}

document.querySelectorAll('.see-all[data-goto]').forEach(btn => {
  btn.addEventListener('click', () => {
    const target = btn.dataset.goto;
    const navItem = document.querySelector(`.nav-item[data-section="${target}"]`);
    if (navItem) navItem.click();
  });
});

/* ── Dashboard sub-tabs ─────────────────── */
document.querySelectorAll('#sec-dashboard .tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('#sec-dashboard .tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
  });
});

/* ── Toggle switches ────────────────────── */
document.querySelectorAll('.toggle').forEach(toggle => {
  toggle.addEventListener('click', () => {
    toggle.classList.toggle('on');
  });
});