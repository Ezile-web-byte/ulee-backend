package com.ulee.ulee_backend.service;

/**
 * Thrown when AiService cannot produce a reply — an unconfigured/blank API
 * key, an HTTP error, a timeout, a malformed response, or a response with
 * no usable reply content. Unlike PlacesService (which swallows every
 * failure into PlacesResult.unavailable()), AiService deliberately lets
 * this propagate: the caller (SwaiChatService, added in a later stage) is
 * the one place that decides the user-facing fallback message, so AI
 * failures must not be swallowed here.
 *
 * The message on this exception is always a short, generic, human-readable
 * description of what went wrong at the category level (e.g. "Groq API
 * request failed", "not configured") — never the raw provider exception,
 * response body, or request details, so the Groq API key can never leak
 * into a log line or an upstream error message via this exception's
 * toString()/getMessage().
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
