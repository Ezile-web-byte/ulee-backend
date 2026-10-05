package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.dto.ListingSummaryDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * POST /api/chat -> Google Gemini API (free tier).
 *
 * The API key lives ONLY on the server (env var GEMINI_API_KEY).
 * Live listings come from SwaiApiController (same data as GET /api/listings).
 *
 * application.properties:
 *   gemini.api-key=${GEMINI_API_KEY:}
 *   gemini.model=gemini-2.5-flash
 */
@RestController
public class SwaiChatController {

    @Autowired
    private SwaiApiController listingsApi;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.8-flash}")
    private String model;

    // How hard the model "thinks" before answering. Lower = faster replies.
    // Try: minimal / low / medium / high. Leave empty to use Google's default.
    @Value("${gemini.thinking-level:low}")
    private String thinkingLevel;

    // Optional backup model, used on the last retry if the main model is overloaded.
    // e.g. gemini.fallback-model=<a "flash-lite" model code from AI Studio>
    @Value("${gemini.fallback-model:}")
    private String fallbackModel;

    // Google Search grounding (live shops/clubs/events). May not be in the free
    // tier; set gemini.search=true in application.properties to enable.
    @Value("${gemini.search:false}")
    private boolean searchEnabled;

    // ---- Usage limits: protect the free Gemini quota -------------------------
    // All of these can be changed in application.properties (see the bottom of this comment).
    //   swai.limit.per-minute   questions per visitor (IP) per minute
    //   swai.limit.per-day      questions per visitor (IP) per day
    //   swai.limit.daily-tokens total Gemini tokens the whole site may use per day (0 = no cap)
    //   swai.history-turns      how many earlier chat messages are sent along with each question
    //   swai.max-message-chars  longest question accepted
    //   swai.max-output-tokens  longest answer (includes the model's "thinking")
    @Value("${swai.limit.per-minute:6}")
    private int perMinute;

    @Value("${swai.limit.per-day:20}")
    private int perDay;

    @Value("${swai.limit.daily-tokens:0}")
    private long dailyTokenBudget;

    // Site-wide request caps, shared by ALL visitors. The free Gemini tier limits requests
    // (e.g. 15 per minute / 500 per day for a Flash-Lite model), so set these just below
    // your model's limits. 0 = no cap.
    @Value("${swai.limit.global-per-minute:0}")
    private int globalPerMinute;

    @Value("${swai.limit.daily-requests:0}")
    private int dailyRequests;

    @Value("${swai.history-turns:4}")
    private int historyTurns;

    @Value("${swai.max-message-chars:600}")
    private int maxMessageChars;

    @Value("${swai.max-output-tokens:1024}")
    private int maxOutputTokens;

    // Curated local guide (src/main/resources/swai-local-guide.txt): the "hard-coded" background
    // knowledge that is sent to the AI alongside live listings and live API answers.
    @Value("classpath:swai-local-guide.txt")
    private org.springframework.core.io.Resource guideResource;

    private String guideCache;

    private synchronized String loadGuide() {
        if (guideCache == null) {
            try {
                guideCache = new String(guideResource.getInputStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.err.println("Could not load swai-local-guide.txt: " + e.getMessage());
                guideCache = "";
            }
        }
        return guideCache;
    }

    private final RestClient gemini = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    // ---- rate limits ------------------------------------------------------

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /** Per-visitor limit: at most `perMinute` questions in any 60 seconds. */
    private boolean allowed(String ip) {
        if (hits.size() > 5000) hits.clear();   // keep memory bounded
        Deque<Instant> q = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (q) {
            Instant cutoff = Instant.now().minusSeconds(60);
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) {
                q.pollFirst();
            }
            if (q.size() >= perMinute) {
                return false;
            }
            q.addLast(Instant.now());
            return true;
        }
    }

    private static class DayCount {
        LocalDate day = LocalDate.now();
        int count;
    }

    private final Map<String, DayCount> dailyByIp = new ConcurrentHashMap<>();

    /** Per-visitor limit: at most `perDay` questions per calendar day. */
    private boolean withinDailyLimit(String ip) {
        if (dailyByIp.size() > 5000) dailyByIp.clear();
        DayCount d = dailyByIp.computeIfAbsent(ip, k -> new DayCount());
        synchronized (d) {
            LocalDate today = LocalDate.now();
            if (!today.equals(d.day)) {
                d.day = today;
                d.count = 0;
            }
            if (d.count >= perDay) {
                return false;
            }
            d.count++;
            return true;
        }
    }

    // Site-wide request counters (all visitors together).
    private final Object globalLock = new Object();
    private final Deque<Instant> globalHits = new ArrayDeque<>();
    private LocalDate requestsDay = LocalDate.now();
    private int requestsToday = 0;

    /** Returns null if the request may go ahead (and counts it), otherwise the message to show. */
    private String globalLimitMessage() {
        synchronized (globalLock) {
            LocalDate today = LocalDate.now();
            if (!today.equals(requestsDay)) {
                requestsDay = today;
                requestsToday = 0;
            }
            if (dailyRequests > 0 && requestsToday >= dailyRequests) {
                return "The assistant has reached its daily limit. You can still use the buttons above to browse rooms, "
                        + "or try again tomorrow.";
            }
            Instant cutoff = Instant.now().minusSeconds(60);
            while (!globalHits.isEmpty() && globalHits.peekFirst().isBefore(cutoff)) {
                globalHits.pollFirst();
            }
            if (globalPerMinute > 0 && globalHits.size() >= globalPerMinute) {
                return "The assistant is very busy right now. Please try again in a minute.";
            }
            globalHits.addLast(Instant.now());
            requestsToday++;
            return null;
        }
    }

    // Site-wide token budget for the day, counted from Gemini's own usage report.
    private final Object tokenLock = new Object();
    private LocalDate tokensDay = LocalDate.now();
    private long tokensToday = 0;

    private boolean tokenBudgetLeft() {
        synchronized (tokenLock) {
            LocalDate today = LocalDate.now();
            if (!today.equals(tokensDay)) {
                tokensDay = today;
                tokensToday = 0;
            }
            return dailyTokenBudget <= 0 || tokensToday < dailyTokenBudget;
        }
    }

    private void recordTokens(Map<String, Object> res) {
        if (res == null) return;
        Object usage = res.get("usageMetadata");
        if (!(usage instanceof Map<?, ?> u)) return;
        Object total = u.get("totalTokenCount");
        if (!(total instanceof Number n)) return;
        synchronized (tokenLock) {
            LocalDate today = LocalDate.now();
            if (!today.equals(tokensDay)) {
                tokensDay = today;
                tokensToday = 0;
            }
            tokensToday += n.longValue();
            System.err.println("Gemini tokens: this request " + n.longValue() + ", today " + tokensToday
                    + (dailyTokenBudget > 0 ? " / " + dailyTokenBudget : ""));
        }
    }

    // ---- request / response shapes ---------------------------------------

    public record ChatMessage(String role, String content) {}

    public record ChatRequest(String message,
                              List<ChatMessage> history,
                              String propertyTitle,
                              String propertyAddress,
                              String propertyCommuteType,
                              String propertyFeatures) {}

    public record ChatResponse(String reply) {}

    // ---- endpoint ---------------------------------------------------------

    @PostMapping("/api/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req, HttpServletRequest servletReq) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(503).body(new ChatResponse(
                    "The assistant isn't set up yet (missing API key)."));
        }
        if (req.message() == null || req.message().isBlank() || req.message().length() > maxMessageChars) {
            return ResponseEntity.badRequest().body(new ChatResponse(
                    "Please keep your question a bit shorter."));
        }
        if (!tokenBudgetLeft()) {
            return ResponseEntity.status(429).body(new ChatResponse(
                    "The assistant has reached its daily limit. You can still use the buttons above to browse rooms, "
                            + "or try again tomorrow."));
        }
        String ip = servletReq.getRemoteAddr();
        if (!allowed(ip)) {
            return ResponseEntity.status(429).body(new ChatResponse(
                    "You're sending messages too fast. Give it a minute and try again."));
        }
        if (!withinDailyLimit(ip)) {
            return ResponseEntity.status(429).body(new ChatResponse(
                    "You've reached today's limit for AI questions. You can still use the buttons to browse rooms, "
                            + "or come back tomorrow."));
        }
        String busy = globalLimitMessage();
        if (busy != null) {
            return ResponseEntity.status(429).body(new ChatResponse(busy));
        }

        // Build turns: the last few history messages + the new message. Gemini roles: "user" / "model".
        List<String[]> turns = new ArrayList<>(); // {role, text}
        if (req.history() != null) {
            List<ChatMessage> h = req.history();
            for (ChatMessage m : h.subList(Math.max(0, h.size() - historyTurns), h.size())) {
                boolean validRole = "user".equals(m.role()) || "assistant".equals(m.role());
                if (validRole && m.content() != null && !m.content().isBlank()) {
                    turns.add(new String[]{"user".equals(m.role()) ? "user" : "model", truncate(m.content(), 400)});
                }
            }
        }
        turns.add(new String[]{"user", req.message()});

        // Must start with a user turn; merge consecutive same-role turns.
        while (!turns.isEmpty() && !"user".equals(turns.get(0)[0])) {
            turns.remove(0);
        }
        List<Map<String, Object>> contents = new ArrayList<>();
        String lastRole = null;
        StringBuilder buf = new StringBuilder();
        for (String[] t : turns) {
            if (lastRole != null && !lastRole.equals(t[0])) {
                contents.add(content(lastRole, buf.toString()));
                buf.setLength(0);
            }
            if (buf.length() > 0) buf.append("\n");
            buf.append(t[1]);
            lastRole = t[0];
        }
        if (lastRole != null) contents.add(content(lastRole, buf.toString()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", buildSystemPrompt(req)))));
        body.put("contents", contents);
        // Google Search grounding: current shops, clubs, events, opening hours.
        if (searchEnabled) {
            body.put("tools", List.of(Map.of("google_search", Map.of())));
        }
        Map<String, Object> genConfig = new LinkedHashMap<>();
        genConfig.put("maxOutputTokens", maxOutputTokens);
        genConfig.put("temperature", 0.6);
        if (thinkingLevel != null && !thinkingLevel.isBlank()) {
            genConfig.put("thinkingConfig", Map.of("thinkingLevel", thinkingLevel));
        }
        body.put("generationConfig", genConfig);

        try {
            Map<String, Object> res = callGeminiWithRetry(body);
            String reply = extractText(res);
            if (reply.isEmpty()) {
                System.err.println("Gemini returned no text (will retry once): " + describeEmpty(res));
                res = callGeminiWithRetry(body);
                reply = extractText(res);
                if (reply.isEmpty()) {
                    System.err.println("Gemini returned no text again: " + describeEmpty(res));
                }
            }

            if (reply.isEmpty()) {
                reply = "Sorry, I couldn't come up with an answer. Could you rephrase that?";
            }
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            System.err.println("Gemini quota exceeded (429): " + e.getResponseBodyAsString());
            return ResponseEntity.status(429).body(new ChatResponse(
                    "The assistant is busy right now. Please try again in a minute."));
        } catch (Exception e) {
            System.err.println("Gemini call failed: " + e.getMessage());
            return ResponseEntity.status(502).body(new ChatResponse(
                    "Sorry, I'm having trouble right now. Please try again in a moment."));
        }
    }

    // ---- helpers ----------------------------------------------------------

    private boolean hasFallback() {
        return fallbackModel != null && !fallbackModel.isBlank() && !fallbackModel.equals(model);
    }

    /**
     * Calls Gemini. If the main model is out of quota (429) or overloaded (500/503) it switches
     * to the backup model; overloads are retried up to 3 attempts in total.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callGeminiWithRetry(Map<String, Object> body) throws InterruptedException {
        int attempts = 3;
        String useModel = model;
        boolean switched = false;

        for (int i = 1; ; i++) {
            try {
                Map<String, Object> result = gemini.post()
                        .uri("/v1beta/models/" + useModel + ":generateContent")
                        .header("x-goog-api-key", apiKey)
                        .header("content-type", "application/json")
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                recordTokens(result);
                return result;
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                // This model's per-minute or per-day quota is used up.
                if (!switched && hasFallback()) {
                    System.err.println("Gemini: " + useModel + " is out of quota, switching to " + fallbackModel);
                    useModel = fallbackModel;
                    switched = true;
                    continue;
                }
                throw e;
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                if (i >= attempts) throw e;
                System.err.println("Gemini overloaded (" + e.getStatusCode() + "), retry " + i);
                if (!switched && hasFallback()) {
                    useModel = fallbackModel;
                    switched = true;
                }
                Thread.sleep(600L * i);
            }
        }
    }

    private static String extractText(Map<String, Object> res) {
        StringBuilder out = new StringBuilder();
        if (res == null) return "";
        List<?> candidates = (List<?>) res.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<?, ?> cand = (Map<?, ?>) candidates.get(0);
            Map<?, ?> c = (Map<?, ?>) cand.get("content");
            if (c != null && c.get("parts") != null) {
                for (Object p : (List<?>) c.get("parts")) {
                    Object text = ((Map<?, ?>) p).get("text");
                    if (text != null) out.append(text);
                }
            }
        }
        return out.toString().trim();
    }

    /** Short description of why Gemini returned nothing (finishReason / blocked prompt). */
    private static String describeEmpty(Map<String, Object> res) {
        if (res == null) return "null response";
        Object feedback = res.get("promptFeedback");
        List<?> candidates = (List<?>) res.get("candidates");
        Object finish = (candidates != null && !candidates.isEmpty())
                ? ((Map<?, ?>) candidates.get(0)).get("finishReason") : "no candidates";
        return "finishReason=" + finish + ", promptFeedback=" + feedback;
    }

    private static Map<String, Object> content(String role, String text) {
        return Map.of("role", role, "parts", List.of(Map.of("text", text)));
    }

    private String buildSystemPrompt(ChatRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are ULEE, the friendly AI assistant on the ULEE student accommodation website. \
                You help Nelson Mandela University (NMU) students in Gqeberha (Port Elizabeth), \
                South Africa, make their search easier.

                You help with:
                - Finding and comparing accommodation: rent, room types, and getting to campus. \
                  Recommend ULEE listings from the list below first. ULEE room types are \
                  Single, Sharing and Commune.
                - Getting to the NMU campuses (South, North, Ocean Sciences, Second Avenue, Bird Street, \
                  Missionvale) from anywhere in Gqeberha: walking, taxi or bus, ride-hailing, driving.
                - Student life across all of Gqeberha, not only Summerstrand: malls and shops, beaches, \
                  restaurants, gyms, clubs and societies, running groups, things to do, and safety.

                Rules:
                - Start with a direct answer, then add one helpful detail or follow-up suggestion.
                - Walking or travel times: give a rough estimate (for example "about 10-15 minutes on foot") \
                  only when you are reasonably sure, say it is an estimate, and suggest checking Google Maps \
                  walking directions for the exact route. Never present an estimate as exact.
                - ONLY recommend ULEE accommodation from the listings provided below. Never invent ULEE \
                  listings, prices or availability.
                - If asked for accommodation elsewhere on the web and you have not verified it with search, \
                  say you can only see ULEE listings right now, then suggest which areas suit the student's \
                  budget and campus.
                - If you are not sure about opening hours, events or prices, say so instead of guessing.
                - You cannot make bookings or take payments. Tell the student to open the listing and use \
                  "View & Apply", or contact the ULEE team.
                - Keep answers short (2-5 sentences), friendly, plain text, no markdown formatting.
                - Amounts are in South African rand (R).
                """);

        String guide = loadGuide();
        if (!guide.isBlank()) {
            sb.append("\n").append(guide).append("\n");
        }

        if (notBlank(req.propertyTitle())) {
            sb.append("\nThe student is viewing this property right now:\n");
            sb.append("Name: ").append(req.propertyTitle()).append("\n");
            if (notBlank(req.propertyAddress())) {
                sb.append("Address: ").append(req.propertyAddress()).append("\n");
            }
            if (notBlank(req.propertyCommuteType())) {
                sb.append("Getting to campus: ").append(req.propertyCommuteType()).append("\n");
            }
            if (notBlank(req.propertyFeatures())) {
                sb.append("Features: ").append(req.propertyFeatures()).append("\n");
            }
            sb.append("Prefer answering about this property and the area around its address.\n");
        }

        sb.append("\nCurrent available ULEE listings (the only ones you may mention). "
                + "Some lines show only the basics. If the student wants a detail that is not shown "
                + "(features, deposit, bedrooms, distance, availability, rating), say you can check it and ask "
                + "which listing they mean. Never guess a detail that is not listed.\n");
        String block = listingsText(req);
        if (block == null) {
            sb.append("(listings unavailable right now; do not mention specific listings)\n");
        } else {
            sb.append(block);
            sb.append("When recommending a listing, write its name followed by its link path exactly as written, "
                    + "for example: North End Residence 1 (/property/12). The chat turns each path into a listing card "
                    + "with a View & Apply button, so do not describe or mention the button yourself.\n");
        }
        return sb.toString();
    }

    // ---- Listings for the AI prompt ----------------------------------------
    // The listing list is cached for 60 seconds (reading it touches the database several
    // times per listing). To save tokens, most listings are sent as ONE short line; the full
    // details are only sent when they are needed:
    //   FULL     -> the listing the student is viewing, or a listing named in the conversation
    //   DETAILED -> every listing, when the question is about a detail (wifi, deposit, gym...)
    //   COMPACT  -> everything else (name, area, rent, room type, getting to campus)

    private enum Level { COMPACT, DETAILED, FULL }

    private static final Pattern DETAIL_WORDS = Pattern.compile(
            "wifi|wi-fi|gym|furnish|deposit|bedroom|bathroom|kitchen|laundry|parking|security|study|"
                    + "amenit|feature|pool|braai|rating|review|available|capacity|ensuite|en-suite|distance",
            Pattern.CASE_INSENSITIVE);

    private List<ListingSummaryDTO> listingsCache;
    private long listingsCacheAt;

    private synchronized List<ListingSummaryDTO> cachedListings() {
        long now = System.currentTimeMillis();
        if (listingsCache != null && now - listingsCacheAt < 60_000) {
            return listingsCache;
        }
        try {
            listingsCache = listingsApi.listings();
            listingsCacheAt = now;
        } catch (Exception e) {
            System.err.println("Could not load listings for the AI prompt: " + e.getMessage());
        }
        return listingsCache;   // may be slightly old, or null if we never managed to load them
    }

    private String listingsText(ChatRequest req) {
        List<ListingSummaryDTO> listings = cachedListings();
        if (listings == null) return null;
        if (listings.isEmpty()) return "(none available right now)\n";

        String recent = (req.message() == null ? "" : req.message()).toLowerCase();
        if (req.history() != null) {
            List<ChatMessage> h = req.history();
            for (ChatMessage m : h.subList(Math.max(0, h.size() - historyTurns), h.size())) {
                if (m.content() != null) recent += " " + m.content().toLowerCase();
            }
        }
        boolean detailQuestion = req.message() != null && DETAIL_WORDS.matcher(req.message()).find();

        StringBuilder sb = new StringBuilder();
        for (ListingSummaryDTO l : listings) {
            String title = l.getTitle() == null ? "" : l.getTitle().toLowerCase();
            boolean viewing = notBlank(req.propertyTitle()) && title.equals(req.propertyTitle().trim().toLowerCase());
            boolean named = !title.isEmpty() && recent.contains(title);

            Level level = (viewing || named) ? Level.FULL : detailQuestion ? Level.DETAILED : Level.COMPACT;
            sb.append("- ").append(describeListing(l, level)).append("\n");
        }
        return sb.toString();
    }

    private static String describeListing(ListingSummaryDTO l, Level level) {
        List<String> parts = new ArrayList<>();
        parts.add(l.getTitle() + " (/property/" + l.getId() + ")");

        if (level == Level.COMPACT) {
            String where = notBlank(l.getSuburb())
                    ? joinNonBlank(", ", l.getSuburb(), l.getCity())
                    : joinNonBlank(", ", l.getAddress(), l.getCity());
            if (!where.isEmpty()) parts.add(where);
            if (l.getRent() != null) parts.add("R" + l.getRent().stripTrailingZeros().toPlainString() + "/mo");
            if (notBlank(l.getType())) parts.add(l.getType());
            if (notBlank(l.getCommuteType())) parts.add("to campus: " + l.getCommuteType());
            return String.join(" | ", parts);
        }

        String where = joinNonBlank(", ", l.getAddress(), l.getSuburb(), l.getCity());
        if (!where.isEmpty()) parts.add("location: " + where);

        if (l.getRent() != null) {
            parts.add("rent: R" + l.getRent().stripTrailingZeros().toPlainString() + "/month");
        }
        if (l.getDeposit() != null) {
            parts.add("deposit: R" + l.getDeposit().stripTrailingZeros().toPlainString());
        }
        if (notBlank(l.getType())) parts.add("room type: " + l.getType());
        if (l.getBedrooms() != null) parts.add("bedrooms: " + l.getBedrooms());
        if (l.getBathrooms() != null) parts.add("bathrooms: " + l.getBathrooms());
        if (l.getCapacity() != null) parts.add("holds " + l.getCapacity() + " student(s)");
        if (l.getFurnished() != null) parts.add("furnished: " + (l.getFurnished() ? "yes" : "no"));
        if (l.getStudyFriendly() != null) parts.add("study friendly: " + (l.getStudyFriendly() ? "yes" : "no"));
        if (notBlank(l.getCommuteType())) parts.add("getting to campus: " + l.getCommuteType());
        if (l.getDistanceFromUniversity() != null) {
            parts.add("distance from university: " + l.getDistanceFromUniversity().stripTrailingZeros().toPlainString() + " km");
        }
        if (l.getRating() != null) {
            parts.add("rating: " + l.getRating().stripTrailingZeros().toPlainString()
                    + (l.getReviewCount() != null ? " (" + l.getReviewCount() + " reviews)" : ""));
        }
        if (notBlank(l.getAvailableFrom())) parts.add("available from: " + l.getAvailableFrom());
        if (notBlank(l.getFeatures())) parts.add("features: " + l.getFeatures());
        if (level == Level.FULL && notBlank(l.getDescription())) parts.add("about: " + l.getDescription());
        return String.join(" | ", parts);
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String joinNonBlank(String sep, String... items) {
        List<String> out = new ArrayList<>();
        for (String i : items) {
            if (notBlank(i) && !out.contains(i.trim())) out.add(i.trim());
        }
        return String.join(sep, out);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}