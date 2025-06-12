package com.horizon.eventservice.service;

import com.horizon.eventservice.dto.EventCreateDTO;
import com.horizon.eventservice.dto.EventResponseDTO;
import com.horizon.eventservice.dto.EventUpdateDTO;
import com.horizon.eventservice.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventService {
    Optional<EventResponseDTO> getEventById(UUID id);
    List<EventResponseDTO> getAllEvents();
    Event createEvent(EventCreateDTO createDTO);
    Optional<Event> updateEvent(EventUpdateDTO updateDTO);
    void deleteEventById(UUID id);
    void removeUserFromAllEvents(String userId);
} 