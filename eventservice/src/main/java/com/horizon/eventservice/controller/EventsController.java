package com.horizon.eventservice.controller;

import com.horizon.eventservice.DTO.*;
import com.horizon.eventservice.model.Event;
import com.horizon.eventservice.Interface.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
public class EventsController {

    @Autowired
    private EventService eventService;

    @GetMapping("/batch")
    public ResponseEntity<List<EventResponseDTO>> getEventsByIds(@RequestParam("ids") List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<UUID> uuidList = ids.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());

        List<EventResponseDTO> events = eventService.getEventsByIds(uuidList);

        // The original logic returned notFound for empty results, but returning an empty list might be more consistent for a "batch get" operation.
        // If no events match the given IDs, it's not a "not found" error for the resource endpoint itself.
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getEvent(@PathVariable UUID id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        List<EventResponseDTO> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody EventCreateDTO createDTO) {
        Event event = eventService.createEvent(createDTO);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable UUID id, @RequestBody EventUpdateDTO updateDTO) {
        updateDTO.setId(id);
        Optional<Event> updated = eventService.updateEvent(updateDTO);
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        Optional<EventResponseDTO> eventOptional = eventService.getEventById(id)
                .map(dto -> {
                    eventService.deleteEventById(id);
                    return dto;
                });
        return eventOptional.isPresent() ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
