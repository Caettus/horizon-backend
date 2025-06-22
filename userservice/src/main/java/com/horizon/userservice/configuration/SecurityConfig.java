package com.horizon.userservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Injecteer een property om security aan/uit te zetten. Standaard staat het aan.
    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            // Als security is uitgeschakeld, sta alle requests toe en schakel CSRF uit.
            // Dit is ideaal voor de test-omgeving.
            http
                    .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
        } else {
            // Dit is de originele, productie-veilige configuratie.
            http
                    .authorizeHttpRequests(authz -> authz
                            .requestMatchers("/public/**", "/actuator/**").permitAll()
                            .requestMatchers("/users/internal/synchronize").permitAll()
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(withDefaults())
                    );
        }
        return http.build();
    }
}