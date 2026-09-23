// Collapse/expand behaviour for the shared landlord sidebar
// (fragments/landlord-sidebar.html). Include this script on every
// landlord page that renders that fragment.
(function () {
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('sidebar-toggle');
    const appShell = document.querySelector('.app-shell');
    if (!sidebar || !toggleBtn || !appShell) return;

    function setCollapsed(collapsed) {
        // .collapsed drives the icon-only rail styling on the sidebar
        // itself; .sidebar-collapsed on the shell resizes the grid
        // column (see landlord-style.css) so .main actually reflows
        // into the freed space instead of leaving a gap.
        sidebar.classList.toggle('collapsed', collapsed);
        appShell.classList.toggle('sidebar-collapsed', collapsed);
        toggleBtn.setAttribute('aria-expanded', String(!collapsed));
    }

    // Remember the collapsed/expanded state across page loads and
    // across pages, so navigating between My Properties / Applications /
    // Reviews doesn't reset it.
    const STORAGE_KEY = 'ulee-sidebar-collapsed';
    if (localStorage.getItem(STORAGE_KEY) === 'true') {
        setCollapsed(true);
    }

    toggleBtn.addEventListener('click', function () {
        const collapsed = !sidebar.classList.contains('collapsed');
        setCollapsed(collapsed);
        localStorage.setItem(STORAGE_KEY, String(collapsed));
    });
})();