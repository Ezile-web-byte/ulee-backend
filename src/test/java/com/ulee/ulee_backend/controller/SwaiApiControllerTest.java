package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.dto.ChatResponseDTO;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.repository.PropertyImageRepository;
import com.ulee.ulee_backend.repository.PropertyRepository;
import com.ulee.ulee_backend.repository.UserRepository;
import com.ulee.ulee_backend.service.SwaiChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level tests for SwaiApiController. Repositories and
 * SwaiChatService are mocked; this class verifies only HTTP adaptation
 * (request parsing, delegation, response shape). Security filters are
 * disabled for this slice (addFilters = false).
 */
@WebMvcTest(SwaiApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class SwaiApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyRepository propertyRepository;
    @MockitoBean
    private PropertyImageRepository propertyImageRepository;
    @MockitoBean
    private SwaiChatService swaiChatService;
    @MockitoBean
    private UserRepository userRepository;

    // ---- POST /api/swai/chat -----------------------------------------

    @Test
    void chat_returnsChatResponseDTO_asJson() throws Exception {
        when(swaiChatService.handle(any(), any())).thenReturn(new ChatResponseDTO("Hello! How can I help?"));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\",\"propertyId\":123}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello! How can I help?"));
    }

    @Test
    void chat_passesMessageAndPropertyId_toSwaiChatService() throws Exception {
        when(swaiChatService.handle(any(), any())).thenReturn(new ChatResponseDTO("ok"));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is the rent?\",\"propertyId\":42}"))
                .andExpect(status().isOk());

        verify(swaiChatService).handle(argThat(request ->
                "What is the rent?".equals(request.getMessage())
                        && Integer.valueOf(42).equals(request.getPropertyId())), any());
    }

    @Test
    void chat_withNullPropertyId_isAcceptedAndPassedThrough() throws Exception {
        when(swaiChatService.handle(any(), any())).thenReturn(new ChatResponseDTO("General context reply."));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is ULEE?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("General context reply."));

        verify(swaiChatService).handle(argThat(request ->
                "What is ULEE?".equals(request.getMessage())
                        && request.getPropertyId() == null), any());
    }

    @Test
    void chat_withNoRequestBody_stillCallsService_withNullRequest() throws Exception {
        when(swaiChatService.handle(null, null)).thenReturn(new ChatResponseDTO("Please type a question."));

        // No body at all: required = false on @RequestBody means Spring
        // passes null instead of rejecting the request with a 400.
        mockMvc.perform(post("/api/swai/chat").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Please type a question."));

        verify(swaiChatService).handle(null, null);
    }

    @Test
    void chat_isAPureAdapter_returnsExactlyWhatServiceReturns_noWrappingOrModification() throws Exception {
        String safeFallback = "I'm having trouble answering right now. Please try again in a moment.";
        when(swaiChatService.handle(any(), any())).thenReturn(new ChatResponseDTO(safeFallback));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is the nearest mall?\",\"propertyId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(safeFallback));
    }

    // ---- GET /api/listings --------------------------------------------

    @Test
    void listings_stillReturnsExpectedShape_unaffectedByChatEndpointAddition() throws Exception {
        Property property = new Property();
        property.setPropertyID(1);
        property.setTitle("The Dunes");
        property.setAddress("69 Zenios Place");
        property.setCity("Summerstrand");
        property.setRent(new BigDecimal("2800.00"));
        property.setType("Single Room");
        property.setCommuteType("Walking distance");

        when(propertyRepository.findByIsAvailableTrue()).thenReturn(List.of(property));
        when(propertyImageRepository.findByPropertyIDIn(List.of(1))).thenReturn(List.of());

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("The Dunes"))
                .andExpect(jsonPath("$[0].address").value("69 Zenios Place"))
                .andExpect(jsonPath("$[0].city").value("Summerstrand"))
                .andExpect(jsonPath("$[0].rent").value(2800.00))
                .andExpect(jsonPath("$[0].type").value("Single Room"))
                .andExpect(jsonPath("$[0].commuteType").value("Walking distance"));
    }

    @Test
    void listings_withNoAvailableProperties_returnsEmptyArray() throws Exception {
        when(propertyRepository.findByIsAvailableTrue()).thenReturn(List.of());
        when(propertyImageRepository.findByPropertyIDIn(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}