package com.horizon.eventservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.horizon.eventservice.Interface.EventService;
import com.horizon.eventservice.event.UserForgottenEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UserDeletionSagaListener {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserDeletionSagaListener(EventService eventService, ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "horizon.users.forgotten.events")
    public void handleUserForgottenEvent(String message) {
        try {
            UserForgottenEvent event = objectMapper.readValue(message, UserForgottenEvent.class);
            String userId = event.getKeycloakId();
            eventService.removeUserFromAllEvents(userId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 