package com.horizon.eventservice.controller;

import com.horizon.eventservice.DTO.*;
import com.horizon.eventservice.model.Event;
import com.horizon.eventservice.Interface.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventsController {

    @Autowired
    private EventService eventService;

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
