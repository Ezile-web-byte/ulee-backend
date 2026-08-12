package com.ulee.ulee_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug condition exploration test — Property 1: Unified Design System Applied.
 *
 * <p>This is a BUGFIX exploration test for the {@code ui-consistency-fix} spec. It encodes the
 * EXPECTED (post-fix) behavior: every non-student surface (landlord, admin, login/register,
 * applications, update, reviews) must render with the student-page design system —
 * {@code Sora} / {@code Playfair Display} fonts, the canonical teal token scale
 * ({@code hsl(180,67%,47%)}) with no purple secondary, pill-shaped ({@code 50px}) action buttons,
 * and {@code 12px} cards.
 *
 * <p><b>On UNFIXED code these assertions MUST FAIL.</b> The failures are the counterexamples that
 * confirm the visual inconsistency exists (the divergent "Academic Vitality" palette, the wrong
 * {@code DM Sans}/{@code Fraunces}/{@code Plus Jakarta Sans}/{@code Hanken Grotesk} fonts, and the
 * non-pill {@code 7px}/{@code 8px} button radii). After the fix (task 4) the same test should PASS.
 *
 * <p><b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 1.9, 1.10, 1.11</b>
 */
class UiConsistencyDesignSystemTest {

    private static final Path STATIC_DIR = Path.of("src", "main", "resources", "static");

    // Canonical design-system values (post-fix targets).
    private static final String CANONICAL_PRIMARY = "hsl(180,67%,47%)";
    private static final String CANONICAL_PRIMARY_DARK = "hsl(180,67%,36%)";

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String readCss(String fileName) {
        Path p = STATIC_DIR.resolve(fileName);
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stylesheet: " + p, e);
        }
    }

    /** Lowercase + strip all whitespace so color/radius comparisons are format-insensitive. */
    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("\\s+", "");
    }

    /** Returns the value of a CSS custom property, e.g. cssVar(css, "--primary"). */
    private static String cssVar(String css, String name) {
        Matcher m = Pattern.compile(Pattern.quote(name) + "\\s*:\\s*([^;]+);").matcher(css);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Returns the body of the first rule for the given simple selector (e.g. "body", ".btn"). */
    private static String ruleBody(String css, String selector) {
        // Selector must stand alone: preceded by start/newline/brace/comma/space and followed by "{".
        Pattern p = Pattern.compile("(?:^|[\\n}\\s,])" + Pattern.quote(selector) + "\\s*\\{([^}]*)}");
        Matcher m = p.matcher(css);
        return m.find() ? m.group(1) : null;
    }

    /** Returns the border-radius value declared inside the given selector's rule body. */
    private static String borderRadiusOf(String css, String selector) {
        String body = ruleBody(css, selector);
        if (body == null) {
            return null;
        }
        Matcher m = Pattern.compile("border-radius\\s*:\\s*([^;]+);").matcher(body);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Returns the font-family value declared inside the given selector's rule body. */
    private static String fontFamilyOf(String css, String selector) {
        String body = ruleBody(css, selector);
        if (body == null) {
            return null;
        }
        Matcher m = Pattern.compile("font-family\\s*:\\s*([^;]+);").matcher(body);
        return m.find() ? m.group(1).trim() : null;
    }

    // ---------------------------------------------------------------------
    // Property 1 — fonts across non-student surfaces
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Landlord dashboard (landlord-style.css) uses Sora/Playfair Display fonts")
    void landlordDashboardFonts() {
        String css = readCss("landlord-style.css");
        String body = normalize(cssVar(css, "--font-body"));
        String display = normalize(cssVar(css, "--font-display"));

        assertAll(
                () -> assertTrue(body.contains("sora"),
                        "landlord --font-body must be 'Sora' but was: " + cssVar(css, "--font-body")),
                () -> assertTrue(display.contains("playfairdisplay"),
                        "landlord --font-display must be 'Playfair Display' but was: " + cssVar(css, "--font-display")),
                () -> assertTrue(!body.contains("hankengrotesk") && !display.contains("plusjakartasans"),
                        "landlord fonts still use the Academic Vitality families (Hanken Grotesk / Plus Jakarta Sans)")
        );
    }

    @Test
    @DisplayName("Manage properties (Manage properties.css) uses Sora/Playfair Display fonts")
    void managePropertiesFonts() {
        String css = readCss("Manage properties.css");
        String body = normalize(cssVar(css, "--font-body"));
        String display = normalize(cssVar(css, "--font-display"));

        assertAll(
                () -> assertTrue(body.contains("sora"),
                        "manage-properties --font-body must be 'Sora' but was: " + cssVar(css, "--font-body")),
                () -> assertTrue(display.contains("playfairdisplay"),
                        "manage-properties --font-display must be 'Playfair Display' but was: " + cssVar(css, "--font-display"))
        );
    }

    @Test
    @DisplayName("Landlord reviews (landlord-reviews.css) uses Playfair Display titles and Sora body")
    void landlordReviewsFonts() {
        String css = readCss("landlord-reviews.css");
        String titleFont = normalize(fontFamilyOf(css, ".review-group-title"));

        assertAll(
                () -> assertTrue(titleFont != null && titleFont.contains("playfairdisplay"),
                        ".review-group-title must use 'Playfair Display' but was: "
                                + fontFamilyOf(css, ".review-group-title")),
                () -> assertTrue(!normalize(css).contains("'plusjakartasans'"),
                        "landlord-reviews.css still hard-codes 'Plus Jakarta Sans'"),
                () -> assertTrue(!normalize(css).contains("'hankengrotesk'"),
                        "landlord-reviews.css still hard-codes 'Hanken Grotesk' (should be 'Sora')")
        );
    }

    @Test
    @DisplayName("Applications page (application.css) body font is Sora")
    void applicationsBodyFont() {
        String css = readCss("application.css");
        String bodyFont = normalize(fontFamilyOf(css, "body"));
        assertTrue(bodyFont != null && bodyFont.contains("sora"),
                "application.css body font must be 'Sora' but was: " + fontFamilyOf(css, "body"));
    }

    @Test
    @DisplayName("Update property page (update.css) uses Sora body and Playfair Display headings")
    void updateFonts() {
        String css = readCss("update.css");
        String bodyFont = normalize(fontFamilyOf(css, "body"));

        assertAll(
                () -> assertTrue(bodyFont != null && bodyFont.contains("sora"),
                        "update.css body font must be 'Sora' but was: " + fontFamilyOf(css, "body")),
                () -> assertTrue(!normalize(css).contains("'fraunces'"),
                        "update.css still uses 'Fraunces' for headings (should be 'Playfair Display')")
        );
    }

    @Test
    @DisplayName("List property page (listProperty.css) --font-body is Sora")
    void listPropertyFontBody() {
        String css = readCss("listProperty.css");
        String body = normalize(cssVar(css, "--font-body"));
        assertTrue(body.contains("sora"),
                "listProperty.css --font-body must be 'Sora' but was: " + cssVar(css, "--font-body"));
    }

    // ---------------------------------------------------------------------
    // Property 1 — colors, purple secondary, radii, pills (landlord surfaces)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Landlord surfaces use canonical teal tokens with no purple secondary")
    void landlordCanonicalTealNoPurple() {
        for (String file : new String[]{"landlord-style.css", "Manage properties.css"}) {
            String css = readCss(file);
            String norm = normalize(css);
            String primary = normalize(cssVar(css, "--primary"));
            String primaryDark = normalize(cssVar(css, "--primary-dark"));

            assertAll("teal tokens in " + file,
                    () -> assertTrue(CANONICAL_PRIMARY.equals(primary),
                            file + " --primary must be " + CANONICAL_PRIMARY + " but was: " + cssVar(css, "--primary")),
                    () -> assertTrue(CANONICAL_PRIMARY_DARK.equals(primaryDark),
                            file + " --primary-dark must be " + CANONICAL_PRIMARY_DARK + " but was: "
                                    + cssVar(css, "--primary-dark")),
                    () -> assertTrue(!norm.contains("#147592"),
                            file + " still uses the Academic Vitality deep teal #147592"),
                    () -> assertTrue(!norm.contains("#441587"),
                            file + " still uses the royal purple secondary #441587")
            );
        }
    }

    @Test
    @DisplayName("Landlord cards use 12px radius and action buttons are pill-shaped (50px)")
    void landlordCardRadiusAndPillButtons() {
        for (String file : new String[]{"landlord-style.css", "Manage properties.css"}) {
            String css = readCss(file);
            String radiusSm = normalize(cssVar(css, "--radius-sm"));
            String norm = normalize(css);

            assertAll("radii/pills in " + file,
                    () -> assertTrue("12px".equals(radiusSm),
                            file + " card radius (--radius-sm) must be 12px but was: " + cssVar(css, "--radius-sm")),
                    () -> assertTrue(!norm.contains("--radius-lg:24px"),
                            file + " still defines the 24px card radius (--radius-lg: 24px)"),
                    () -> assertTrue(norm.contains("50px"),
                            file + " has no pill-shaped (50px) action button radius")
            );
        }
    }

    // ---------------------------------------------------------------------
    // Property 1 — admin edge case: colors/fonts already correct, buttons must be pills
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Admin dashboard (admin-style.css) already uses Sora + teal (confirming pass)")
    void adminAlreadyOnDesignSystem() {
        String css = readCss("admin-style.css");
        String norm = normalize(css);
        assertAll(
                () -> assertTrue(norm.contains("font-family:'sora'"),
                        "admin-style.css should already use Sora"),
                () -> assertTrue(CANONICAL_PRIMARY.equals(normalize(cssVar(css, "--p"))),
                        "admin-style.css --p should already be " + CANONICAL_PRIMARY + " but was: " + cssVar(css, "--p"))
        );
    }

    @Test
    @DisplayName("Admin action buttons/tabs are pill-shaped (50px)")
    void adminButtonsArePills() {
        String css = readCss("admin-style.css");
        String btnRadius = normalize(borderRadiusOf(css, ".btn"));
        String tabRadius = normalize(borderRadiusOf(css, ".tab"));

        assertAll(
                () -> assertTrue("50px".equals(btnRadius),
                        "admin .btn border-radius must be 50px but was: " + borderRadiusOf(css, ".btn")),
                () -> assertTrue("50px".equals(tabRadius),
                        "admin .tab border-radius must be 50px but was: " + borderRadiusOf(css, ".tab"))
        );
    }
}
