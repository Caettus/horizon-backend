package com.horizon.eventservice.saga;

import com.horizon.eventservice.Interface.EventService;
import com.horizon.eventservice.event.SagaReplyMessage;
import com.horizon.eventservice.event.UserForgottenEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserDeletionSagaListener {

    private final EventService eventService;
    private final RabbitTemplate rabbitTemplate;
    private static final String REPLY_EXCHANGE = "saga.replies.exchange";
    private static final String REPLY_ROUTING_KEY = "saga.replies.queue";
    private static final String SERVICE_NAME = "eventservice";

    @Autowired
    public UserDeletionSagaListener(EventService eventService, RabbitTemplate rabbitTemplate) {
        this.eventService = eventService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "horizon.users.forgotten.events")
    public void handleUserForgottenEvent(UserForgottenEvent event) {
        SagaReplyMessage reply;
        try {
            String userId = event.getKeycloakId();
            eventService.removeUserFromAllEvents(userId);
            System.out.println("Successfully removed user " + userId + " from all events.");
            reply = new SagaReplyMessage(event.getSagaId(), true, SERVICE_NAME, null);
        } catch (Exception e) {
            System.err.println("Failed to remove user " + event.getKeycloakId() + " from events. Error: " + e.getMessage());
            reply = new SagaReplyMessage(event.getSagaId(), false, SERVICE_NAME, e.getMessage());
        }

        rabbitTemplate.convertAndSend(REPLY_EXCHANGE, REPLY_ROUTING_KEY, reply);
        System.out.println("Sent saga reply from eventservice for sagaId: " + event.getSagaId());
    }
} 