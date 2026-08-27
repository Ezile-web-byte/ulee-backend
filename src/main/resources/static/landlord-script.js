
// ==========================================================
// ULEE — Landlord Dashboard (My Properties)
// Handles client-side filtering, sorting, and dynamic metric navigation.
// ==========================================================

let activeFilter = "all";
let activeSort = "date-added";

function getCards() {
  return Array.from(document.querySelectorAll("#propertyGrid .property-card"));
}

function applyFilterAndSort() {
  const cards = getCards();
  const grid = document.getElementById("propertyGrid");
  const emptyState = document.getElementById("filterEmptyState");

  // Filter logic
  let visibleCount = 0;
  cards.forEach(card => {
    const matches = activeFilter === "all" || card.dataset.status === activeFilter;
    card.style.display = matches ? "" : "none";
    if (matches) visibleCount++;
  });

  if (emptyState) {
    emptyState.style.display = visibleCount === 0 ? "block" : "none";
  }

  // Sort logic
  const sorted = cards.slice().sort((a, b) => {
    if (activeSort === "name-asc") {
      return a.dataset.name.localeCompare(b.dataset.name);
    }
    if (activeSort === "rent-desc") {
      return parseFloat(b.dataset.rent) - parseFloat(a.dataset.rent);
    }
    return 0; // "date-added" — preserves server sequence
  });

  sorted.forEach(card => grid.appendChild(card));
}

function initFilters() {
  const group = document.getElementById("filterGroup");
  if (!group) return;

  group.addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;

    group.querySelectorAll(".chip").forEach(c => {
      c.classList.remove("chip--active");
      c.setAttribute("aria-selected", "false");
    });
    chip.classList.add("chip--active");
    chip.setAttribute("aria-selected", "true");

    activeFilter = chip.dataset.filter;
    applyFilterAndSort();
  });
}

function initSort() {
  const select = document.getElementById("sortSelect");
  if (!select) return;

  select.addEventListener("change", (e) => {
    activeSort = e.target.value;
    applyFilterAndSort();
  });
}

function initMetricLinks() {
  // "Awaiting Review" now shows PROPERTIES pending admin approval (see
  // PropertyController.viewLandlordDashboard's awaitingApprovalCount), so
  // clicking it should filter this same page down to Pending Approval —
  // not navigate away to Applications, which is a different concept
  // entirely (student applications waiting on the landlord, not
  // properties waiting on the admin).
  const pendingCard = document.getElementById("statPendingApplications")?.closest(".stat-card");
  if (pendingCard) {
    pendingCard.style.cursor = "pointer";
    pendingCard.addEventListener("click", () => {
      const pendingChip = document.querySelector('.chip[data-filter="pending"]');
      if (pendingChip) {
        pendingChip.click(); // reuses the exact same logic as clicking the chip by hand
      }
      document.getElementById("propertyGrid")?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }

  const totalCard = document.getElementById("statTotalProperties")?.closest(".stat-card");
  if (totalCard) {
    totalCard.style.cursor = "pointer";
    totalCard.addEventListener("click", () => {
      document.getElementById("propertyGrid")?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }
}

document.addEventListener("DOMContentLoaded", () => {
  initFilters();
  initSort();
  initMetricLinks();
});