package com.horizon.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Component
public class UserSyncGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserSyncGlobalFilter.class);
    private final WebClient.Builder webClientBuilder;


    private final String userServiceUri = "http://userservice-service:8081";

    public UserSyncGlobalFilter(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    // Define a DTO that matches UserSyncRequestDTO in userservice
    private static class UserSyncRequest {
        private String keycloakId;
        private String username;
        private String email;

        public UserSyncRequest(String keycloakId, String username, String email) {
            this.keycloakId = keycloakId;
            this.username = username;
            this.email = email;
        }
        public String getKeycloakId() { return keycloakId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .map(JwtAuthenticationToken::getToken)
            .flatMap(jwt -> {
                // Check if already synced recently (e.g. via a custom claim in JWT set by Keycloak after first sync, or a flag in session if stateful)
                // For a stateless approach, one might call it always, relying on idempotency,
                // or use a short-lived cache in the gateway (not ideal for distributed gateways).
                // For simplicity, this filter will attempt to call it if principal is present.
                // More sophisticated logic (e.g. "first request after login") is harder in a stateless filter.

                String keycloakId = jwt.getSubject();
                String username = jwt.getClaimAsString("preferred_username"); // Common Keycloak claim
                String email = jwt.getClaimAsString("email");

                if (keycloakId == null || username == null || email == null) {
                    log.warn("Missing claims for user sync: keycloakId, preferred_username, or email. Skipping sync.");
                    return chain.filter(exchange); // Proceed without sync if claims are missing
                }

                UserSyncRequest syncRequest = new UserSyncRequest(keycloakId, username, email);
                log.info("Attempting to synchronize user: {}", keycloakId);

                return webClientBuilder.build()
                    .post()
                    .uri(userServiceUri + "/users/internal/synchronize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(syncRequest)
                    .retrieve()
                    .toBodilessEntity() // We don't care about the response body much, just that it succeeds
                    .doOnSuccess(response -> log.info("User sync call for {} completed with status: {}", keycloakId, response.getStatusCode()))
                    .doOnError(error -> log.error("User sync call for {} failed: {}", keycloakId, error.getMessage()))
                    .then(chain.filter(exchange))
                    .onErrorResume(e -> {
                        // If user sync call fails, log and continue the chain. Don't break user's request.
                        log.error("Error during user sync, proceeding with original request: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
            })
            .switchIfEmpty(chain.filter(exchange)); // If no principal, just continue
    }

    @Override
    public int getOrder() {
        // Run after Spring Security's authentication filter, e.g., AuthenticationWebFilter.DEFAULT_ORDER (0)
        // Or SecurityContextServerWebExchangeWebFilter.getOrder() which is -100.
        // We need the principal to be populated.
        return Ordered.HIGHEST_PRECEDENCE + 101; // Example: Just after security context is established.
        // Or use a specific order like SecurityReactorContextConfiguration.SecurityReactorContextSubscriber.ORDER + 1
    }
} 