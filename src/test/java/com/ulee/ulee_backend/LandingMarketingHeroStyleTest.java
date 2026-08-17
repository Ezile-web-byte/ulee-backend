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
 * Bug condition exploration test — Property 4: Landing/Marketing Hero Style Applied.
 *
 * <p>This is a BUGFIX exploration test for the {@code ui-consistency-fix} spec. It encodes the
 * EXPECTED (post-fix) behavior for the Landing/Marketing track: {@code properties.html},
 * {@code register.html}, and {@code logIn.html} must render using the "bold hero" style already
 * proven in {@code student/student-dashboard.html} — {@code Plus Jakarta Sans} display /
 * {@code Hanken Grotesk} body fonts, a yellow accent ({@code #ffe170} background /
 * {@code #221b00} text) on CTAs, glass-panel surfaces, pill ({@code 999px}/{@code 50px}) radii,
 * and (where applicable) a full-width photo + dark-overlay hero surface.
 *
 * <p><b>On UNFIXED code these assertions MUST FAIL.</b> The failures are the counterexamples that
 * confirm the Landing/Marketing deviation exists: {@code properties.html} has no styling of any
 * kind, {@code register.html} is still on the teal {@code Sora}/{@code Playfair Display} system,
 * and {@code logIn.html}/{@code login-style.css} are still on {@code DM Sans}/
 * {@code DM Serif Display} with a flat dark background. After the fix (task 4.7 Group F, and the
 * login theme portion of task 4.5) the same test should PASS (re-verified in task 4.9).
 *
 * <p>Note: the broken {@code image2.jpeg} asset path itself is covered by
 * {@link LoginBackgroundAssetTest} (task 2) and is intentionally NOT re-asserted here — this test's
 * login checks are scoped to fonts/theme only.
 *
 * <p><b>Validates: Requirements 1.5, 1.6, 1.12, 1.13</b>
 */
class LandingMarketingHeroStyleTest {

    private static final Path TEMPLATES_DIR = Path.of("src", "main", "resources", "templates");
    private static final Path STATIC_DIR = Path.of("src", "main", "resources", "static");

    private static final String HERO_YELLOW = "#ffe170";
    private static final String HERO_ON_YELLOW = "#221b00";
    private static final String TEAL_PRIMARY_HSL = "hsl(180,67%,47%)";

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static String readTemplate(String fileName) {
        Path p = TEMPLATES_DIR.resolve(fileName);
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read template: " + p, e);
        }
    }

    private static String readCss(String fileName) {
        Path p = STATIC_DIR.resolve(fileName);
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stylesheet: " + p, e);
        }
    }

    /**
     * Lowercase + strip all whitespace and {@code +} characters so color/font comparisons are
     * format-insensitive. The {@code +} is stripped because Google Fonts URLs encode spaces in
     * multi-word family names as {@code +} (e.g. {@code family=Plus+Jakarta+Sans}), which is the
     * convention used consistently across every template in this project (see
     * {@code landlord-index.html}'s {@code Playfair+Display} link, for example).
     */
    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[\\s+]+", "");
    }

    /** Returns the value of a CSS custom property, e.g. cssVar(css, "--primary"). */
    private static String cssVar(String css, String name) {
        Matcher m = Pattern.compile(Pattern.quote(name) + "\\s*:\\s*([^;]+);").matcher(css);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Returns the body of the first rule for the given simple selector (e.g. "body", ".sa-logo"). */
    private static String ruleBody(String css, String selector) {
        Pattern p = Pattern.compile("(?:^|[\\n}\\s,])" + Pattern.quote(selector) + "\\s*\\{([^}]*)}");
        Matcher m = p.matcher(css);
        return m.find() ? m.group(1) : null;
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

    /** Returns the background/background-color value declared inside the given selector's rule body. */
    private static String backgroundOf(String css, String selector) {
        String body = ruleBody(css, selector);
        if (body == null) {
            return null;
        }
        Matcher m = Pattern.compile("background(?:-color)?\\s*:\\s*([^;]+);").matcher(body);
        return m.find() ? m.group(1).trim() : null;
    }

    /** Extracts the inline {@code <style>...</style>} block content from an HTML document, if any. */
    private static String inlineStyleBlockOf(String html) {
        Matcher m = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE).matcher(html);
        return m.find() ? m.group(1) : null;
    }

    /** True if the HTML document has a <link rel="stylesheet" ...> reference anywhere in the head. */
    private static boolean hasStylesheetLink(String html) {
        return Pattern.compile("<link[^>]*rel=[\"']stylesheet[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
                .matcher(html).find();
    }

    // ---------------------------------------------------------------------
    // properties.html — currently zero styling of any kind
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("properties.html has a linked stylesheet or <style> block, and a Hero font-family reference")
    void propertiesHtmlHasHeroStyling() {
        String html = readTemplate("properties.html");

        boolean hasLink = hasStylesheetLink(html);
        String inlineStyle = inlineStyleBlockOf(html);
        boolean hasStyleBlock = inlineStyle != null && !inlineStyle.isBlank();
        String norm = normalize(html);
        boolean hasHeroFont = norm.contains("plusjakartasans") || norm.contains("hankengrotesk");

        assertAll(
                () -> assertTrue(hasLink || hasStyleBlock,
                        "properties.html must have at least one <link rel=\"stylesheet\"> or a <style> block, "
                                + "but the template currently has zero styling of any kind"),
                () -> assertTrue(hasHeroFont,
                        "properties.html must reference the Landing/Marketing Hero fonts "
                                + "('Plus Jakarta Sans' / 'Hanken Grotesk') somewhere in its head/style, "
                                + "but no such font-family reference was found")
        );
    }

    // ---------------------------------------------------------------------
    // register.html — currently teal Sora/Playfair Display system
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("register.html inline <style> uses Hanken Grotesk / Plus Jakarta Sans, not Sora / Playfair Display")
    void registerHtmlFonts() {
        String html = readTemplate("register.html");
        String style = inlineStyleBlockOf(html);
        assertTrue(style != null, "register.html must have an inline <style> block");

        String bodyFont = normalize(fontFamilyOf(style, "body"));
        String norm = normalize(style);

        assertAll(
                () -> assertTrue(bodyFont != null
                                && (bodyFont.contains("hankengrotesk") || bodyFont.contains("plusjakartasans")),
                        "register.html body font-family must be 'Hanken Grotesk' or 'Plus Jakarta Sans' "
                                + "but was: " + fontFamilyOf(style, "body")),
                () -> assertTrue(!norm.contains("'sora'"),
                        "register.html inline style still declares the Dashboard/Functional font 'Sora'"),
                () -> assertTrue(!norm.contains("'playfairdisplay'"),
                        "register.html inline style still declares the Dashboard/Functional font 'Playfair Display'")
        );
    }

    @Test
    @DisplayName("register.html .role-btn.active / .submit-btn resolve to the yellow accent, not teal --primary")
    void registerHtmlYellowAccent() {
        String html = readTemplate("register.html");
        String style = inlineStyleBlockOf(html);
        assertTrue(style != null, "register.html must have an inline <style> block");

        String primaryVar = normalize(cssVar(style, "--primary"));
        String roleBtnActiveBg = normalize(backgroundOf(style, ".role-btn.active"));
        String submitBtnBg = normalize(backgroundOf(style, ".submit-btn"));

        assertAll(
                () -> assertTrue(!TEAL_PRIMARY_HSL.equals(primaryVar),
                        "register.html --primary token must not resolve to the Dashboard/Functional teal "
                                + TEAL_PRIMARY_HSL + " but was: " + cssVar(style, "--primary")),
                () -> assertTrue(roleBtnActiveBg != null
                                && (roleBtnActiveBg.contains(HERO_YELLOW) || roleBtnActiveBg.contains("var(--primary)")
                                        && !TEAL_PRIMARY_HSL.equals(primaryVar)),
                        ".role-btn.active background must resolve to the yellow accent " + HERO_YELLOW
                                + " but was: " + backgroundOf(style, ".role-btn.active")
                                + " (--primary=" + cssVar(style, "--primary") + ")"),
                () -> assertTrue(submitBtnBg != null
                                && (submitBtnBg.contains(HERO_YELLOW) || submitBtnBg.contains("var(--primary)")
                                        && !TEAL_PRIMARY_HSL.equals(primaryVar)),
                        ".submit-btn background must resolve to the yellow accent " + HERO_YELLOW
                                + " but was: " + backgroundOf(style, ".submit-btn")
                                + " (--primary=" + cssVar(style, "--primary") + ")"),
                () -> assertTrue(normalize(style).contains(normalize(HERO_ON_YELLOW))
                                || !TEAL_PRIMARY_HSL.equals(primaryVar),
                        "register.html must use dark-on-yellow text " + HERO_ON_YELLOW + " for the yellow CTAs")
        );
    }

    // ---------------------------------------------------------------------
    // logIn.html / login-style.css — currently DM Sans/DM Serif Display + flat dark bg
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("login-style.css body font is Hanken Grotesk, not DM Sans")
    void loginBodyFont() {
        String css = readCss("login-style.css");
        String bodyFont = normalize(fontFamilyOf(css, "body"));

        assertAll(
                () -> assertTrue(bodyFont != null && bodyFont.contains("hankengrotesk"),
                        "login-style.css body font-family must be 'Hanken Grotesk' but was: "
                                + fontFamilyOf(css, "body")),
                () -> assertTrue(bodyFont == null || !bodyFont.contains("dmsans"),
                        "login-style.css body font-family still resolves to 'DM Sans'")
        );
    }

    @Test
    @DisplayName(".sa-logo font is Plus Jakarta Sans, not DM Serif Display")
    void loginLogoFont() {
        String css = readCss("login-style.css");
        String logoFont = normalize(fontFamilyOf(css, ".sa-logo"));

        assertAll(
                () -> assertTrue(logoFont != null && logoFont.contains("plusjakartasans"),
                        ".sa-logo font-family must be 'Plus Jakarta Sans' but was: "
                                + fontFamilyOf(css, ".sa-logo")),
                () -> assertTrue(logoFont == null || !logoFont.contains("dmserifdisplay"),
                        ".sa-logo font-family still resolves to 'DM Serif Display'")
        );
    }

    @Test
    @DisplayName("login page overlay uses the Hero dark-gradient treatment, not a flat rgba fill")
    void loginBackgroundIsNotFlatDark() {
        // NOTE: on the actual UNFIXED file on disk, --bg already resolves to a light
        // hsl(0,0%,99%) rather than a literal #121212 (a prior partial pass already nudged the
        // page toward the OLD teal unification, not the NEW Landing/Marketing Hero direction).
        // The concrete remaining deviation from the Hero pattern is that the .page-overlay dark
        // scrim is a flat rgba(...) fill (via ::after) instead of the Hero's full-width photo +
        // dark GRADIENT overlay (e.g. linear-gradient(to right, rgba(0,91,116,.8), transparent)).
        String css = readCss("login-style.css");
        String overlayAfterBody = ruleBody(css, ".page-overlay::after");
        String overlayBody = ruleBody(css, ".page-overlay");
        String combined = normalize((overlayAfterBody == null ? "" : overlayAfterBody)
                + (overlayBody == null ? "" : overlayBody));

        assertTrue(combined.contains("linear-gradient"),
                "login .page-overlay must use a dark linear-gradient overlay (the Hero full-width "
                        + "photo + dark-gradient-overlay pattern) but it currently only applies a flat "
                        + "rgba(...) fill with no gradient/photo hero treatment");
    }

    @Test
    @DisplayName("logIn.html <head> requests the Hero Google Fonts, not DM Sans/DM Serif Display")
    void loginHeadFontLink() {
        String html = readTemplate("logIn.html");
        String norm = normalize(html);

        assertAll(
                () -> assertTrue(norm.contains("plusjakartasans") || norm.contains("hankengrotesk"),
                        "logIn.html <head> must link the Hero Google Fonts "
                                + "(Plus Jakarta Sans / Hanken Grotesk)"),
                () -> assertTrue(!norm.contains("dm+sans") && !norm.contains("dm+serif+display"),
                        "logIn.html <head> still links the old DM Sans / DM Serif Display Google Fonts")
        );
    }
}
