package com.horizon.rsvpservice.listener;

import com.horizon.common.events.UserRegisteredEvent;
import com.horizon.common.events.UserProfileUpdatedEvent;
import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.repository.RsvpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventListener.class);

    public static final String RSVP_USER_EVENTS_QUEUE = "rsvp.service.user.updates.queue"; // As per plan

    private final RsvpRepository rsvpRepository;

    @Autowired
    public UserEventListener(RsvpRepository rsvpRepository) {
        this.rsvpRepository = rsvpRepository;
    }

    @RabbitListener(queues = RSVP_USER_EVENTS_QUEUE)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        LOGGER.info("Received UserRegisteredEvent: {}", event);
        if (event == null || event.getKeycloakId() == null || event.getUsername() == null) {
            LOGGER.warn("Received incomplete UserRegisteredEvent: {}", event);
            return;
        }

        String keycloakId = event.getKeycloakId();
        String displayName = event.getUsername(); // Username from event is the displayName

        List<Rsvp> rsvps = rsvpRepository.findByUserId(keycloakId);
        if (rsvps != null && !rsvps.isEmpty()) {
            LOGGER.info("Found {} RSVPs for user {} to update displayName.", rsvps.size(), keycloakId);
            for (Rsvp rsvp : rsvps) {
                rsvp.setUserDisplayName(displayName);
                rsvpRepository.save(rsvp);
            }
            LOGGER.info("Finished updating displayNames for user {} from UserRegisteredEvent.", keycloakId);
        } else {
            LOGGER.info("No existing RSVPs found for user {} upon registration event.", keycloakId);
        }
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
        if (event == null || event.getKeycloakId() == null || event.getUpdatedFields() == null || event.getUpdatedFields().isEmpty()) {
            LOGGER.warn("Received incomplete or empty UserProfileUpdatedEvent: {}", event);
            return;
        }

        String keycloakId = event.getKeycloakId();
        Map<String, Object> updatedFields = event.getUpdatedFields();

        if (updatedFields.containsKey("username")) {
            Object usernameUpdateObj = updatedFields.get("username");
            if (usernameUpdateObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> usernameMap = (Map<String, String>) usernameUpdateObj;
                String newUsername = usernameMap.get("newValue");

                if (newUsername != null) {
                    LOGGER.info("Username updated for user {}. New username: {}", keycloakId, newUsername);
                    List<Rsvp> rsvps = rsvpRepository.findByUserId(keycloakId);
                    if (rsvps != null && !rsvps.isEmpty()) {
                        LOGGER.info("Found {} RSVPs for user {} to update displayName.", rsvps.size(), keycloakId);
                        for (Rsvp rsvp : rsvps) {
                            rsvp.setUserDisplayName(newUsername);
                            rsvpRepository.save(rsvp);
                        }
                        LOGGER.info("Finished updating displayNames for user {} from UserProfileUpdatedEvent.", keycloakId);
                    } else {
                        LOGGER.info("No RSVPs found for user {} to update displayName from UserProfileUpdatedEvent.", keycloakId);
                    }
                } else {
                    LOGGER.warn("New username is null in UserProfileUpdatedEvent for user {}.", keycloakId);
                }
            } else {
                LOGGER.warn("Username update field is not in the expected format (Map) for user {}. Received: {}", keycloakId, usernameUpdateObj);
            }
        } else {
            LOGGER.info("UserProfileUpdatedEvent for user {} did not contain a username update.", keycloakId);
        }
    }
} 