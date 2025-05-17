package com.horizon.userservice.config; // Adjust package as per your project structure

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import static org.mockito.Mockito.mock;

@Configuration
@Profile("test") // This configuration will only be active when the "test" profile is active
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll() // Allow all requests for the "test" profile
                )
                .csrf(AbstractHttpConfigurer::disable); // Disable CSRF for easier testing with TestRestTemplate
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Provide a mock JwtDecoder for tests
        // This avoids needing a real JWT issuer URI or JWK set URI during tests
        return mock(JwtDecoder.class);
    }
}