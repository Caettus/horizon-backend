package com.horizon.eventservice.Interface;

import com.horizon.eventservice.DTO.EventResponseDTO;
import com.horizon.eventservice.model.Event;
import com.horizon.eventservice.DAL.EventDAL;
import com.horizon.eventservice.DTO.EventResponseDTO;
import com.horizon.eventservice.DTO.EventCreateDTO;
import com.horizon.eventservice.DTO.EventUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventDAL eventDAL;

    public Optional<EventResponseDTO> getEventById(UUID id) {
        return eventDAL.findById(id).map(this::mapToDTO);
    }



    public List<EventResponseDTO> getAllEvents() {
        return eventDAL.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Event createEvent(EventCreateDTO createDTO) {
        Event event = new Event();
        event.setTitle(createDTO.getTitle());
        event.setDescription(createDTO.getDescription());
        event.setLocation(createDTO.getLocation());
        event.setStartDate(createDTO.getStartDate());
        event.setEndDate(createDTO.getEndDate());
        event.setCategory(createDTO.getCategory());
        event.setTags(createDTO.getTags());
        event.setPrivate(createDTO.isPrivate());
        event.setOrganizerId(createDTO.getOrganizerId());
        event.setStatus(Event.EventStatus.UPCOMING);
        event.setCreatedAt(java.time.LocalDateTime.now());
        return eventDAL.save(event);
    }

    public Optional<Event> updateEvent(EventUpdateDTO updateDTO) {
        Optional<Event> optionalEvent = eventDAL.findById(updateDTO.getId());
        if (optionalEvent.isEmpty()) return Optional.empty();

        Event event = optionalEvent.get();
        if (updateDTO.getTitle() != null) event.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null) event.setDescription(updateDTO.getDescription());
        if (updateDTO.getLocation() != null) event.setLocation(updateDTO.getLocation());
        if (updateDTO.getStartDate() != null) event.setStartDate(updateDTO.getStartDate());
        if (updateDTO.getEndDate() != null) event.setEndDate(updateDTO.getEndDate());
        if (updateDTO.getCategory() != null) event.setCategory(updateDTO.getCategory());
        if (updateDTO.getTags() != null) event.setTags(updateDTO.getTags());
        if (updateDTO.getPrivate() != null) event.setPrivate(updateDTO.getPrivate());
        event.setUpdatedAt(java.time.LocalDateTime.now());

        return Optional.of(eventDAL.save(event));
    }
    public void deleteEventById(UUID id) {
        eventDAL.deleteById(id);
    }

    private EventResponseDTO mapToDTO(Event event) {
        EventResponseDTO responseDTO = new EventResponseDTO();
        responseDTO.setId(event.getId());
        responseDTO.setTitle(event.getTitle());
        responseDTO.setDescription(event.getDescription());
        responseDTO.setLocation(event.getLocation());
        responseDTO.setStartDate(event.getStartDate());
        responseDTO.setEndDate(event.getEndDate());
        responseDTO.setCategory(event.getCategory());
        responseDTO.setTags(event.getTags());
        responseDTO.setPrivate(event.isPrivate());
        responseDTO.setOrganizerId(event.getOrganizerId());
        return responseDTO;
    }
}
