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
   * propertyAddress/propertyCommuteType/propertyFeatures are optional extras
   * used only to answer the Property_Context category chips below — a missing
   * one just means that category falls back to "not listed yet".
   */
  resolveContext({ isPropertyPage, propertyId, propertyTitle, propertyAddress, propertyCommuteType, propertyFeatures }) {
    const hasUsableTitle = !this.isBlank(propertyTitle);
    if (isPropertyPage && propertyId != null && hasUsableTitle) {
      return {
        mode: 'property',
        propertyId,
        propertyTitle: propertyTitle.trim(),
        propertyAddress: this.isBlank(propertyAddress) ? null : propertyAddress.trim(),
        propertyCommuteType: this.isBlank(propertyCommuteType) ? null : propertyCommuteType.trim(),
        propertyFeatures: this.isBlank(propertyFeatures) ? null : propertyFeatures.trim()
      };
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
  },

  // ------------------------------------------------------------------
  // Category-guided listing matcher (General_Context only).
  //
  // The assistant walks the student through a small question tree —
  // budget, room type, commute — instead of open-ended free text, then
  // filters approved listings down to matches. Each node has a `prompt`
  // and a list of `options`; an option either points deeper into the tree
  // (`next`) or is terminal and narrows listings via `test`.
  //
  // Field names below (rent, type, commuteType) match ListingSummaryDTO /
  // Property exactly, since listings come straight from GET /api/listings
  // (see SwaiApiController) with no reshaping on the client.
  // ------------------------------------------------------------------
  CATEGORIES: {
    root: {
      prompt: 'What matters most in your search?',
      options: [
        { label: '💰 Rent budget', next: 'rent' },
        { label: '🛏️ Room type', next: 'roomType' },
        { label: '🚌 Getting to campus', next: 'commute' }
      ]
    },
    rent: {
      prompt: "What's your monthly budget?",
      options: [
        { label: 'Under R2 000', test: (l) => Number(l.rent) < 2000 },
        { label: 'Under R2 500', test: (l) => Number(l.rent) < 2500 },
        { label: 'Under R3 000', test: (l) => Number(l.rent) < 3000 }
      ]
    },
    roomType: {
      prompt: 'What kind of room are you after?',
      options: [
        { label: 'Single room', test: (l) => /single/i.test(l.type || '') },
        { label: 'Sharing (2)', test: (l) => /sharing.*2|2.*shar/i.test(l.type || '') },
        { label: 'Sharing (3+)', test: (l) => /sharing.*[3-9]|[3-9].*shar/i.test(l.type || '') }
      ]
    },
    commute: {
      prompt: 'How do you want to get to campus?',
      options: [
        { label: 'Walking distance', test: (l) => /walk/i.test(l.commuteType || '') },
        { label: 'Public transport', test: (l) => /public|taxi|bus/i.test(l.commuteType || '') },
        { label: 'Own car', test: (l) => /car|driv/i.test(l.commuteType || '') }
      ]
    }
  },

  /**
   * Placeholder listings, used only as a fallback when GET /api/listings
   * can't be reached (e.g. a static preview page with no backend). Shape
   * matches ListingSummaryDTO exactly so the same filter/render code works
   * for both real and sample data.
   */
  SAMPLE_LISTINGS: [
    { id: 1, title: 'The Dunes', address: '69 Zenios Place', city: 'Summerstrand', rent: 2800, type: 'Single Room', commuteType: 'Walking distance', imageUrl: null },
    { id: 2, title: 'Harbour View', address: '12 Marine Drive', city: 'Summerstrand', rent: 2200, type: 'Sharing (2)', commuteType: 'Walking distance', imageUrl: null },
    { id: 3, title: 'Campus Court', address: '4 University Way', city: 'Summerstrand', rent: 2450, type: 'Single Room', commuteType: 'Public transport', imageUrl: null },
    { id: 4, title: 'The Pines', address: '21 Beach Road', city: 'Humewood', rent: 3200, type: 'Single Room', commuteType: 'Own car', imageUrl: null },
    { id: 5, title: 'Kingsley Digs', address: '8 College Road', city: 'Summerstrand', rent: 1950, type: 'Sharing (3)', commuteType: 'Walking distance', imageUrl: null }
  ],

  /** Looks up a category node by key, or null if it doesn't exist. */
  getCategory(key) {
    return this.CATEGORIES[key] || null;
  },

  /** For a terminal option (has `test`), returns the listings that match. */
  matchListings(listings, option) {
    if (!Array.isArray(listings) || !option || typeof option.test !== 'function') return [];
    return listings.filter(option.test);
  },

  /** Short human-readable tags summarizing why a listing matched. */
  buildMatchTags(listing) {
    const tags = [];
    if (listing.rent != null) tags.push(`R${listing.rent}/mo`);
    if (listing.type) tags.push(listing.type);
    if (listing.commuteType) tags.push(listing.commuteType);
    return tags;
  },

  // ------------------------------------------------------------------
  // Property_Context category chips — answers questions about the ONE
  // listing the widget is embedded on, using only the fields passed in
  // via the fragment's data attributes (propertyAddress, propertyCommuteType,
  // propertyFeatures). No listing search/matching here — just a direct
  // answer built from that property's own data.
  // ------------------------------------------------------------------
  PROPERTY_CATEGORIES: {
    root: {
      prompt: 'What would you like to know about this property?',
      options: [
        { label: '✨ Special features', key: 'features' },
        { label: '🚌 Getting there', key: 'commute' },
        { label: '📍 Location', key: 'location' }
      ]
    }
  },

  /** Builds a direct answer for a Property_Context category, from context alone. */
  buildPropertyCategoryAnswer(key, context) {
    const title = context.propertyTitle || 'This property';
    switch (key) {
      case 'features':
        return context.propertyFeatures
            ? `${title} offers: ${context.propertyFeatures}.`
            : `${title} doesn't have any special features or amenities listed yet.`;
      case 'commute':
        return context.propertyCommuteType
            ? `Getting to campus from ${title}: ${context.propertyCommuteType}.`
            : `Commute details for ${title} haven't been listed yet — worth asking the landlord directly.`;
      case 'location':
        return context.propertyAddress
            ? `${title} is located at ${context.propertyAddress}.`
            : `The exact address for ${title} isn't available here yet.`;
      default:
        return `Not sure how to answer that about ${title} yet.`;
    }
  }
};

if (typeof module !== 'undefined' && module.exports) module.exports = SwaiLogic;
if (typeof window !== 'undefined') window.SwaiLogic = SwaiLogic;