// Required-field validation for Save Changes on edit-property.html.
// Extracted from an inline <script> block.
//
// "Save Changes" is BLOCKED entirely if any required field is missing
// or invalid — the click is cancelled, the landlord is jumped to the
// step containing the first unanswered required field, that field is
// highlighted red and scrolled into view. "Save Draft" (elsewhere on
// this page) is completely untouched by this script — it always saves
// whatever's filled in, no validation, exactly as before.
(function () {
  const REQUIRED_FIELDS_EDIT = [
    { name: 'title', label: 'Property Title', step: 1, validate: v => !!(v && v.trim().length > 0) },
    { name: 'capacity', label: 'Capacity', step: 1, validate: v => !!(v && Number(v) >= 1) },
    { name: 'rent', label: 'Monthly Rent', step: 2, validate: v => !!(v && Number(v) > 0) },
    { name: 'address', label: 'Full Address', step: 2, validate: v => !!(v && v.trim().length > 0) }
  ];

  function fieldEl(name) {
    return document.querySelector(`#editPropertyForm [name="${name}"]`);
  }

  function getInvalidFields() {
    return REQUIRED_FIELDS_EDIT.filter(f => {
      const el = fieldEl(f.name);
      const val = el ? el.value : '';
      return !f.validate(val);
    });
  }

  function highlightInvalid(fields) {
    document.querySelectorAll('#editPropertyForm .field-invalid').forEach(el => el.classList.remove('field-invalid'));
    fields.forEach(f => {
      const el = fieldEl(f.name);
      if (el) el.classList.add('field-invalid');
    });
  }

  // Reveals the given step. Tries the REAL step-tracker's own click
  // handler first (whatever edit-property.js wired it to), so the
  // page's internal "current step" state stays in sync instead of
  // this script fighting it. Falls back to toggling the .active
  // class directly (same pattern the creation wizard uses) if no
  // tracker nodes are found.
  function revealStep(stepIndex) {
    const trackerNodes = document.querySelectorAll('#editStepsTracker .step-node');
    if (trackerNodes && trackerNodes.length > stepIndex) {
      trackerNodes[stepIndex].click();
      return;
    }
    document.querySelectorAll('.edit-step').forEach(el => {
      el.classList.toggle('active', Number(el.dataset.step) === stepIndex);
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    const saveChangesBtn = document.getElementById('saveChangesBtn');
    if (!saveChangesBtn) return;

    saveChangesBtn.addEventListener('click', function (e) {
      const invalid = getInvalidFields();
      if (invalid.length > 0) {
        e.preventDefault(); // block the save — do not submit
        e.stopImmediatePropagation();
        highlightInvalid(invalid);

        const firstInvalid = invalid[0];
        revealStep(firstInvalid.step);

        // Give the step a moment to actually become visible
        // before scrolling/focusing the field inside it.
        setTimeout(() => {
          const el = fieldEl(firstInvalid.name);
          if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.focus();
          }
        }, 60);

        alert('These required fields (marked *) still need valid values:\n\n' +
            invalid.map(f => '• ' + f.label).join('\n') +
            '\n\nFill them in to save changes, or use "Save Draft" to keep your progress without publishing yet.');
      } else {
        highlightInvalid([]);
        // Not forced invalid — let the click proceed normally.
        // formAction stays whatever it already is ("update"),
        // since nothing here ever sets it to "draft" anymore.
      }
    });

    // Live-clear the red border the moment a field becomes valid again.
    REQUIRED_FIELDS_EDIT.forEach(f => {
      const el = fieldEl(f.name);
      if (el) {
        el.addEventListener('input', () => {
          if (f.validate(el.value)) el.classList.remove('field-invalid');
        });
      }
    });
  });
})();