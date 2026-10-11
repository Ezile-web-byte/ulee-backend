package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.config.GroqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Unit tests for GroqAiService. All HTTP interaction is mocked via
 * MockRestServiceServer bound to a RestClient — no live Groq calls are
 * made, and no real GROQ_API_KEY is required or used.
 */
class GroqAiServiceTest {

    private static final String BASE_URL = "https://fake-groq.test";

    private MockRestServiceServer mockServer;
    private GroqAiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        GroqProperties properties = new GroqProperties();
        properties.setKey("test-api-key");
        properties.setBaseUrl(BASE_URL);

        service = new GroqAiService(restClient, properties);
    }

    @Test
    void throwsAiServiceException_whenApiKeyIsBlank_andMakesNoHttpCall() {
        GroqProperties blankKeyProperties = new GroqProperties();
        blankKeyProperties.setKey("");
        blankKeyProperties.setBaseUrl(BASE_URL);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GroqAiService unconfiguredService = new GroqAiService(restClient, blankKeyProperties);

        AiServiceException exception = assertThrows(AiServiceException.class,
                () -> unconfiguredService.generateReply("system prompt", "What restaurants are nearby?"));

        assertTrue(exception.getMessage().contains("not configured"));
        assertTrue(!exception.getMessage().contains("test-api-key"));
        server.verify(); // no expectations registered; fails if any request was made
    }

    @Test
    void throwsAiServiceException_whenApiKeyIsNull_andMakesNoHttpCall() {
        GroqProperties nullKeyProperties = new GroqProperties();
        nullKeyProperties.setKey(null);
        nullKeyProperties.setBaseUrl(BASE_URL);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GroqAiService unconfiguredService = new GroqAiService(restClient, nullKeyProperties);

        assertThrows(AiServiceException.class,
                () -> unconfiguredService.generateReply("system prompt", "What restaurants are nearby?"));

        server.verify();
    }

    @Test
    void returnsReplyContent_whenGroqRespondsSuccessfully() {
        String body = """
                {
                  "id": "chatcmpl-123",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "There's a great restaurant called Beerhouse about 600m away."
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String reply = service.generateReply("You are a helpful assistant.", "What restaurants are nearby?");

        assertEquals("There's a great restaurant called Beerhouse about 600m away.", reply);
        mockServer.verify();
    }

    @Test
    void neverSendsApiKeyInRequestBody_onlyInAuthorizationHeader() {
        String body = """
                {
                  "choices": [
                    { "message": { "content": "Some reply." } }
                  ]
                }
                """;

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        service.generateReply("system prompt", "hello");

        mockServer.verify();
    }

    @Test
    void throwsAiServiceException_whenGroqRespondsWithServerError() {
        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withServerError());

        AiServiceException exception = assertThrows(AiServiceException.class,
                () -> service.generateReply("system prompt", "hello"));

        assertTrue(exception.getMessage().contains("Groq API request failed"));
        mockServer.verify();
    }

    @Test
    void throwsAiServiceException_whenGroqRateLimits() {
        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withStatus(TOO_MANY_REQUESTS));

        assertThrows(AiServiceException.class, () -> service.generateReply("system prompt", "hello"));
        mockServer.verify();
    }

    @Test
    void throwsAiServiceException_whenGroqReturnsUnauthorized_badApiKey() {
        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withStatus(UNAUTHORIZED));

        assertThrows(AiServiceException.class, () -> service.generateReply("system prompt", "hello"));
        mockServer.verify();
    }

    @Test
    void throwsAiServiceException_whenResponseHasNoChoices() {
        String body = """
                {
                  "choices": []
                }
                """;

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        AiServiceException exception = assertThrows(AiServiceException.class,
                () -> service.generateReply("system prompt", "hello"));

        assertTrue(exception.getMessage().contains("choices"));
        mockServer.verify();
    }

    @Test
    void throwsAiServiceException_whenMessageContentIsMissing() {
        String body = """
                {
                  "choices": [
                    { "message": { "role": "assistant" } }
                  ]
                }
                """;

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThrows(AiServiceException.class, () -> service.generateReply("system prompt", "hello"));
        mockServer.verify();
    }

    @Test
    void throwsAiServiceException_whenMessageContentIsBlank() {
        String body = """
                {
                  "choices": [
                    { "message": { "content": "   " } }
                  ]
                }
                """;

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThrows(AiServiceException.class, () -> service.generateReply("system prompt", "hello"));
        mockServer.verify();
    }

    @Test
    void throwsAiServiceException_whenResponseBodyIsMalformed() {
        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withSuccess("not valid json", MediaType.APPLICATION_JSON));

        assertThrows(AiServiceException.class, () -> service.generateReply("system prompt", "hello"));
        mockServer.verify();
    }

    @Test
    void omitsSystemMessage_whenSystemPromptIsBlank() {
        String body = """
                {
                  "choices": [
                    { "message": { "content": "A reply with no system prompt." } }
                  ]
                }
                """;

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(startsWith(BASE_URL + "/openai/v1/chat/completions")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String reply = service.generateReply("", "hello");

        assertEquals("A reply with no system prompt.", reply);
        mockServer.verify();
    }
}
