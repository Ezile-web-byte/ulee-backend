 /* ── Sidebar toggle ─────────────────────── */
  const sidebar = document.getElementById('sidebar');
  document.getElementById('sidebar-toggle').addEventListener('click', () => {
    sidebar.classList.toggle('collapsed');
  });

  /* ── Navigation ─────────────────────────── */
  const topbarTitles = {
    dashboard: 'Dashboard <span>— Overview</span>',
    listings:  'Listings <span>— All Properties</span>',
    users:     'Users <span>— All Accounts</span>',
    reviews:   'Reviews <span>— Moderation</span>',
    settings:  'Settings <span>— Platform Config</span>',
  };

  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
      const section = item.dataset.section;
      if (!section) return;
      document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
      item.classList.add('active');
      document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
      const sec = document.getElementById('sec-' + section);
      if (sec) sec.classList.add('active');
      document.getElementById('topbar-title').innerHTML = topbarTitles[section] || 'Dashboard';
    });
  });

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