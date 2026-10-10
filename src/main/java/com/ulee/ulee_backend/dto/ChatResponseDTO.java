package com.ulee.ulee_backend.dto;

/**
 * Response body for the SWAI chat endpoint (added in a later stage).
 * Kept minimal for v1 — just the assistant's reply text. The reply may be
 * either a grounded LLM answer or a deterministic fallback message; the
 * frontend renders either the same way, so no extra flag is needed yet.
 */
public class ChatResponseDTO {

    private String reply;

    public ChatResponseDTO() {
    }

    public ChatResponseDTO(String reply) {
        this.reply = reply;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
}
