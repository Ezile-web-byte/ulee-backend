// ==========================================================
// ULEE — Landlord Dashboard (My Properties)
// Cards are now server-rendered by Thymeleaf in landlord-index.html.
// This script only filters/sorts/handles the already-rendered DOM.
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

  // Filter
  let visibleCount = 0;
  cards.forEach(card => {
    const matches = activeFilter === "all" || card.dataset.status === activeFilter;
    card.style.display = matches ? "" : "none";
    if (matches) visibleCount++;
  });

  emptyState.style.display = visibleCount === 0 ? "block" : "none";

  // Sort (re-append in new order; hidden cards move with their sorted position, harmless)
  const sorted = cards.slice().sort((a, b) => {
    if (activeSort === "name-asc") {
      return a.dataset.name.localeCompare(b.dataset.name);
    }
    if (activeSort === "rent-desc") {
      return parseFloat(b.dataset.rent) - parseFloat(a.dataset.rent);
    }
    return 0; // "date-added" — keep server-provided order
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
  const pendingCard = document.getElementById("statPendingApplications")?.closest(".stat-card");
  if (pendingCard) {
    pendingCard.style.cursor = "pointer";
    pendingCard.addEventListener("click", () => {
      window.location.href = "/manage-applications";
    });
  }

  const totalCard = document.getElementById("statTotalProperties")?.closest(".stat-card");
  if (totalCard) {
    totalCard.style.cursor = "pointer";
    totalCard.addEventListener("click", () => {
      document.getElementById("propertyGrid").scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }
}

document.addEventListener("DOMContentLoaded", () => {
  initFilters();
  initSort();
  initMetricLinks();
});