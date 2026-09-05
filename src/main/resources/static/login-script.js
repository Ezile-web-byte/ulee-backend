// ─────────────────────────────
// 🔹 PANEL SWITCHING
// ─────────────────────────────
function switchPanel(panel) {
  document.querySelectorAll('.sa-panel').forEach(p => p.classList.remove('active'));

  const target = document.getElementById('panel-' + panel);
  if (target) target.classList.add('active');

  hideToast();
}

function showForgot() {
  switchPanel('forgot');
}

// ─────────────────────────────
// 🔹 TOAST SYSTEM
// ─────────────────────────────
function showToast(msg, type) {
  const t = document.getElementById('toast');
  if (!t) return;

  t.textContent = msg;
  t.className = 'sa-toast ' + type;
}

function hideToast() {
  const toast = document.getElementById('toast');
  if (toast) toast.className = 'sa-toast';
}

// ─────────────────────────────
// 🔹 PASSWORD TOGGLE
// ─────────────────────────────
function togglePassword(inputId, button) {
  const input = document.getElementById(inputId);
  if (!input) return;

  const isHidden = input.type === 'password';
  input.type = isHidden ? 'text' : 'password';

  if (button) {
    button.classList.toggle('is-visible', isHidden);
    button.setAttribute('aria-label', isHidden ? 'Hide password' : 'Show password');
    button.setAttribute('aria-pressed', String(isHidden));
  }
}

// ─────────────────────────────
// 🔹 HELPERS
// ─────────────────────────────
function getFieldValue(primaryId, fallbackId) {
  const primary = document.getElementById(primaryId);
  const fallback = document.getElementById(fallbackId);
  const field = primary || fallback;
  return field ? field.value.trim() : '';
}

// ─────────────────────────────
// 🔹 AUTH HANDLERS (FRONTEND)
// ─────────────────────────────
function handleLogin() {
  const email = getFieldValue('login-email', 'login-user');
  const p = document.getElementById('login-pass')?.value;

  if (!email || !p) {
    showToast('Please enter your email and password.', 'info');
    return;
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    showToast('Please enter a valid email address.', 'info');
    return;
  }

  // Build and submit a real form POST to Spring Security's /login endpoint,
  // since this page has no native <form> wrapper around these inputs.
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = '/login';

  const userInput = document.createElement('input');
  userInput.type = 'hidden';
  userInput.name = 'username';
  userInput.value = email;
  form.appendChild(userInput);

  const passInput = document.createElement('input');
  passInput.type = 'hidden';
  passInput.name = 'password';
  passInput.value = p;
  form.appendChild(passInput);

  document.body.appendChild(form);
  form.submit();
}

function handleRegister() {
  const fn = document.getElementById('reg-fname')?.value.trim();
  const em = getFieldValue('reg-email', 'reg-user');
  const pw = document.getElementById('reg-pass')?.value;

  if (!fn || !em || !pw) {
    showToast('Please fill in all fields.', 'info');
    return;
  }

  if (pw.length < 8) {
    showToast('Password must be at least 8 characters.', 'info');
    return;
  }

  showToast('Account created! Welcome, ' + fn + '.', 'success');
}

function handleForgot() {
  const v = getFieldValue('forgot-email', 'forgot-val');

  if (!v) {
    showToast('Please enter your email.', 'info');
    return;
  }

  showToast('Reset link sent.', 'success');
}

// ─────────────────────────────
// 🔹 CARD CLICK → DETAILS PAGE
// ─────────────────────────────
function initCards() {
  document.querySelectorAll(".prop-card").forEach(card => {

    card.addEventListener("click", (e) => {

      // ❌ Prevent Apply button from triggering redirect
      if (e.target.classList.contains("card-cta")) return;

      const name = card.querySelector(".card-name")?.innerText;
      const location = card.querySelector(".card-meta")?.innerText;
      const price = card.querySelector(".card-price")?.innerText;
      const image = card.querySelector("img")?.src;

      const description = "Student accommodation with great facilities";

      const data = { name, location, price, image, description };

      localStorage.setItem("selectedAccommodation", JSON.stringify(data));

      window.location.href = "details.html";
    });

  });
}

// ─────────────────────────────
// 🔹 INIT WHEN PAGE LOADS
// ─────────────────────────────
document.addEventListener("DOMContentLoaded", () => {

  // Initialize login/register panels
  const initialPanel = window.location.hash.replace('#', '');
  if (['login', 'register', 'forgot'].includes(initialPanel)) {
    switchPanel(initialPanel);
  }

  // Initialize cards
  initCards();
});