package com.ulee.ulee_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug condition exploration test — Property 2: Login Background Image Reference Resolves.
 *
 * <p>This is a BUGFIX exploration test for the {@code ui-consistency-fix} spec. It encodes the
 * EXPECTED (post-fix) behavior: the login {@code .page-overlay} rule references a background image
 * asset that actually exists at the served static path, so the blurred backdrop renders.
 *
 * <p><b>On UNFIXED code this assertion MUST FAIL.</b> {@code login-style.css} references
 * {@code url("image2.jpeg")}, but no such file exists under the static directory — the real asset
 * is {@code login-image2.jpeg}. The failure is the counterexample confirming the broken reference:
 * {@code .page-overlay} requests {@code image2.jpeg} which does not exist, so the blurred backdrop
 * never renders. After the fix (task 4.5) the reference becomes {@code url("login-image2.jpeg")}
 * and this test PASSES.
 *
 * <p><b>Validates: Requirements 1.7</b>
 */
class LoginBackgroundAssetTest {

    private static final Path STATIC_DIR = Path.of("src", "main", "resources", "static");
    private static final String LOGIN_CSS = "login-style.css";

    private static String readCss(String fileName) {
        Path p = STATIC_DIR.resolve(fileName);
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stylesheet: " + p, e);
        }
    }

    /** Returns the body of the first rule for the given simple selector (e.g. ".page-overlay"). */
    private static String ruleBody(String css, String selector) {
        Pattern p = Pattern.compile("(?:^|[\\n}\\s,])" + Pattern.quote(selector) + "\\s*\\{([^}]*)}");
        Matcher m = p.matcher(css);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Extracts the first {@code url(...)} asset reference from a CSS rule body, stripping quotes.
     * e.g. {@code background: url("image2.jpeg") center...} -> {@code image2.jpeg}.
     */
    private static String urlAssetOf(String ruleBody) {
        if (ruleBody == null) {
            return null;
        }
        Matcher m = Pattern.compile("url\\(\\s*['\"]?([^'\")]+)['\"]?\\s*\\)").matcher(ruleBody);
        return m.find() ? m.group(1).trim() : null;
    }

    @Test
    @DisplayName(".page-overlay background image reference resolves to an existing static asset")
    void pageOverlayBackgroundAssetExists() {
        String css = readCss(LOGIN_CSS);

        String overlayRule = ruleBody(css, ".page-overlay");
        assertNotNull(overlayRule, "login-style.css must declare a .page-overlay rule");

        String asset = urlAssetOf(overlayRule);
        assertNotNull(asset, ".page-overlay must reference a background image via url(...)");

        // CSS url() is relative to the stylesheet, which is served from the static root.
        Path resolved = STATIC_DIR.resolve(asset).normalize();

        assertTrue(Files.exists(resolved),
                ".page-overlay references '" + asset + "' which does not exist at the served static path ("
                        + resolved + "); the blurred backdrop never renders. "
                        + "The real asset is 'login-image2.jpeg'.");
    }
}
