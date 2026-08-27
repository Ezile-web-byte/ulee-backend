// landlord-review-detail.js
// Powers /property-reviews/{id}: the property jump-menu, the clickable
// High/Poor rating stat cards that filter the review list, and the
// landlord respond/edit-response controls on each review card.
document.addEventListener('DOMContentLoaded', function () {
    const grid = document.getElementById('reviewListGrid');
    const emptyState = document.getElementById('reviewSortEmptyState');
    const cards = grid ? Array.from(grid.querySelectorAll('.review-card-wrap')) : [];

    // ── Property switcher: jump straight to another property's reviews ──
    const switcher = document.getElementById('propertySwitcher');
    if (switcher) {
        switcher.addEventListener('change', function () {
            const id = switcher.value;
            if (id) {
                window.location.href = '/property-reviews/' + encodeURIComponent(id);
            }
        });
    }

    // ── Rating filter (click High Ratings / Poor Ratings card; clicking
    //    the active one again, or clicking Average, resets to show all) ──
    const metricCards = document.querySelectorAll('.rating-metric-card');
    let activeRatingFilter = 'all';

    function applyRatingFilter(filter) {
        activeRatingFilter = filter;

        metricCards.forEach(function (card) {
            const isActive = filter !== 'all' && card.dataset.ratingFilter === filter;
            card.classList.toggle('rating-metric-card--active', isActive);
        });

        cards.forEach(function (card) {
            const rating = parseFloat(card.dataset.rating);
            let visible = true;
            if (filter === 'high') {
                visible = rating >= 3;
            } else if (filter === 'poor') {
                visible = rating < 3;
            }
            card.style.display = visible ? '' : 'none';
        });

        updateEmptyState();
    }

    function updateEmptyState() {
        if (!emptyState) return;
        const anyVisible = cards.some(function (card) {
            return card.style.display !== 'none';
        });
        emptyState.style.display = (cards.length > 0 && !anyVisible) ? '' : 'none';
    }

    metricCards.forEach(function (card) {
        card.addEventListener('click', function () {
            const filter = card.dataset.ratingFilter;
            const nextFilter = (filter === activeRatingFilter || filter === 'all') ? 'all' : filter;
            applyRatingFilter(nextFilter);
        });
    });

    // Newest-first by default (no visible sort control anymore, but this
    // keeps the list in a sensible order on load).
    if (grid) {
        const sorted = cards.slice().sort(function (a, b) {
            return new Date(b.dataset.date) - new Date(a.dataset.date);
        });
        sorted.forEach(function (card) {
            grid.appendChild(card);
        });
    }

    // ── Landlord review responses (respond / edit / cancel) ──
    if (grid) {
        grid.addEventListener('click', function (e) {
            const openBtn = e.target.closest('.review-respond-btn, .review-response-edit-btn');
            if (openBtn) {
                const form = openBtn.closest('.review-card').querySelector('.review-response-form');
                if (form) form.classList.add('is-open');
                return;
            }
            const cancelBtn = e.target.closest('.review-response-cancel');
            if (cancelBtn) {
                const form = cancelBtn.closest('.review-response-form');
                if (form) form.classList.remove('is-open');
            }
        });
    }
});