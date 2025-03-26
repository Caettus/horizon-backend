package com.horizon.eventservice.controller;

import com.horizon.eventservice.DTO.EventDTO;
import com.horizon.eventservice.Interface.EventInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class EventsController {

    @Autowired
    private EventInterface eventInterface;

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getUserDetails(@PathVariable("id") int id) {
        EventDTO response = eventInterface.getEventById(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/test")
    public String testEndpoint() {
        return "EventController is working!";
    }
}