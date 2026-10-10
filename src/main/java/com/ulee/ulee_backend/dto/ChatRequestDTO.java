package com.ulee.ulee_backend.dto;

/**
 * Request body for the SWAI chat endpoint (added in a later stage).
 * propertyId is a HINT only — the backend re-resolves the actual Property
 * from the database by this id and never trusts any other property detail
 * the browser might send, per the SWAI security/reliability requirements.
 * Null propertyId means General_Context (no specific property in view).
 */
public class ChatRequestDTO {

    private String message;
    private Integer propertyId;

    public ChatRequestDTO() {
    }

    public ChatRequestDTO(String message, Integer propertyId) {
        this.message = message;
        this.propertyId = propertyId;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getPropertyId() { return propertyId; }
    public void setPropertyId(Integer propertyId) { this.propertyId = propertyId; }
}
