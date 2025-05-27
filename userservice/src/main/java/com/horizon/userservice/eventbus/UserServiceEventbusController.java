package com.horizon.userservice.eventbus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.UUID;

import java.util.Collections;

@RestController
@RequestMapping("/internal/eventbus/userservice")
public class UserServiceEventbusController {

    private final EventCreatedListener listener;

    public UserServiceEventbusController(EventCreatedListener listener) {
        this.listener = listener;
    }

    // GET http://localhost:8082/internal/eventbus/userservice/event-count/{userId}
    @GetMapping("/event-count/{userId}")
    public Map<String, Integer> getEventCount(@PathVariable UUID userId) {
        return Collections.singletonMap("count", listener.getEventCount(userId));
    }
}
