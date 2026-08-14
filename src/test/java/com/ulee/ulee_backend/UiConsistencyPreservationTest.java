package com.ulee.ulee_backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Preservation snapshot test — Property 3: Student Pages, Behavior, and Structure Unchanged.
 *
 * <p>This is a BUGFIX preservation test for the {@code ui-consistency-fix} spec. It follows the
 * observation-first methodology: it captures the baseline behavior on the UNFIXED code for all
 * inputs where {@code isBugCondition(element)} is FALSE — student-page visuals, JavaScript-driven
 * behavior, the DOM/Thymeleaf markup contract, controller routes, and the login page's responsive
 * layout. These are the things the UI-consistency fix must NOT change.
 *
 * <p><b>On UNFIXED code these assertions MUST PASS</b> — that is the whole point: they record the
 * baseline to preserve. After the fix (task 4) they are re-run unchanged (task 4.9) and must STILL
 * pass, proving no regression leaked outside the intended CSS / font-{@code <link>} edits.
 *
 * <p>The fix is pure CSS + font-{@code <link>} work, so:
 * <ul>
 *   <li>Student templates, all {@code .js} files, and controllers are never touched — asserted with
 *       byte-for-byte SHA-256 snapshots of the whole file.</li>
 *   <li>Templates whose {@code <head>} font {@code <link>} changes must keep their {@code <body>}
 *       (all {@code th:*} markup, form actions, class/id selectors) identical — asserted with a
 *       SHA-256 snapshot of the {@code <body>} region only.</li>
 * </ul>
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</b>
 */
class UiConsistencyPreservationTest {

    private static final Path STATIC_DIR = Path.of("src", "main", "resources", "static");
    private static final Path TEMPLATES_DIR = Path.of("src", "main", "resources", "templates");
    private static final Path CONTROLLER_DIR =
            Path.of("src", "main", "java", "com", "ulee", "ulee_backend", "controller");

    // ---------------------------------------------------------------------
    // Baseline snapshots captured on the UNFIXED code.
    // These files must remain byte-for-byte identical after the fix.
    // ---------------------------------------------------------------------

    /** Student templates — the design-system reference; the fix must not touch them (Req 3.1). */
    private static final Map<String, String> STUDENT_TEMPLATE_HASHES = Map.of(
            "property-detail.html", "3356825f938427ea363568a26b5ddb701bf9b5a606ea637ca3f0974ac193f0ba",
            "register.html",        "11c3d9d40001158a9d32a0120877a90645eaa282edbc0081a877714d131168bf",
            "my-property-reviews.html", "a2d944c9cf56694f524b5a80466ee65b4aeb7792e63a53b7557916a73701ee07",
            "properties.html",      "f443c19f76b115ef5a5b06ce8860ec1c74c45b2b628de3febfd66a2c7e71a098",
            "my-applications.html", "4e52b47ec9a13c9064416cd890d4f7877581ba64ed4099bb22fe7e19da9bcff6"
    );

    /** JavaScript files — no JS logic is modified, so behavior is preserved (Req 3.2). */
    private static final Map<String, String> JS_HASHES = Map.of(
            "admin-script.js",    "501d206d7d4acefb460f1bd8ff54a6c983b022fc84b616f036383dc5a1fe5a4d",
            "landlord-script.js", "10763217cf4fad03964ad2660b4048f8f3fa59cb4ee7e35b128c1dd62e851b25",
            "listProperty.js",    "49aa90a49ff10061cea266d605f2b4f8aef4590a6c9285a2d1834aeb9e058565",
            "application.js",     "4e4584c75fc31dcd5e05de573625d0726c11296e2f1224ffac97ed4bc29c11e3",
            "update.js",          "a5e6be8f498303ddde68fda3fdd24aa488b7edaed931f93ab22a63bffd2c6c13",
            "login-script.js",    "bcf5b5ae839800fd95fbc2ec7653202c31fd39dbd3b007ed8b910f24ad3f8615"
    );

    /** Controllers — routes and rendered model data must be unchanged (Req 3.6). */
    private static final Map<String, String> CONTROLLER_HASHES = Map.of(
            "AdminController.java",    "c8bfcac4d80a613fc142778b8f57c99c5239d04ed781067742e824808da6fe9c",
            "PageController.java",     "3d2ffc55bfbcccfa5db6d084987bd292d7ba8948959e836a78bd49389c0c3ebd",
            "PanoramaController.java", "74d7a24557f151fa4cff02a5a4efdda8541176ecc8c0c2ef65eed4e5cd36d655",
            "PropertyController.java", "8fc7988fa507d25c67645fd7a8eb0914e0a39a27469a0bcc6190576358fbf7ed",
            "RegisterController.java", "e66948bcd74ef6866888a81b08463a48d0f6ba6f695d3a3a8c9586b3f1e0489a"
    );

    /**
     * Templates whose {@code <head>} font {@code <link>} changes in the fix (task 4.6/4.7). The
     * fix is confined to the {@code <head>}, so the {@code <body>} region — every {@code th:*}
     * attribute, form action, and class/id selector JS + the server depend on — must stay identical
     * (Req 3.3, 3.4, 3.6). Keyed by the template's path relative to the templates dir.
     */
    private static final Map<String, String> TEMPLATE_BODY_HASHES = Map.of(
            "landlord/landlord-index.html",       "89471b5db943c4b7d1012ffed855f41bf2479623071e992d5600457d50db85d8",
            "landlord/listProperty.html",         "d2b7562e4c59890ee0b04d443604f498f75e6b703b98e3297adc6a2266469740",
            "landlord/manage-applications.html",  "f615eb419b202a76b46aafe4608a1a7e01fab2e7f0673f9c4e3adac4c17b87da",
            "landlord/my-property-reviews.html",  "0ef50f2aa592a7e668b0288b87335412ae89afeb0a7a74778735083f183d1b6b",
            "application.html",                   "f7f2c8fecebc630e5764c02ab9052118966f8a8f848a4c333ed7e7b4aaa0c21d",
            "logIn.html",                         "bdea3b98094e5c7328a5f2575ba8a0dd0ff217082e9a86f2e7d14e072cab4c66",
            "manage-properties.html",             "9b4e4814d9cf4399a2f3ad8afd470f63122742385c025955aab431c9cb5ba888",
            "update.html",                        "724834afa3937d5534963140c152029c2d45956cd27ef83c9c06f81e2e853af8"
    );

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static byte[] readBytes(Path p) {
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read file: " + p, e);
        }
    }

    private static String readText(Path p) {
        return new String(readBytes(p), StandardCharsets.UTF_8);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Extracts the {@code <body>...</body>} region of an HTML document. The fix only edits the
     * {@code <head>}, so hashing this region isolates the DOM/markup contract from the intended
     * font-{@code <link>} change. Extraction is done on the decoded string (matching the recorded
     * baseline), so any UTF-8 BOM in the {@code <head>} does not affect the result.
     */
    private static String bodyRegion(String html) {
        int start = html.indexOf("<body");
        int endMarker = html.indexOf("</body>");
        assertTrue(start >= 0, "document has no <body> tag");
        assertTrue(endMarker >= 0, "document has no </body> tag");
        int end = endMarker + "</body>".length();
        return html.substring(start, end);
    }

    private static String cssVar(String css, String name) {
        Matcher m = Pattern.compile(Pattern.quote(name) + "\\s*:\\s*([^;]+);").matcher(css);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("\\s+", "");
    }

    // ---------------------------------------------------------------------
    // Req 3.1 — Student page visuals unchanged
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Student templates are byte-for-byte unchanged (visuals preserved)")
    void studentTemplatesUnchanged() {
        STUDENT_TEMPLATE_HASHES.forEach((name, expected) -> {
            Path p = TEMPLATES_DIR.resolve(name);
            String actual = sha256Hex(readBytes(p));
            assertEquals(expected, actual,
                    "Student template '" + name + "' changed — the fix must not touch student pages "
                            + "(Req 3.1). Expected SHA-256 " + expected + " but was " + actual);
        });
    }

    @Test
    @DisplayName("Student inline design tokens still resolve to the canonical teal system")
    void studentInlineTokensBaseline() {
        // Snapshot of the computed-style intent: teal primary, 12px card radius, Sora/Playfair.
        String detail = readText(TEMPLATES_DIR.resolve("property-detail.html"));
        String register = readText(TEMPLATES_DIR.resolve("register.html"));
        String reviews = readText(TEMPLATES_DIR.resolve("my-property-reviews.html"));

        assertAll(
                () -> assertEquals("hsl(180,67%,47%)", normalize(cssVar(detail, "--primary")),
                        "property-detail --primary baseline changed"),
                () -> assertEquals("hsl(180,67%,47%)", normalize(cssVar(register, "--primary")),
                        "register --primary baseline changed"),
                () -> assertEquals("hsl(180,67%,47%)", normalize(cssVar(reviews, "--primary")),
                        "my-property-reviews --primary baseline changed"),
                () -> assertEquals("12px", normalize(cssVar(detail, "--radius")),
                        "property-detail --radius baseline changed"),
                () -> assertEquals("12px", normalize(cssVar(reviews, "--radius")),
                        "my-property-reviews --radius baseline changed"),
                () -> assertTrue(normalize(detail).contains("'sora'") && normalize(detail).contains("playfairdisplay"),
                        "property-detail must keep Sora/Playfair Display fonts"),
                () -> assertTrue(normalize(register).contains("'sora'"),
                        "register must keep the Sora body font")
        );
    }

    // ---------------------------------------------------------------------
    // Req 3.2 — JavaScript behavior unchanged
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("JavaScript files are byte-for-byte unchanged (behavior preserved)")
    void javascriptUnchanged() {
        JS_HASHES.forEach((name, expected) -> {
            Path p = STATIC_DIR.resolve(name);
            String actual = sha256Hex(readBytes(p));
            assertEquals(expected, actual,
                    "JavaScript file '" + name + "' changed — no JS logic may be modified by the fix "
                            + "(Req 3.2). Expected SHA-256 " + expected + " but was " + actual);
        });
    }

    @Test
    @DisplayName("Key JS-driven interactions are present in the baseline (documented behavior)")
    void javascriptBehaviorMarkers() {
        String login = readText(STATIC_DIR.resolve("login-script.js"));
        String admin = readText(STATIC_DIR.resolve("admin-script.js"));
        String update = readText(STATIC_DIR.resolve("update.js"));
        String landlord = readText(STATIC_DIR.resolve("landlord-script.js"));
        String wizard = readText(STATIC_DIR.resolve("listProperty.js"));
        String application = readText(STATIC_DIR.resolve("application.js"));

        assertAll(
                () -> assertTrue(login.contains("function switchPanel") && login.contains("function togglePassword"),
                        "login panel-switch + password-toggle behavior must be present"),
                () -> assertTrue(admin.contains("classList.toggle('collapsed')"),
                        "admin sidebar collapse behavior must be present"),
                () -> assertTrue(update.contains("function toggleSidebar") && update.contains("function saveChanges"),
                        "update sidebar/save behavior must be present"),
                () -> assertTrue(landlord.contains("function applyFilterAndSort"),
                        "landlord filter/sort behavior must be present"),
                () -> assertTrue(wizard.contains("function goStep") && wizard.contains("function publishListing"),
                        "list-property wizard steps + publish behavior must be present"),
                () -> assertTrue(application.contains("function updateStatus") && application.contains("function openModal"),
                        "applications accept/reject + modal behavior must be present")
        );
    }

    // ---------------------------------------------------------------------
    // Req 3.3, 3.4, 3.6 — DOM / Thymeleaf markup contract unchanged
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Changed templates keep their <body> markup byte-for-byte (DOM contract preserved)")
    void changedTemplateBodiesUnchanged() {
        TEMPLATE_BODY_HASHES.forEach((relPath, expected) -> {
            Path p = TEMPLATES_DIR.resolve(relPath);
            String body = bodyRegion(readText(p));
            String actual = sha256Hex(body.getBytes(StandardCharsets.UTF_8));
            assertEquals(expected, actual,
                    "Template '" + relPath + "' <body> changed — only the <head> font <link> may be "
                            + "edited; th:* markup, form actions, and class/id selectors must be preserved "
                            + "(Req 3.3, 3.4, 3.6). Expected SHA-256 " + expected + " but was " + actual);
        });
    }

    // ---------------------------------------------------------------------
    // Req 3.6 — Controller routes / rendered data unchanged
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Controllers are byte-for-byte unchanged (routes and model data preserved)")
    void controllersUnchanged() {
        CONTROLLER_HASHES.forEach((name, expected) -> {
            Path p = CONTROLLER_DIR.resolve(name);
            String actual = sha256Hex(readBytes(p));
            assertEquals(expected, actual,
                    "Controller '" + name + "' changed — no backend route or model data may change "
                            + "(Req 3.6). Expected SHA-256 " + expected + " but was " + actual);
        });
    }

    // ---------------------------------------------------------------------
    // Req 3.5 — Login page centered, responsive layout unchanged
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Login page keeps its centered, responsive flex layout")
    void loginResponsiveLayoutBaseline() {
        String css = readText(STATIC_DIR.resolve("login-style.css"));
        String norm = normalize(css);
        assertNotNull(css);
        assertAll(
                () -> assertTrue(norm.contains("min-height:100vh"),
                        "login body must span the full viewport height (centered layout)"),
                () -> assertTrue(norm.contains("align-items:center"),
                        "login layout must vertically center the card"),
                () -> assertTrue(norm.contains("justify-content:center"),
                        "login layout must horizontally center the card")
        );
    }
}
