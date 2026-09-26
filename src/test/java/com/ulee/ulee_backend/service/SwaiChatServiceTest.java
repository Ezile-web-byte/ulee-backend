package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.dto.ChatRequestDTO;
import com.ulee.ulee_backend.dto.ChatResponseDTO;
import com.ulee.ulee_backend.dto.NearbyPlaceDTO;
import com.ulee.ulee_backend.dto.PlacesResult;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SwaiChatService. PropertyRepository, PlacesService, and
 * AiService are all mocked — no database, no Geoapify, no Groq is ever
 * called by this test class.
 */
@ExtendWith(MockitoExtension.class)
class SwaiChatServiceTest {

    private static final Integer PROPERTY_ID = 7;
    private static final BigDecimal LATITUDE = new BigDecimal("-33.9608");
    private static final BigDecimal LONGITUDE = new BigDecimal("25.6022");

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private PlacesService placesService;
    @Mock
    private AiService aiService;

    private SwaiChatService service;

    @BeforeEach
    void setUp() {
        service = new SwaiChatService(propertyRepository, placesService, aiService);
    }

    private Property propertyWithCoordinates() {
        Property property = new Property();
        property.setPropertyID(PROPERTY_ID);
        property.setTitle("The Dunes");
        property.setAddress("69 Zenios Place");
        property.setCity("Summerstrand");
        property.setRent(new BigDecimal("2800.00"));
        property.setLatitude(LATITUDE);
        property.setLongitude(LONGITUDE);
        return property;
    }

    private Property propertyWithoutCoordinates() {
        Property property = new Property();
        property.setPropertyID(PROPERTY_ID);
        property.setTitle("The Dunes");
        property.setAddress("69 Zenios Place");
        property.setCity("Summerstrand");
        property.setRent(new BigDecimal("2800.00"));
        property.setLatitude(null);
        property.setLongitude(null);
        return property;
    }

    // ---- Basic input handling -------------------------------------

    @Test
    void nullMessage_returnsPromptWithoutCallingAnyService() {
        ChatRequestDTO request = new ChatRequestDTO(null, PROPERTY_ID);

        ChatResponseDTO response = service.handle(request);

        assertNotNull(response.getReply());
        verifyNoInteractions(propertyRepository, placesService, aiService);
    }

    @Test
    void blankMessage_returnsPromptWithoutCallingAnyService() {
        ChatRequestDTO request = new ChatRequestDTO("   ", PROPERTY_ID);

        ChatResponseDTO response = service.handle(request);

        assertNotNull(response.getReply());
        verifyNoInteractions(propertyRepository, placesService, aiService);
    }

    @Test
    void propertyNotFound_proceedsInGeneralContext_doesNotThrow() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());
        when(aiService.generateReply(anyString(), anyString())).thenReturn("A general reply.");

        ChatRequestDTO request = new ChatRequestDTO("What is the rent?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertEquals("A general reply.", response.getReply());
        verify(placesService, never()).findNearbyPlaces(any(), any());
    }

    @Test
    void nullPropertyId_neverCallsPropertyRepository() {
        when(aiService.generateReply(anyString(), anyString())).thenReturn("A general reply.");

        ChatRequestDTO request = new ChatRequestDTO("What is the rent?", null);
        service.handle(request);

        verify(propertyRepository, never()).findById(any());
    }

    // ---- Neighbourhood question: missing coordinates ---------------

    @Test
    void neighbourhoodQuestion_missingCoordinates_returnsFallback_neverCallsPlacesOrAi() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithoutCoordinates()));

        ChatRequestDTO request = new ChatRequestDTO("What is the nearest mall?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertTrue(response.getReply().toLowerCase().contains("neighbourhood")
                || response.getReply().toLowerCase().contains("don't have"));
        verify(placesService, never()).findNearbyPlaces(any(), any());
        verify(aiService, never()).generateReply(anyString(), anyString());
    }

    @Test
    void neighbourhoodQuestion_noPropertyInContext_returnsFallback_neverCallsPlacesOrAi() {
        ChatRequestDTO request = new ChatRequestDTO("Are there any restaurants nearby?", null);
        ChatResponseDTO response = service.handle(request);

        assertNotNull(response.getReply());
        verify(placesService, never()).findNearbyPlaces(any(), any());
        verify(aiService, never()).generateReply(anyString(), anyString());
    }

    // ---- Neighbourhood question: PlacesResult unavailable -----------

    @Test
    void neighbourhoodQuestion_placesUnavailable_returnsFallback_neverCallsAi() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        when(placesService.findNearbyPlaces(LATITUDE, LONGITUDE)).thenReturn(PlacesResult.unavailable());

        ChatRequestDTO request = new ChatRequestDTO("Is there a gym nearby?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertNotNull(response.getReply());
        verify(aiService, never()).generateReply(anyString(), anyString());
    }

    // ---- Neighbourhood question: successful lookup, zero results ----

    @Test
    void neighbourhoodQuestion_successfulLookupZeroResults_callsAi_promptSaysNoneFound() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        when(placesService.findNearbyPlaces(LATITUDE, LONGITUDE)).thenReturn(PlacesResult.success(List.of()));
        when(aiService.generateReply(anyString(), anyString())).thenReturn("No matching places were found nearby.");

        ChatRequestDTO request = new ChatRequestDTO("Are there any churches nearby?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertEquals("No matching places were found nearby.", response.getReply());

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).generateReply(systemPromptCaptor.capture(), eq("Are there any churches nearby?"));
        String systemPrompt = systemPromptCaptor.getValue();
        assertTrue(systemPrompt.toLowerCase().contains("no matching"));
        assertTrue(systemPrompt.toLowerCase().contains("do not invent"));
    }

    // ---- Neighbourhood question: successful lookup with places ------

    @Test
    void neighbourhoodQuestion_successfulLookupWithPlaces_promptContainsFactualPlaceContext() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        List<NearbyPlaceDTO> places = List.of(
                new NearbyPlaceDTO("Baywest Mall", "shopping_mall", 1200.5, "Baywest Mall, Gqeberha"),
                new NearbyPlaceDTO("Corner Pharmacy", "pharmacy", 350.0, "Main Road, Gqeberha")
        );
        when(placesService.findNearbyPlaces(LATITUDE, LONGITUDE)).thenReturn(PlacesResult.success(places));
        when(aiService.generateReply(anyString(), anyString()))
                .thenReturn("Baywest Mall is about 1.2km away, and there's a pharmacy closer by.");

        ChatRequestDTO request = new ChatRequestDTO("What is the nearest mall?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertEquals("Baywest Mall is about 1.2km away, and there's a pharmacy closer by.", response.getReply());

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).generateReply(systemPromptCaptor.capture(), eq("What is the nearest mall?"));
        String systemPrompt = systemPromptCaptor.getValue();

        // Factual property context present
        assertTrue(systemPrompt.contains("The Dunes"));
        assertTrue(systemPrompt.contains("69 Zenios Place"));
        assertTrue(systemPrompt.contains("Summerstrand"));

        // Factual nearby-place context present: name, category, distance, address
        assertTrue(systemPrompt.contains("Baywest Mall"));
        assertTrue(systemPrompt.contains("shopping_mall"));
        assertTrue(systemPrompt.contains("1200") || systemPrompt.contains("1,200"));
        assertTrue(systemPrompt.contains("Corner Pharmacy"));
        assertTrue(systemPrompt.contains("pharmacy"));
        assertTrue(systemPrompt.contains("350"));

        // Grounding / no-invention instructions present
        assertTrue(systemPrompt.toLowerCase().contains("do not invent"));
    }

    // ---- Non-neighbourhood question ---------------------------------

    @Test
    void nonNeighbourhoodQuestion_neverCallsPlacesService() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        when(aiService.generateReply(anyString(), anyString())).thenReturn("The rent is R2800 per month.");

        ChatRequestDTO request = new ChatRequestDTO("What is the rent for this property?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertEquals("The rent is R2800 per month.", response.getReply());
        verify(placesService, never()).findNearbyPlaces(any(), any());
    }

    @Test
    void nonNeighbourhoodQuestion_promptContainsPropertyContext_noInventionInstruction() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        when(aiService.generateReply(anyString(), anyString())).thenReturn("Some reply.");

        ChatRequestDTO request = new ChatRequestDTO("How many bedrooms does this have?", PROPERTY_ID);
        service.handle(request);

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).generateReply(systemPromptCaptor.capture(), eq("How many bedrooms does this have?"));
        String systemPrompt = systemPromptCaptor.getValue();

        assertTrue(systemPrompt.contains("The Dunes"));
        assertTrue(systemPrompt.toLowerCase().contains("do not invent"));
    }

    // ---- AiServiceException handling --------------------------------

    @Test
    void aiServiceException_producesSafeFallback_doesNotLeakExceptionDetails() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        when(aiService.generateReply(anyString(), anyString()))
                .thenThrow(new AiServiceException("Groq API request failed"));

        ChatRequestDTO request = new ChatRequestDTO("What is the rent?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertNotNull(response.getReply());
        assertFalse(response.getReply().contains("Groq"));
        assertFalse(response.getReply().contains("Exception"));
        assertFalse(response.getReply().contains("api-key"));
    }

    @Test
    void aiServiceException_duringNeighbourhoodQuestion_producesSafeFallback() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        when(placesService.findNearbyPlaces(LATITUDE, LONGITUDE))
                .thenReturn(PlacesResult.success(List.of(
                        new NearbyPlaceDTO("Baywest Mall", "shopping_mall", 1200.0, null))));
        when(aiService.generateReply(anyString(), anyString()))
                .thenThrow(new AiServiceException("Groq API request failed"));

        ChatRequestDTO request = new ChatRequestDTO("What is the nearest mall?", PROPERTY_ID);
        ChatResponseDTO response = service.handle(request);

        assertNotNull(response.getReply());
        assertFalse(response.getReply().contains("Groq"));
    }

    // ---- Intent heuristic (direct unit test of the pure method) -----

    @Test
    void isNeighbourhoodQuestion_detectsExpectedKeywords() {
        assertTrue(service.isNeighbourhoodQuestion("What is the nearest mall?"));
        assertTrue(service.isNeighbourhoodQuestion("Are there any restaurants nearby?"));
        assertTrue(service.isNeighbourhoodQuestion("Is this area convenient if I don't have a car?"));
        assertTrue(service.isNeighbourhoodQuestion("What can I do around here on weekends?"));
        assertFalse(service.isNeighbourhoodQuestion("What is the rent for this property?"));
        assertFalse(service.isNeighbourhoodQuestion("How many bedrooms does it have?"));
    }

    @Test
    void placesService_calledExactlyOnce_forNeighbourhoodQuestionWithCoordinates() {
        when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(propertyWithCoordinates()));
        when(placesService.findNearbyPlaces(LATITUDE, LONGITUDE)).thenReturn(PlacesResult.success(List.of()));
        when(aiService.generateReply(anyString(), anyString())).thenReturn("reply");

        ChatRequestDTO request = new ChatRequestDTO("Are there any gyms nearby?", PROPERTY_ID);
        service.handle(request);

        verify(placesService, times(1)).findNearbyPlaces(LATITUDE, LONGITUDE);
    }
}
