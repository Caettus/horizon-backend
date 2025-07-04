package com.horizon.eventservice.Interface;

import com.horizon.eventservice.DTO.EventCreateDTO;
import com.horizon.eventservice.DTO.EventResponseDTO;
import com.horizon.eventservice.DTO.EventUpdateDTO;
import com.horizon.eventservice.DAL.EventDAL;
import com.horizon.eventservice.eventbus.EventPublisher;
import com.horizon.eventservice.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventDAL eventDAL;
    private final EventPublisher eventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

    @Autowired
    public EventServiceImpl(EventDAL eventDAL, EventPublisher eventPublisher) {
        this.eventDAL = eventDAL;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<EventResponseDTO> getEventById(UUID id) {
        return eventDAL.findById(id).map(this::mapToDTO);
    }

    @Override
    public List<EventResponseDTO> getAllEvents() {
        return eventDAL.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Event createEvent(EventCreateDTO createDTO) {
        logger.info("[EventServiceImpl] Received EventCreateDTO: {}", createDTO);
        logger.info("[EventServiceImpl] OrganizerId from DTO: {}", createDTO.getOrganizerId());

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle(createDTO.getTitle());
        event.setDescription(createDTO.getDescription());
        event.setLocation(createDTO.getLocation());
        event.setStartDate(createDTO.getStartDate());
        event.setEndDate(createDTO.getEndDate());
        event.setCategory(createDTO.getCategory());
        event.setTags(createDTO.getTags());
        event.setPrivate(createDTO.isPrivate());
        event.setOrganizerId(createDTO.getOrganizerId());
        event.setImageUrl(createDTO.getImageUrl());
        event.setStatus(Event.EventStatus.UPCOMING);
        event.setCreatedAt(LocalDateTime.now());

        logger.info("[EventServiceImpl] Event object before save: {}", event);
        logger.info("[EventServiceImpl] OrganizerId in Event object before save: {}", event.getOrganizerId());

        Event saved = eventDAL.save(event);
        logger.info("[EventServiceImpl] Saved Event object: {}", saved);
        logger.info("[EventServiceImpl] OrganizerId in Saved Event object: {}", saved.getOrganizerId());

        // publiceer het nieuwe event naar de eventbus
        eventPublisher.publishEventCreated(saved);

        return saved;
    }

    @Override
    public Optional<Event> updateEvent(EventUpdateDTO updateDTO) {
        Optional<Event> optional = eventDAL.findById(updateDTO.getId());
        if (optional.isEmpty()) return Optional.empty();
        Event event = optional.get();

        if (updateDTO.getTitle() != null)       event.setTitle(updateDTO.getTitle());
        if (updateDTO.getDescription() != null) event.setDescription(updateDTO.getDescription());
        if (updateDTO.getLocation() != null)    event.setLocation(updateDTO.getLocation());
        if (updateDTO.getStartDate() != null)   event.setStartDate(updateDTO.getStartDate());
        if (updateDTO.getEndDate() != null)     event.setEndDate(updateDTO.getEndDate());
        if (updateDTO.getCategory() != null)    event.setCategory(updateDTO.getCategory());
        if (updateDTO.getTags() != null)        event.setTags(updateDTO.getTags());
        if (updateDTO.getPrivate() != null)     event.setPrivate(updateDTO.getPrivate());

        event.setUpdatedAt(LocalDateTime.now());
        return Optional.of(eventDAL.save(event));
    }

    @Override
    public void deleteEventById(UUID id) {
        eventDAL.deleteById(id);
    }

    private EventResponseDTO mapToDTO(Event event) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setLocation(event.getLocation());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        dto.setCategory(event.getCategory());
        dto.setTags(event.getTags());
        dto.setPrivate(event.isPrivate());
        dto.setOrganizerId(event.getOrganizerId());
        return dto;
    }
}
