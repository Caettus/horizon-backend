package com.horizon.eventservice.Interface;

import com.horizon.eventservice.DTO.EventCreateDTO;
import com.horizon.eventservice.DTO.EventResponseDTO;
import com.horizon.eventservice.DTO.EventUpdateDTO;
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
