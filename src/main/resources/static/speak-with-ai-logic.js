// Exposed as window.SwaiLogic in the browser, and via module.exports for Node.
// Pure, DOM-free logic for the "Speak with AI" widget.
const SwaiLogic = {
  /** For all inputs, returns true only for strings that are empty or all-whitespace. */
  isBlank(text) {
    return typeof text !== 'string' || text.trim().length === 0;
  },

  /** For all message arrays and a new message, returns a NEW array with it appended (no mutation). */
  appendMessage(messages, message) {
    return [...messages, message];
  },

  /** For all boolean panel states, returns the inverted state. */
  toggleOpen(isOpen) {
    return !isOpen;
  },

  /**
   * Resolves whether the widget operates in Property_Context or General_Context.
   * Property_Context requires ALL of: isPropertyPage is true, propertyId is not
   * null/undefined, AND propertyTitle is non-blank (per isBlank above).
   */
  resolveContext({ isPropertyPage, propertyId, propertyTitle }) {
    const hasUsableTitle = !this.isBlank(propertyTitle);
    if (isPropertyPage && propertyId != null && hasUsableTitle) {
      return { mode: 'property', propertyId, propertyTitle: propertyTitle.trim() };
    }
    return { mode: 'general' };
  },

  /**
   * Pure placeholder response builder — no I/O, no timers.
   * In Property_Context, embeds context.propertyTitle verbatim.
   * In General_Context (or any other mode), never references a specific property.
   */
  buildPlaceholderResponse(userText, context) {
    if (context.mode === 'property') {
      return `Thanks for asking about "${context.propertyTitle}"! I'm a placeholder assistant for now, ` +
             `but soon I'll be able to answer real questions about this listing.`;
    }
    return `Thanks for your message! I'm a placeholder assistant for now, but real AI answers are coming soon.`;
  }
};

if (typeof module !== 'undefined' && module.exports) module.exports = SwaiLogic;
if (typeof window !== 'undefined') window.SwaiLogic = SwaiLogic;
