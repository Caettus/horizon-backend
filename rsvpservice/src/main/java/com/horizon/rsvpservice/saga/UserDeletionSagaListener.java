package com.horizon.rsvpservice.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.horizon.common.events.UserForgottenEvent;
import com.horizon.rsvpservice.service.RsvpService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UserDeletionSagaListener {

    private final RsvpService rsvpService;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserDeletionSagaListener(RsvpService rsvpService, ObjectMapper objectMapper) {
        this.rsvpService = rsvpService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "horizon.users.forgotten.rsvps")
    public void handleUserForgottenEvent(String message) {
        try {
            UserForgottenEvent event = objectMapper.readValue(message, UserForgottenEvent.class);
            String userId = event.getKeycloakId();
            rsvpService.deleteRsvpsByUserId(userId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 