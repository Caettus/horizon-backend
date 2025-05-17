package com.horizon.userservice.controller;

import com.horizon.userservice.DTO.UserCreateDTO;
import com.horizon.userservice.DTO.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/users";
    }

    @Test
    void createUser_whenValidInput_thenReturnsCreatedUser() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("testuser");
        dto.setEmail("test" + System.currentTimeMillis() + "@example.com");
        dto.setAge("25");
        dto.setKeycloakId("test-keycloak-id-" + System.currentTimeMillis());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<UserCreateDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<UserResponseDTO> response = restTemplate.postForEntity(baseUrl(), request, UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
    }
}

