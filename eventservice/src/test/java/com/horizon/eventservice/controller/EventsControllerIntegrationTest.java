package com.horizon.eventservice.controller;

import com.horizon.eventservice.DTO.EventCreateDTO;
import com.horizon.eventservice.DTO.EventResponseDTO;
import com.horizon.eventservice.DTO.EventUpdateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
public class EventsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCRUDOperationsForEventsController() throws Exception {
        // --- CREATE an event ---
        EventCreateDTO createDTO = new EventCreateDTO();
        createDTO.setTitle("Test Event");
        createDTO.setDescription("This is a test event");
        createDTO.setLocation("Test Location");
        createDTO.setStartDate(LocalDateTime.now().plusDays(1));
        createDTO.setEndDate(LocalDateTime.now().plusDays(1).plusHours(2));
        createDTO.setCategory("Testing");
        createDTO.setTags(Arrays.asList("tag1", "tag2"));
        createDTO.setPrivate(false);
        createDTO.setOrganizerId(UUID.randomUUID());
        createDTO.setImageUrl("http://example.com/image.png");

        String createJson = objectMapper.writeValueAsString(createDTO);

        MvcResult createResult = mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andReturn();

        String createdContent = createResult.getResponse().getContentAsString();
        EventResponseDTO createdEvent = objectMapper.readValue(createdContent, EventResponseDTO.class);
        UUID eventId = createdEvent.getId();

        // --- READ by ID ---
        mockMvc.perform(get("/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.title").value("Test Event"));

        // --- READ all ---
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId.toString()));

        // --- UPDATE the event ---
        EventUpdateDTO updateDTO = new EventUpdateDTO();
        updateDTO.setTitle("Updated Event");
        updateDTO.setDescription("Updated description");
        updateDTO.setLocation("Updated Location");
        updateDTO.setStartDate(createDTO.getStartDate().plusDays(2));
        updateDTO.setEndDate(createDTO.getEndDate().plusDays(2).plusHours(1));
        updateDTO.setCategory("Updated Category");
        updateDTO.setTags(Arrays.asList("updatedTag"));
        updateDTO.setPrivate(true);

        String updateJson = objectMapper.writeValueAsString(updateDTO);

        mockMvc.perform(put("/events/{id}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Event"))
                .andExpect(jsonPath("$.description").value("Updated description"));

        // --- DELETE the event ---
        mockMvc.perform(delete("/events/{id}", eventId))
                .andExpect(status().isNoContent());

        // --- VERIFY deletion ---
        mockMvc.perform(get("/events/{id}", eventId))
                .andExpect(status().isNotFound());
    }
}
