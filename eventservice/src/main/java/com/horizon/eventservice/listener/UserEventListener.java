package com.horizon.eventservice.listener;

import com.horizon.eventservice.DAL.EventDAL;
import com.horizon.eventservice.event.UserDeletionRequestedEvent;
import com.horizon.eventservice.model.Event;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class UserEventListener {

    private final EventDAL eventDAL;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserEventListener(EventDAL eventDAL, RabbitTemplate rabbitTemplate) {
        this.eventDAL = eventDAL;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "user.deletion.requested.eventservice.queue")
    public void handleUserDeletionRequested(UserDeletionRequestedEvent event) {
        UUID userId = UUID.fromString(event.getKeycloakId());

        List<Event> events = eventDAL.findAll();
        for (Event e : events) {
            if (e.getOrganizerId().equals(userId)) {
                eventDAL.delete(e);
            } else {
                e.getAllowedUsers().remove(userId);
                e.getAttendees().remove(userId);
                e.getWaitlist().remove(userId);
                eventDAL.save(e);
            }
        }

        rabbitTemplate.convertAndSend("user.deletion.exchange", "user.deletion.events.confirmed", event.getKeycloakId());
    }
} 