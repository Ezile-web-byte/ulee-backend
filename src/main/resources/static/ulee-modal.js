/* ULEE shared modal: place in src/main/resources/static/js/ulee-modal.js
 *
 *   await UleeModal.confirm({ title, message, okText, cancelText, tone })  -> true / false
 *   await UleeModal.alert({ title, message, list, tone })                  -> true
 *   await UleeModal.choice({ title, message, buttons: [{label, value, style}] }) -> value ('cancel' on Esc/backdrop)
 *   UleeModal.collectProblems(containerEl)                                  -> ["Field: message", ...]
 *
 * tone: 'info' (default) | 'warn' | 'danger'
 * Any element with data-confirm is intercepted automatically (see bottom of file).
 */
window.UleeModal = window.UleeModal || (() => {
    const ICONS = { info: '✨', warn: '⚠️', danger: '🗑️' };
    let overlay, box, iconEl, titleEl, msgEl, listEl, actionsEl;
    let resolver = null;
    let lastFocus = null;

    function build() {
        overlay = document.createElement('div');
        overlay.className = 'ul-modal-overlay';
        overlay.innerHTML =
            '<div class="ul-modal" role="dialog" aria-modal="true" aria-labelledby="ul-modal-title">' +
            '<div class="ul-modal-icon"></div>' +
            '<h3 class="ul-modal-title" id="ul-modal-title"></h3>' +
            '<p class="ul-modal-message"></p>' +
            '<ul class="ul-modal-list" hidden></ul>' +
            '<div class="ul-modal-actions"></div>' +
            '</div>';
        document.body.appendChild(overlay);
        box = overlay.firstChild;
        iconEl = box.querySelector('.ul-modal-icon');
        titleEl = box.querySelector('.ul-modal-title');
        msgEl = box.querySelector('.ul-modal-message');
        listEl = box.querySelector('.ul-modal-list');
        actionsEl = box.querySelector('.ul-modal-actions');

        overlay.addEventListener('mousedown', (e) => { if (e.target === overlay) close('cancel'); });
        document.addEventListener('keydown', (e) => {
            if (!overlay.classList.contains('is-open')) return;
            if (e.key === 'Escape') close('cancel');
            if (e.key === 'Tab') trapFocus(e);
        });
    }

    function trapFocus(e) {
        const btns = actionsEl.querySelectorAll('button');
        if (!btns.length) return;
        const first = btns[0], last = btns[btns.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    }

    function close(value) {
        if (!overlay || !overlay.classList.contains('is-open')) return;
        overlay.classList.remove('is-open');
        document.body.style.overflow = '';
        const r = resolver; resolver = null;
        if (lastFocus && lastFocus.focus) lastFocus.focus();
        if (r) r(value);
    }

    function show({ icon, title = '', message = '', list = [], tone = 'info', buttons }) {
        if (!overlay) build();
        if (resolver) close('cancel'); // never stack two modals

        lastFocus = document.activeElement;
        box.className = 'ul-modal' + (tone === 'danger' ? ' is-danger' : tone === 'warn' ? ' is-warn' : '');
        iconEl.textContent = icon || ICONS[tone] || ICONS.info;
        titleEl.textContent = title;
        msgEl.textContent = message;
        msgEl.hidden = !message;

        listEl.innerHTML = '';
        listEl.hidden = !list.length;
        list.forEach((t) => { const li = document.createElement('li'); li.textContent = t; listEl.appendChild(li); });

        actionsEl.innerHTML = '';
        buttons.forEach((b) => {
            const el = document.createElement('button');
            el.type = 'button';
            el.className = 'ul-btn-' + (b.style || 'primary');
            el.textContent = b.label;
            el.addEventListener('click', () => close(b.value));
            actionsEl.appendChild(el);
        });

        overlay.classList.add('is-open');
        document.body.style.overflow = 'hidden';
        // focus the safest button: the last "primary/danger" is the action, so focus the first (cancel) for destructive tones
        const focusBtn = tone === 'danger' ? actionsEl.firstChild : actionsEl.lastChild;
        if (focusBtn) focusBtn.focus();

        return new Promise((resolve) => { resolver = resolve; });
    }

    function confirm(o = {}) {
        return show({
            ...o,
            buttons: [
                { label: o.cancelText || 'Cancel', value: false, style: 'ghost' },
                { label: o.okText || 'Confirm', value: true, style: o.tone === 'danger' ? 'danger' : 'primary' },
            ],
        }).then((v) => v === true);
    }

    function alert(o = {}) {
        return show({ ...o, buttons: [{ label: o.okText || 'OK', value: true, style: 'primary' }] }).then(() => true);
    }

    function choice(o = {}) { return show(o); }

    // Reads the browser's built-in validation (required, min, max, maxlength, pattern, setCustomValidity)
    function collectProblems(container) {
        const problems = [];
        container.querySelectorAll('.ul-invalid').forEach((f) => f.classList.remove('ul-invalid'));
        container.querySelectorAll('input, select, textarea').forEach((f) => {
            if (f.disabled || f.type === 'hidden' || f.checkValidity()) return;
            const label =
                f.dataset.label ||
                (f.id && container.querySelector('label[for="' + f.id + '"]')?.textContent.trim()) ||
                f.closest('.form-group, .field')?.querySelector('label')?.textContent.trim() ||
                f.name || 'Field';
            problems.push(label.replace(/\s*\*\s*$/, '') + ': ' + f.validationMessage);
            f.classList.add('ul-invalid');
        });
        const firstBad = container.querySelector('.ul-invalid');
        if (firstBad) firstBad.scrollIntoView({ block: 'center', behavior: 'smooth' });
        return problems;
    }

    return { confirm, alert, choice, collectProblems };
})();

/* Auto-wiring (wired once, even if this file is loaded twice): add data-confirm to ANY link or button and it asks first.
 * Optional attributes: data-confirm-title, data-confirm-message, data-confirm-ok,
 * data-confirm-cancel, data-confirm-tone (info|warn|danger), data-confirm-icon
 * Runs in the capture phase, so existing onclick handlers on that element wait until the person confirms. */
if (!window.__uleeConfirmWired) {
    window.__uleeConfirmWired = true;
    document.addEventListener('click', async (e) => {
        const el = e.target.closest('[data-confirm]');
        if (!el || el._ulConfirmed) return;
        e.preventDefault();
        e.stopImmediatePropagation();
        const d = el.dataset;
        const ok = await UleeModal.confirm({
            title: d.confirmTitle || 'Are you sure?',
            message: d.confirmMessage || '',
            okText: d.confirmOk,
            cancelText: d.confirmCancel,
            tone: d.confirmTone,
            icon: d.confirmIcon,
        });
        if (!ok) return;
        el._ulConfirmed = true;
        el.click();
        el._ulConfirmed = false;
    }, true);
}