package com.ulee.ulee_backend.service;

/**
 * Generates a natural-language reply from a grounded prompt. Isolated
 * behind this interface so the concrete LLM provider (currently Groq, see
 * GroqAiService) can be swapped later without changing any caller.
 *
 * Implementations must never fabricate grounding facts themselves — this
 * interface only turns already-assembled context (built by the caller,
 * e.g. SwaiChatService in a later stage, from ULEE property data and/or
 * PlacesService results) into conversational text. Unlike PlacesService,
 * implementations must NOT swallow failures: any condition that prevents a
 * trustworthy reply (unconfigured/blank API key, HTTP error, timeout,
 * malformed response, or a response with no usable content) must throw
 * AiServiceException so the caller can choose the correct user-facing
 * fallback.
 */
public interface AiService {

    /**
     * @param systemPrompt instructions establishing the assistant's role
     *                      and the grounded facts it may use
     * @param userMessage  the student's question
     * @return the assistant's reply text
     * @throws AiServiceException if a trustworthy reply could not be produced
     */
    String generateReply(String systemPrompt, String userMessage);
}
