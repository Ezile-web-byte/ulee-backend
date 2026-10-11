package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.config.GeoapifyProperties;
import com.ulee.ulee_backend.dto.NearbyPlaceDTO;
import com.ulee.ulee_backend.dto.PlacesResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

/**
 * Unit tests for GeoapifyPlacesService. All HTTP interaction is mocked via
 * MockRestServiceServer bound to a RestClient — no live Geoapify calls are
 * made, and no real GEOAPIFY_API_KEY is required or used.
 */
class GeoapifyPlacesServiceTest {

    private static final String BASE_URL = "https://fake-geoapify.test";
    private static final BigDecimal LATITUDE = new BigDecimal("-33.9608");
    private static final BigDecimal LONGITUDE = new BigDecimal("25.6022");

    private MockRestServiceServer mockServer;
    private GeoapifyPlacesService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        GeoapifyProperties properties = new GeoapifyProperties();
        properties.setKey("test-api-key");
        properties.setBaseUrl(BASE_URL);

        service = new GeoapifyPlacesService(restClient, properties);
    }

    @Test
    void returnsUnavailable_whenLatitudeIsNull_andMakesNoHttpCall() {
        PlacesResult result = service.findNearbyPlaces(null, LONGITUDE);

        assertFalse(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());
        mockServer.verify(); // no expectations registered; fails if any request was made
    }

    @Test
    void returnsUnavailable_whenLongitudeIsNull_andMakesNoHttpCall() {
        PlacesResult result = service.findNearbyPlaces(LATITUDE, null);

        assertFalse(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());
        mockServer.verify();
    }

    @Test
    void returnsUnavailable_whenApiKeyIsBlank_andMakesNoHttpCall() {
        GeoapifyProperties blankKeyProperties = new GeoapifyProperties();
        blankKeyProperties.setKey("");
        blankKeyProperties.setBaseUrl(BASE_URL);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GeoapifyPlacesService unconfiguredService = new GeoapifyPlacesService(restClient, blankKeyProperties);

        PlacesResult result = unconfiguredService.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertFalse(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());
        server.verify();
    }

    @Test
    void returnsUnavailable_whenApiKeyIsNull_andMakesNoHttpCall() {
        GeoapifyProperties nullKeyProperties = new GeoapifyProperties();
        nullKeyProperties.setKey(null);
        nullKeyProperties.setBaseUrl(BASE_URL);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GeoapifyPlacesService unconfiguredService = new GeoapifyPlacesService(restClient, nullKeyProperties);

        PlacesResult result = unconfiguredService.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertFalse(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());
        server.verify();
    }

    @Test
    void returnsSuccessWithMappedPlaces_whenGeoapifyRespondsWithFeatures() {
        String body = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {
                        "name": "Baywest Mall",
                        "formatted": "Baywest Mall, Gqeberha, South Africa",
                        "distance": 1200.5,
                        "categories": ["commercial", "commercial.shopping_mall"]
                      }
                    },
                    {
                      "type": "Feature",
                      "properties": {
                        "name": "Corner Pharmacy",
                        "formatted": "Main Road, Gqeberha, South Africa",
                        "distance": 350.0,
                        "categories": ["healthcare", "healthcare.pharmacy"]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/places")))
                .andExpect(queryParam("apiKey", "test-api-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PlacesResult result = service.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertTrue(result.isAvailable());
        List<NearbyPlaceDTO> places = result.getPlaces();
        assertEquals(2, places.size());

        NearbyPlaceDTO mall = places.get(0);
        assertEquals("Baywest Mall", mall.getName());
        assertEquals("shopping_mall", mall.getCategory());
        assertEquals(1200.5, mall.getDistanceMeters());
        assertEquals("Baywest Mall, Gqeberha, South Africa", mall.getAddress());

        NearbyPlaceDTO pharmacy = places.get(1);
        assertEquals("Corner Pharmacy", pharmacy.getName());
        // More specific "healthcare.pharmacy" must win over the broader
        // "healthcare" label per CATEGORY_LABELS ordering.
        assertEquals("pharmacy", pharmacy.getCategory());

        mockServer.verify();
    }

    @Test
    void returnsSuccessWithEmptyPlaces_whenGeoapifyRespondsWithNoFeatures_notUnavailable() {
        String body = """
                {
                  "type": "FeatureCollection",
                  "features": []
                }
                """;

        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/places")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PlacesResult result = service.findNearbyPlaces(LATITUDE, LONGITUDE);

        // Critical distinction (per approved architecture): a successful
        // lookup with zero matches is available=true, NOT unavailable.
        assertTrue(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());

        mockServer.verify();
    }

    @Test
    void returnsUnavailable_whenGeoapifyRespondsWithServerError() {
        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/places")))
                .andRespond(withServerError());

        PlacesResult result = service.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertFalse(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());
        mockServer.verify();
    }

    @Test
    void returnsUnavailable_whenGeoapifyRateLimits() {
        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/places")))
                .andRespond(withStatus(TOO_MANY_REQUESTS));

        PlacesResult result = service.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertFalse(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());
        mockServer.verify();
    }

    @Test
    void returnsUnavailable_whenGeoapifyReturnsUnauthorized_badApiKey() {
        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/places")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        PlacesResult result = service.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertFalse(result.isAvailable());
        assertTrue(result.getPlaces().isEmpty());
        mockServer.verify();
    }

    @Test
    void skipsUnnamedFeatures() {
        String body = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {
                        "formatted": "Unnamed place, Gqeberha",
                        "distance": 500.0,
                        "categories": ["catering.restaurant"]
                      }
                    },
                    {
                      "type": "Feature",
                      "properties": {
                        "name": "Beerhouse",
                        "formatted": "Beerhouse, Gqeberha",
                        "distance": 600.0,
                        "categories": ["catering.pub"]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/places")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PlacesResult result = service.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertTrue(result.isAvailable());
        assertEquals(1, result.getPlaces().size());
        assertEquals("Beerhouse", result.getPlaces().get(0).getName());
        assertEquals("nightlife", result.getPlaces().get(0).getCategory());

        mockServer.verify();
    }

    @Test
    void categorizesUnknownCategoryAsOther() {
        String body = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {
                        "name": "Mystery Spot",
                        "formatted": "Somewhere, Gqeberha",
                        "distance": 100.0,
                        "categories": ["office.lawyer"]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/v2/places")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PlacesResult result = service.findNearbyPlaces(LATITUDE, LONGITUDE);

        assertTrue(result.isAvailable());
        assertEquals(1, result.getPlaces().size());
        assertEquals("other", result.getPlaces().get(0).getCategory());

        mockServer.verify();
    }
}
