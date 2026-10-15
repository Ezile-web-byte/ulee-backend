(function () {
  function build(sel) {
    if (sel.dataset.ulee || sel.multiple || sel.hasAttribute('data-native')) return;
    sel.dataset.ulee = '1';
    var cs = getComputedStyle(sel);
    var wrap = document.createElement('div');
    wrap.className = 'ulee-select';
    if (cs.display !== 'block') wrap.classList.add('ulee-select-inline');
    var w = sel.offsetWidth;
    if (w > 0) wrap.style.minWidth = w + 'px';

    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'ulee-select-btn';
    btn.setAttribute('aria-haspopup', 'listbox');
    if (sel.getAttribute('aria-label')) btn.setAttribute('aria-label', sel.getAttribute('aria-label'));
    var label = document.createElement('span');
    label.className = 'ulee-select-label';
    var arrow = document.createElement('span');
    arrow.className = 'ulee-select-arrow';
    btn.appendChild(label);
    btn.appendChild(arrow);
    var panel = document.createElement('div');
    panel.className = 'ulee-select-panel';
    panel.setAttribute('role', 'listbox');

    sel.parentNode.insertBefore(wrap, sel);
    wrap.appendChild(sel);
    wrap.appendChild(btn);
    wrap.appendChild(panel);
    sel.classList.add('ulee-select-native');
    sel.tabIndex = -1;

    function close() { wrap.classList.remove('open'); }
    function render() {
      panel.innerHTML = '';
      Array.prototype.forEach.call(sel.options, function (o, i) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'ulee-opt';
        b.textContent = o.textContent.trim();
        if (o.disabled) b.disabled = true;
        if (o.value === '') b.classList.add('ulee-opt-placeholder');
        if (i === sel.selectedIndex) b.classList.add('selected');
        b.addEventListener('click', function () {
          sel.selectedIndex = i;
          sel.dispatchEvent(new Event('change', { bubbles: true }));
          close();
          btn.focus();
        });
        panel.appendChild(b);
      });
    }
    function sync() {
      var o = sel.options[sel.selectedIndex];
      label.textContent = o ? o.textContent.trim() : '';
      label.classList.toggle('placeholder', !o || o.value === '');
      render();
    }
    sel.addEventListener('change', sync);
    btn.addEventListener('click', function () {
      var willOpen = !wrap.classList.contains('open');
      document.querySelectorAll('.ulee-select.open').forEach(function (s) { s.classList.remove('open'); });
      if (willOpen) wrap.classList.add('open');
    });
    wrap.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { close(); btn.focus(); }
    });
    document.addEventListener('click', function (e) {
      if (!wrap.contains(e.target)) close();
    });
    if (sel.id) {
      var lab = document.querySelector('label[for="' + sel.id + '"]');
      if (lab) lab.addEventListener('click', function (e) { e.preventDefault(); btn.focus(); });
    }
    sync();
  }
  function init() { document.querySelectorAll('select').forEach(build); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();