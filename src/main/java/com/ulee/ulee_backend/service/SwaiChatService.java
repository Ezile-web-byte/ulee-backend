package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.dto.ChatRequestDTO;
import com.ulee.ulee_backend.dto.ChatResponseDTO;
import com.ulee.ulee_backend.dto.NearbyPlaceDTO;
import com.ulee.ulee_backend.dto.PlacesResult;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Orchestrates a single SWAI chat turn: resolves the authoritative
 * Property from the database (never trusting client-supplied property
 * details), decides — via a cheap, deterministic keyword heuristic,
 * never a second LLM call — whether the question needs neighbourhood/
 * place data, fetches that data only when warranted, and produces either
 * a grounded LLM reply or a safe deterministic fallback.
 *
 * This class is the single place where the three failure modes converge
 * to safe, non-hallucinated behavior:
 *   - missing/invalid property coordinates on a neighbourhood question,
 *   - PlacesResult.available() == false (provider failure/unconfigured),
 *   - AiServiceException (LLM provider failure).
 * In every one of those cases, the LLM is either never called, or is
 * called with an explicit instruction that no matching places were found
 * — it is never left free to invent neighbourhood facts.
 */
@Service
public class SwaiChatService {

    // Deterministic v1 intent heuristic — a fixed keyword list, checked
    // once against the lowercased message. No second LLM call is used to
    // classify intent, per the approved architecture: it must be free and
    // instantaneous, and it must never itself risk hallucination.
    private static final Set<String> NEIGHBOURHOOD_KEYWORDS = Set.of(
            "nearby", "near", "nearest", "around", "area",
            "neighbourhood", "neighborhood",
            "mall", "shopping", "supermarket", "grocery",
            "restaurant", "cafe",
            "park", "beach", "relax",
            "gym",
            "church", "worship",
            "club", "nightlife", "entertainment",
            "hospital", "pharmacy",
            "transport", "bus", "taxi",
            "weekend", "things to do",
            "walking", "walk", "car", "commute"
    );

    private static final String NEIGHBOURHOOD_DATA_UNAVAILABLE_FALLBACK =
            "I don't have reliable neighbourhood information for this property right now. " +
                    "You're welcome to ask me about the property itself, like rent, amenities, or room type.";

    private static final String AI_UNAVAILABLE_FALLBACK =
            "I'm having trouble answering right now. Please try again in a moment.";

    private final PropertyRepository propertyRepository;
    private final PlacesService placesService;
    private final AiService aiService;

    @Autowired
    public SwaiChatService(PropertyRepository propertyRepository,
                           PlacesService placesService,
                           AiService aiService) {
        this.propertyRepository = propertyRepository;
        this.placesService = placesService;
        this.aiService = aiService;
    }

    /** Chat turn for a visitor whose name is not known. */
    public ChatResponseDTO handle(ChatRequestDTO request) {
        return handle(request, null);
    }

    /** Chat turn; studentFirstName is the logged-in student's first name, or null. */
    public ChatResponseDTO handle(ChatRequestDTO request, String studentFirstName) {
        String message = request == null ? null : request.getMessage();

        if (message == null || message.isBlank()) {
            return new ChatResponseDTO(
                    "Please type a question and I'll do my best to help."
            );
        }

        // propertyId is only ever used as a lookup key. Every property
        // fact used below comes from the entity fetched here — never from
        // any other field the browser might have sent alongside it.
        Property property = null;

        if (request.getPropertyId() != null) {
            property = propertyRepository
                    .findById(request.getPropertyId())
                    .orElse(null);

            // A stale/unknown id is not an error — it just means this turn
            // proceeds in General_Context, same as no id being supplied.
        }

        boolean isNeighbourhoodQuestion =
                isNeighbourhoodQuestion(message);

        if (isNeighbourhoodQuestion) {
            return handleNeighbourhoodQuestion(message, property, studentFirstName);
        }

        return handleGeneralOrPropertyQuestion(message, property, studentFirstName);
    }

    /** Pure, deterministic intent check — no network/LLM call. */
    boolean isNeighbourhoodQuestion(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        for (String keyword : NEIGHBOURHOOD_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private ChatResponseDTO handleNeighbourhoodQuestion(
            String message,
            Property property,
            String studentFirstName
    ) {
        if (property == null
                || property.getLatitude() == null
                || property.getLongitude() == null) {

            // No property in context, or no usable coordinates — do not
            // call PlacesService at all, and do not let the LLM improvise
            // neighbourhood facts it has no grounding for.
            return new ChatResponseDTO(
                    NEIGHBOURHOOD_DATA_UNAVAILABLE_FALLBACK
            );
        }

        PlacesResult placesResult =
                placesService.findNearbyPlaces(
                        property.getLatitude(),
                        property.getLongitude()
                );

        if (!placesResult.isAvailable()) {
            // Lookup failed/timed out/rate-limited/unconfigured — never
            // send this question to the LLM without factual place data.
            return new ChatResponseDTO(
                    NEIGHBOURHOOD_DATA_UNAVAILABLE_FALLBACK
            );
        }

        // A successful lookup — whether or not it found any places — may
        // go to the LLM, since the prompt built below always states
        // explicitly what was and wasn't found.
        String systemPrompt =
                friendlyIntro(studentFirstName)
                        + buildNeighbourhoodSystemPrompt(
                        property,
                        placesResult.getPlaces()
                );

        return generateReplyOrFallback(systemPrompt, message);
    }

    private ChatResponseDTO handleGeneralOrPropertyQuestion(
            String message,
            Property property,
            String studentFirstName
    ) {
        // Never calls PlacesService here — a non-neighbourhood question
        // has no reason to spend an external Places API call.
        String systemPrompt =
                friendlyIntro(studentFirstName)
                        + buildPropertyOrGeneralSystemPrompt(property);

        return generateReplyOrFallback(systemPrompt, message);
    }

    private ChatResponseDTO generateReplyOrFallback(
            String systemPrompt,
            String userMessage
    ) {
        try {
            String reply =
                    aiService.generateReply(systemPrompt, userMessage);

            return new ChatResponseDTO(reply);

        } catch (AiServiceException e) {
            // Never expose the exception message, stack trace, provider
            // name, or any key to the user — only this generic fallback.
            return new ChatResponseDTO(AI_UNAVAILABLE_FALLBACK);
        }
    }

    /** Tone, plus (when known) the student's first name, placed at the top of every prompt. */
    private String friendlyIntro(String studentFirstName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tone: be warm, upbeat and encouraging, like a friendly older student helping a ");
        sb.append("newcomer. Keep replies short (2-5 sentences), in plain text with no markdown. ");
        sb.append("Ask at most one short follow-up question. ");

        String name = cleanFirstName(studentFirstName);
        if (name != null) {
            sb.append("The student's first name is ").append(name);
            sb.append(". Greet them by name when they greet you, and use it occasionally, ");
            sb.append("but do not repeat it in every sentence.\n\n");
        } else {
            sb.append("You do not know the student's name, so do not guess one.\n\n");
        }
        return sb.toString();
    }

    /** Keeps only letters, spaces, hyphens and apostrophes (max 30 chars) so a name can't carry instructions. */
    private String cleanFirstName(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[^\\p{L} '\\-]", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > 30 ? cleaned.substring(0, 30).trim() : cleaned;
    }

    /**
     * Builds the grounded prompt for a neighbourhood question.
     */
    private String buildNeighbourhoodSystemPrompt(
            Property property,
            List<NearbyPlaceDTO> places
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are ULEE's student accommodation assistant. ");
        prompt.append("Answer only using the facts supplied below about this property and its ");
        prompt.append("nearby places. Do not invent businesses, distances, facilities, transport ");
        prompt.append("options, safety claims, or any other neighbourhood facts that are not ");
        prompt.append("explicitly listed here. If the supplied information is insufficient to fully ");
        prompt.append("answer the question, say so clearly instead of guessing. Answer in concise, ");
        prompt.append("student-friendly language.\n\n");

        appendPropertyFacts(prompt, property);

        prompt.append("\nNearby places lookup result: ");

        if (places.isEmpty()) {
            prompt.append(
                    "The lookup completed successfully and found NO matching nearby places "
                            + "in the supported categories. Tell the student that no matching places "
                            + "were found nearby — do not invent any place, and do not imply the area "
                            + "has no amenities at all, only that none were found in this lookup.\n"
            );
        } else {
            prompt.append(
                    "The following real nearby places were found (do not add, remove, or "
                            + "alter any of these facts):\n"
            );

            for (NearbyPlaceDTO place : places) {
                prompt.append("- ").append(place.getName());

                if (place.getCategory() != null) {
                    prompt.append(" (")
                            .append(place.getCategory())
                            .append(")");
                }

                if (place.getDistanceMeters() != null) {
                    prompt.append(", about ")
                            .append(Math.round(place.getDistanceMeters()))
                            .append("m away");
                }

                if (place.getAddress() != null
                        && !place.getAddress().isBlank()) {
                    prompt.append(", ")
                            .append(place.getAddress());
                }

                prompt.append("\n");
            }
        }

        return prompt.toString();
    }

    /** Builds the prompt for a non-neighbourhood question. */
    private String buildPropertyOrGeneralSystemPrompt(Property property) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are ULEE's student accommodation assistant. ");
        prompt.append("Answer only using the facts supplied below. Do not invent property details, ");
        prompt.append("prices, amenities, or any other facts not explicitly listed here. If the ");
        prompt.append("supplied information is insufficient to fully answer the question, say so ");
        prompt.append("clearly instead of guessing. Answer in concise, student-friendly language.\n\n");

        if (property == null) {
            prompt.append(buildListingsContext());
        } else {
            appendPropertyFacts(prompt, property);
        }

        return prompt.toString();
    }

    /** Appends authoritative database-sourced property facts. */
    private void appendPropertyFacts(
            StringBuilder prompt,
            Property property
    ) {
        prompt.append(
                "Property facts (from the ULEE database):\n"
        );

        prompt.append("- Title: ")
                .append(valueOrUnknown(property.getTitle()))
                .append("\n");

        prompt.append("- Address: ")
                .append(valueOrUnknown(property.getAddress()))
                .append("\n");

        prompt.append("- City: ")
                .append(valueOrUnknown(property.getCity()))
                .append("\n");

        if (property.getRent() != null) {
            prompt.append("- Rent: R")
                    .append(property.getRent())
                    .append(" per month\n");
        }

        if (property.getType() != null) {
            prompt.append("- Type: ")
                    .append(property.getType())
                    .append("\n");
        }

        // Property now uses capacity rather than bedrooms.
        if (property.getCapacity() != null) {
            prompt.append("- Capacity: ")
                    .append(property.getCapacity())
                    .append(" student(s)\n");
        }

        if (property.getCommuteType() != null) {
            prompt.append("- Commute type: ")
                    .append(property.getCommuteType())
                    .append("\n");
        }

        if (property.getDescription() != null
                && !property.getDescription().isBlank()) {
            prompt.append("- Description: ")
                    .append(property.getDescription())
                    .append("\n");
        }
    }

    /** Lists the available ULEE rooms so the AI can recommend real ones when no property is open. */
    private String buildListingsContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("No specific property is currently in view. Below are the ULEE rooms available ")
                .append("right now. When the student asks about rooms, budget, room type or location, ")
                .append("recommend ONLY rooms from this list. Name the room, its area, its rent per ")
                .append("month and its link path exactly as written, for example: Name (/property/12). ")
                .append("Never invent rooms, prices, lease lengths, facilities or availability. ")
                .append("If nothing on the list fits, say so plainly. Use plain text, no markdown.\n");

        List<Property> available = propertyRepository.findByIsAvailableTrue();
        if (available == null || available.isEmpty()) {
            sb.append("(There are no available rooms listed right now.)\n");
            return sb.toString();
        }

        int shown = 0;
        for (Property p : available) {
            if (shown >= 40) {
                break;
            }
            shown++;
            sb.append("- ").append(valueOrUnknown(p.getTitle()))
                    .append(" (/property/").append(p.getPropertyID()).append(")");
            String area = (p.getSuburb() != null && !p.getSuburb().isBlank()) ? p.getSuburb() : p.getCity();
            if (area != null && !area.isBlank()) {
                sb.append(" | ").append(area);
            }
            if (p.getRent() != null) {
                sb.append(" | R").append(p.getRent().stripTrailingZeros().toPlainString()).append("/month");
            }
            if (p.getType() != null && !p.getType().isBlank()) {
                sb.append(" | ").append(p.getType());
            }
            if (p.getCommuteType() != null && !p.getCommuteType().isBlank()) {
                sb.append(" | to campus: ").append(p.getCommuteType());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String valueOrUnknown(String value) {
        return (value == null || value.isBlank())
                ? "not specified"
                : value;
    }
}