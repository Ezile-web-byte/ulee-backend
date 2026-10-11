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

  // Filter logic. data-status is one of: draft, pending, rejected, available, inactive
  let visibleCount = 0;
  cards.forEach(card => {
    const matches = activeFilter === "all" || card.dataset.status === activeFilter;
    card.style.display = matches ? "" : "none";
    if (matches) visibleCount++;
  });

  if (emptyState) {
    emptyState.style.display = visibleCount === 0 && cards.length > 0 ? "block" : "none";
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

function selectChip(filter) {
  const group = document.getElementById("filterGroup");
  if (!group) return;
  const chip = group.querySelector('.chip[data-filter="' + filter + '"]');
  if (!chip) return;

  group.querySelectorAll(".chip").forEach(c => {
    c.classList.remove("chip--active");
    c.setAttribute("aria-selected", "false");
  });
  chip.classList.add("chip--active");
  chip.setAttribute("aria-selected", "true");

  activeFilter = filter;
  applyFilterAndSort();
}

function initFilters() {
  const group = document.getElementById("filterGroup");
  if (!group) return;

  group.addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    selectChip(chip.dataset.filter);
  });

  // Allows links like /landlord-index?filter=rejected (e.g. from a notification).
  const wanted = new URLSearchParams(window.location.search).get("filter");
  if (wanted) selectChip(wanted);
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
  // "Awaiting Review" counts PROPERTIES pending admin approval, so clicking
  // it filters this same page to Pending Approval.
  const pendingCard = document.getElementById("statPendingApplications")?.closest(".stat-card");
  if (pendingCard) {
    pendingCard.style.cursor = "pointer";
    pendingCard.addEventListener("click", () => {
      selectChip("pending");
      document.getElementById("propertyGrid")?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }

  const totalCard = document.getElementById("statTotalProperties")?.closest(".stat-card");
  if (totalCard) {
    totalCard.style.cursor = "pointer";
    totalCard.addEventListener("click", () => {
      selectChip("all");
      document.getElementById("propertyGrid")?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }

  // The Vacant Rooms card is a real <a href="/vacant-rooms"> in the HTML,
  // so it needs no JavaScript here.
}

document.addEventListener("DOMContentLoaded", () => {
  initFilters();
  initSort();
  initMetricLinks();
});