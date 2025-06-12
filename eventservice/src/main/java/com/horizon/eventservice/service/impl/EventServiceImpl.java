package com.horizon.eventservice.service.impl;

import com.horizon.eventservice.dto.EventCreateDTO;
import com.horizon.eventservice.dto.EventResponseDTO;
import com.horizon.eventservice.dto.EventUpdateDTO;
import com.horizon.eventservice.repository.EventRepository;
import com.horizon.eventservice.eventbus.EventPublisher;
import com.horizon.eventservice.model.Event;
import com.horizon.eventservice.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventPublisher eventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

    @Autowired
    public EventServiceImpl(EventRepository eventRepository, EventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<EventResponseDTO> getEventById(UUID id) {
        return eventRepository.findById(id).map(this::mapToDTO);
    }

    @Override
    public List<EventResponseDTO> getAllEvents() {
        return eventRepository.findAll().stream()
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

        Event saved = eventRepository.save(event);
        logger.info("[EventServiceImpl] Saved Event object: {}", saved);
        logger.info("[EventServiceImpl] OrganizerId in Saved Event object: {}", saved.getOrganizerId());

        // publiceer het nieuwe event naar de eventbus
        eventPublisher.publishEventCreated(saved);

        return saved;
    }

    @Override
    public Optional<Event> updateEvent(EventUpdateDTO updateDTO) {
        Optional<Event> optional = eventRepository.findById(updateDTO.getId());
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
        return Optional.of(eventRepository.save(event));
    }

    @Override
    public void deleteEventById(UUID id) {
        eventRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void removeUserFromAllEvents(String userId) {
        eventRepository.deleteAllByOrganizerId(userId);

        List<Event> eventsAsAttendee = eventRepository.findByAttendeesContains(userId);
        for (Event event : eventsAsAttendee) {
            event.getAttendees().remove(userId);
            eventRepository.save(event);
        }

        List<Event> eventsAsWaitlisted = eventRepository.findByWaitlistContains(userId);
        for (Event event : eventsAsWaitlisted) {
            event.getWaitlist().remove(userId);
            eventRepository.save(event);
        }

        List<Event> eventsAsAllowed = eventRepository.findByAllowedUsersContains(userId);
        for (Event event : eventsAsAllowed) {
            event.getAllowedUsers().remove(userId);
            eventRepository.save(event);
        }
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