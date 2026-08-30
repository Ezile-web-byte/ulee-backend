// Shared toast notification helper for ULEE. Reads a `?toast=xxx` query
// param on page load, shows a small confirmation banner for a few seconds,
// then removes it and cleans the query param out of the URL (so refreshing
// the page doesn't re-show the same toast).
(function () {
  const TOAST_MESSAGES = {
    applied: { text: '✓ Application submitted!', type: 'success' },
    cancelled: { text: '✓ Application has been cancelled successfully.', type: 'info' },
    'document-submitted': { text: '✓ Document submitted!', type: 'success' },
    saved: { text: '❤️ Saved to your favorites', type: 'success' },
    unsaved: { text: 'Removed from saved properties', type: 'info' }
  };

  function showToast(message, type) {
    const el = document.createElement('div');
    el.className = 'ulee-toast ulee-toast-' + type;
    el.textContent = message;
    document.body.appendChild(el);
    requestAnimationFrame(() => el.classList.add('ulee-toast-show'));
    setTimeout(() => {
      el.classList.remove('ulee-toast-show');
      setTimeout(() => el.remove(), 300);
    }, 3000);
  }

  const params = new URLSearchParams(window.location.search);
  const toastKey = params.get('toast');
  if (toastKey && TOAST_MESSAGES[toastKey]) {
    const { text, type } = TOAST_MESSAGES[toastKey];
    showToast(text, type);
    params.delete('toast');
    const newUrl = window.location.pathname + (params.toString() ? '?' + params.toString() : '') + window.location.hash;
    window.history.replaceState({}, '', newUrl);
  }

  // ── Loading-state helper: any form with data-loading-text will disable
  // its submit button and swap its text while the request is in flight.
  document.querySelectorAll('form[data-loading-text]').forEach(form => {
    form.addEventListener('submit', () => {
      const btn = form.querySelector('button[type="submit"]');
      if (btn) {
        btn.dataset.originalText = btn.textContent;
        btn.textContent = form.dataset.loadingText;
        btn.disabled = true;
        btn.style.opacity = '0.7';
        btn.style.cursor = 'not-allowed';
      }
    });
  });
})();
