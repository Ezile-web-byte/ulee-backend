package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.config.GroqProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * AiService implementation backed by Groq's OpenAI-compatible Chat
 * Completions API (POST {base-url}/openai/v1/chat/completions). All
 * Groq-specific request building and response parsing lives in this class
 * only — every other part of the app sees just plain reply text via the
 * AiService interface, so swapping LLM providers later only means writing
 * a new AiService implementation.
 *
 * Uses Jackson 3 (tools.jackson.*) — this project's actual JSON stack
 * under Spring Boot 4.1 (see GeoapifyPlacesService for the same
 * convention; Jackson 2's com.fasterxml.jackson.databind package is not
 * what jackson-databind resolves to here).
 *
 * Reliability contract (see AiService Javadoc): unlike PlacesService, this
 * class does NOT swallow failures — an unconfigured/blank API key, an HTTP
 * error, a timeout, a malformed response, or a response with no usable
 * reply content all become an AiServiceException, thrown out of
 * generateReply. The caller (SwaiChatService, added in a later stage)
 * decides the user-facing fallback.
 */
@Service
public class GroqAiService implements AiService {

    private static final String CHAT_COMPLETIONS_PATH = "/openai/v1/chat/completions";

    // A small, fast Groq-hosted model — good enough for grounded,
    // conversational answers about a single property/neighbourhood
    // without the latency/cost of a larger model.
    private static final String MODEL = "openai/gpt-oss-20b";

    private final RestClient groqRestClient;
    private final GroqProperties groqProperties;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Autowired
    public GroqAiService(@Qualifier("groqRestClient") RestClient groqRestClient,
                          GroqProperties groqProperties) {
        this.groqRestClient = groqRestClient;
        this.groqProperties = groqProperties;
    }

    @Override
    public String generateReply(String systemPrompt, String userMessage) {
        String apiKey = groqProperties.getKey();
        if (apiKey == null || apiKey.isBlank()) {
            // Not configured — fail fast, before attempting any HTTP call,
            // and never send a request with a missing/blank key.
            throw new AiServiceException("Groq API is not configured");
        }

        ObjectNode requestBody = buildRequestBody(systemPrompt, userMessage);

        JsonNode response;
        try {
            response = groqRestClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // Covers non-2xx responses (including 401/403 for a bad key
            // and 429 for rate limiting), timeouts, and connectivity
            // failures. Deliberately does not include the raw exception
            // (which could echo request details) in the thrown message —
            // only a generic, key-free description. The original
            // exception is still attached as the cause for local
            // debugging/logging by the caller, but its toString() is
            // never itself embedded in this exception's own message text.
            throw new AiServiceException("Groq API request failed", e);
        }

        return extractReplyContent(response);
    }

    /** Builds the OpenAI-compatible chat completion request body. */
    private ObjectNode buildRequestBody(String systemPrompt, String userMessage) {
        ObjectNode requestBody = jsonMapper.createObjectNode();
        requestBody.put("model", MODEL);

        ArrayNode messages = requestBody.putArray("messages");

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode systemMessage = jsonMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
        }

        ObjectNode userMessageNode = jsonMapper.createObjectNode();
        userMessageNode.put("role", "user");
        userMessageNode.put("content", userMessage == null ? "" : userMessage);
        messages.add(userMessageNode);

        return requestBody;
    }

    /** Extracts choices[0].message.content, throwing if the response shape doesn't contain usable text. */
    private String extractReplyContent(JsonNode response) {
        if (response == null) {
            throw new AiServiceException("Groq API returned an empty response");
        }

        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new AiServiceException("Groq API response did not contain any choices");
        }

        JsonNode content = choices.get(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new AiServiceException("Groq API response did not contain reply content");
        }

        return content.asText();
    }
}
