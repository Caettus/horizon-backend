package com.horizon.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                // Example: Allow unauthenticated access to actuator health, otherwise require authentication
                // .pathMatchers("/actuator/health").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults()) // Configure JWT validation with properties
            );
        // CSRF protection is typically enabled by default. For stateless APIs (gateways often are),
        // you might disable it if not using session-based CSRF tokens.
        // http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }
} 