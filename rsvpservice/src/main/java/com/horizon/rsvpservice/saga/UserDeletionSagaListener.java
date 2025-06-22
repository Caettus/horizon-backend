package com.horizon.rsvpservice.saga;

import com.horizon.rsvpservice.event.SagaReplyMessage;
import com.horizon.rsvpservice.event.UserForgottenEvent;
import com.horizon.rsvpservice.service.RsvpService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserDeletionSagaListener {

    private final RsvpService rsvpService;
    private final RabbitTemplate rabbitTemplate;

    private static final String REPLY_EXCHANGE = "saga.replies.exchange";
    private static final String REPLY_ROUTING_KEY = "saga.replies.queue";
    private static final String SERVICE_NAME = "rsvpservice";

    @Autowired
    public UserDeletionSagaListener(RsvpService rsvpService, RabbitTemplate rabbitTemplate) {
        this.rsvpService = rsvpService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "horizon.users.forgotten.rsvps")
    public void handleUserForgottenEvent(UserForgottenEvent event) {
        SagaReplyMessage reply;
        try {
            String userId = event.getKeycloakId();
            rsvpService.deleteRsvpsByUserId(userId);
            System.out.println("Successfully deleted RSVPs for user " + userId);
            reply = new SagaReplyMessage(event.getSagaId(), true, SERVICE_NAME, null);
        } catch (Exception e) {
            System.err.println("Failed to delete RSVPs for user " + event.getKeycloakId() + ". Error: " + e.getMessage());
            reply = new SagaReplyMessage(event.getSagaId(), false, SERVICE_NAME, e.getMessage());
        }

        rabbitTemplate.convertAndSend(REPLY_EXCHANGE, REPLY_ROUTING_KEY, reply);
        System.out.println("Sent saga reply from rsvpservice for sagaId: " + event.getSagaId());
    }
} 