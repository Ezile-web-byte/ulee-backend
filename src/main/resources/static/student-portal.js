/* ULEE student portal: small helpers for the "My Stay" pages.
   Place at: src/main/resources/static/student-portal.js
   (loaded with `defer`, so the DOM is ready when this runs) */
(function () {
    // Live character counter:  <textarea data-counter="#comment-count" maxlength="600">
    document.querySelectorAll('textarea[data-counter]').forEach(function (ta) {
        var out = document.querySelector(ta.getAttribute('data-counter'));
        if (!out) return;
        var max = ta.maxLength > 0 ? ta.maxLength : null;
        function update() { out.textContent = ta.value.length + (max ? ' / ' + max : ''); }
        ta.addEventListener('input', update);
        update();
    });

    // Count the metric numbers up from 0 (whole numbers only, never rounds a decimal)
    var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!reduce) {
        document.querySelectorAll('.ulee-metric-value').forEach(function (el) {
            var raw = el.textContent.trim();
            if (!/^\d+$/.test(raw)) return;
            var target = parseInt(raw, 10);
            if (target <= 0) return;
            var start = null, dur = 900;
            el.textContent = '0';
            function tick(now) {
                if (start === null) start = now;
                var t = Math.min((now - start) / dur, 1);
                el.textContent = Math.round(target * (1 - Math.pow(1 - t, 3)));
                if (t < 1) requestAnimationFrame(tick);
            }
            requestAnimationFrame(tick);
        });
    }
})();