package com.horizon.rsvpservice.listener;

import com.horizon.rsvpservice.event.UserRegisteredEvent;
import com.horizon.rsvpservice.event.UserProfileUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventListener.class);

    public static final String RSVP_USER_EVENTS_QUEUE = "rsvp.service.user.updates.queue"; // As per plan

    @RabbitListener(queues = RSVP_USER_EVENTS_QUEUE)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        LOGGER.info("Received UserRegisteredEvent: {}", event);
        // Future: Denormalize user data if needed, e.g., update local cache of display names.
    }

    // To listen to multiple event types on the same queue, you might need separate methods
    // or a generic Message<T> handler with type checking if all events are sent with type headers.
    // For simplicity with @RabbitListener, distinct methods with specific event types are common if a MessageConverter is configured.
    // If UserProfileUpdatedEvent is also sent to the same queue without specific type handling by the message converter,
    // you might need a single method that takes a generic Object or Message and then does instanceof checks.
    // However, if the JSON serializer can distinguish them, two methods might work if they have different GSON/Jackson mappings.

    // Assuming the default Spring AMQP message converter can distinguish based on type if they are distinct on the wire.
    // If not, a single method handling Object and then checking `instanceof` is safer.
    // Let's try with two distinct methods first. This relies on message type information being present
    // for the converter, or that their JSON structures are uniquely identifiable by the converter.

    @RabbitListener(queues = RSVP_USER_EVENTS_QUEUE) // Listening on the same queue
    public void handleUserProfileUpdatedEvent(UserProfileUpdatedEvent event) {
        LOGGER.info("Received UserProfileUpdatedEvent: {}", event);
        // Future: Update denormalized user data if any field in event.getUpdatedFields() is relevant.
    }
} 