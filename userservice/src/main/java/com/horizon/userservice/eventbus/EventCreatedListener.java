package com.horizon.userservice.eventbus;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class EventCreatedListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EVENT_CREATED)
    public void onEventCreated(EventCreatedEvent event) {
        // voorbeeld van actie die ik kan doen:
        // - een notificatie sturen
        // - simpelweg loggen
        System.out.println("[UserService] Nieuw event aangemaakt: " + event.getTitle());
    }
}
