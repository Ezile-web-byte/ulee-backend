package com.ulee.ulee_backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides one pre-configured RestClient per outbound SWAI integration
 * (Geoapify Places, Groq LLM), each pinned to that provider's base URL.
 *
 * Services consume these via constructor injection with @Qualifier,
 * rather than building their own RestClient, so:
 *   - the base URL lives in exactly one place per provider (here), and
 *   - tests can substitute a RestClient bound to MockRestServiceServer
 *     without any change to the service implementation.
 *
 * Spring's synchronous RestClient (spring-web, already on the classpath
 * via spring-boot-starter-webmvc) is used instead of WebClient/Reactor —
 * both outbound calls here are simple, blocking request/response calls
 * with no streaming or reactive composition need, so pulling in WebFlux
 * would be an unjustified new dependency for this use case.
 */
@Configuration
public class SwaiRestClientConfig {

    @Bean
    @Qualifier("geoapifyRestClient")
    public RestClient geoapifyRestClient(GeoapifyProperties geoapifyProperties) {
        return RestClient.builder()
                .baseUrl(geoapifyProperties.getBaseUrl())
                .build();
    }

    @Bean
    @Qualifier("groqRestClient")
    public RestClient groqRestClient(GroqProperties groqProperties) {
        return RestClient.builder()
                .baseUrl(groqProperties.getBaseUrl())
                .build();
    }
}
