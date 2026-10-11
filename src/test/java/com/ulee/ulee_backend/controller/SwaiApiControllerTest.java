package com.ulee.ulee_backend.controller;

import com.ulee.ulee_backend.dto.ChatRequestDTO;
import com.ulee.ulee_backend.dto.ChatResponseDTO;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.repository.PropertyImageRepository;
import com.ulee.ulee_backend.repository.PropertyRepository;
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
 * Controller-level tests for SwaiApiController. PropertyRepository,
 * PropertyImageRepository, and SwaiChatService are all mocked — this
 * class verifies only HTTP adaptation (request parsing, delegation,
 * response shape), never any business logic, since the controller itself
 * contains none: all intent detection, property lookup, Geoapify/Groq
 * calls, and fallback decisions are already covered by
 * SwaiChatServiceTest, GeoapifyPlacesServiceTest, and GroqAiServiceTest.
 *
 * Security filters are disabled for this slice (@AutoConfigureMockMvc
 * (addFilters = false)) so these tests isolate controller behavior from
 * the security filter chain — the permitAll() addition for
 * /api/swai/chat in SecurityConfig is a one-line, directly readable
 * change and is not re-verified via a separate filter-chain integration
 * test here.
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

    // ---- POST /api/swai/chat -----------------------------------------

    @Test
    void chat_returnsChatResponseDTO_asJson() throws Exception {
        when(swaiChatService.handle(any())).thenReturn(new ChatResponseDTO("Hello! How can I help?"));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\",\"propertyId\":123}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello! How can I help?"));
    }

    @Test
    void chat_passesMessageAndPropertyId_toSwaiChatService() throws Exception {
        when(swaiChatService.handle(any())).thenReturn(new ChatResponseDTO("ok"));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is the rent?\",\"propertyId\":42}"))
                .andExpect(status().isOk());

        verify(swaiChatService).handle(argThat(request ->
                "What is the rent?".equals(request.getMessage())
                        && Integer.valueOf(42).equals(request.getPropertyId())));
    }

    @Test
    void chat_withNullPropertyId_isAcceptedAndPassedThrough() throws Exception {
        when(swaiChatService.handle(any())).thenReturn(new ChatResponseDTO("General context reply."));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is ULEE?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("General context reply."));

        verify(swaiChatService).handle(argThat(request ->
                "What is ULEE?".equals(request.getMessage())
                        && request.getPropertyId() == null));
    }

    @Test
    void chat_withNoRequestBody_stillCallsService_withNullRequest() throws Exception {
        when(swaiChatService.handle(null)).thenReturn(new ChatResponseDTO("Please type a question."));

        // No .content(...) at all — required = false on @RequestBody means
        // Spring resolves the parameter to null rather than rejecting the
        // request with a 400.
        mockMvc.perform(post("/api/swai/chat").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Please type a question."));

        verify(swaiChatService).handle(null);
    }

    @Test
    void chat_isAPureAdapter_returnsExactlyWhatServiceReturns_noWrappingOrModification() throws Exception {
        // Demonstrates the controller cannot introduce a new leak of
        // exception details, provider names, or keys: whatever
        // SwaiChatService.handle(...) decides is safe to return (which is
        // where every fallback/error-handling decision lives — see
        // SwaiChatServiceTest) is passed straight through, byte for byte,
        // with no additional processing in this controller.
        String safeFallback = "I'm having trouble answering right now. Please try again in a moment.";
        when(swaiChatService.handle(any())).thenReturn(new ChatResponseDTO(safeFallback));

        mockMvc.perform(post("/api/swai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is the nearest mall?\",\"propertyId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(safeFallback));
    }

    // ---- GET /api/listings — preserved exactly ------------------------

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
