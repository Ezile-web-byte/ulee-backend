function initReviewFilters() {
    const group = document.getElementById("reviewFilterGroup");
    const grid = document.getElementById("reviewSummaryGrid");
    const emptyState = document.getElementById("reviewFilterEmptyState");
    if (!group || !grid) return;

    function applyFilter(filter) {
        const cards = Array.from(grid.querySelectorAll(".review-summary-card"));
        let visibleCount = 0;
        cards.forEach(card => {
            const matches = filter === "all" || card.dataset.status === filter;
            card.style.display = matches ? "" : "none";
            if (matches) visibleCount++;
        });
        if (emptyState) emptyState.style.display = visibleCount === 0 ? "block" : "none";
    }

    group.addEventListener("click", (e) => {
        const chip = e.target.closest(".chip");
        if (!chip) return;

        group.querySelectorAll(".chip").forEach(c => {
            c.classList.remove("chip--active");
            c.setAttribute("aria-selected", "false");
        });
        chip.classList.add("chip--active");
        chip.setAttribute("aria-selected", "true");

        applyFilter(chip.dataset.filter);
    });
}

document.addEventListener("DOMContentLoaded", initReviewFilters);