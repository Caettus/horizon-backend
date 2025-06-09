package com.horizon.rsvpservice.listener;

import com.horizon.rsvpservice.event.UserRegisteredEvent;
import com.horizon.rsvpservice.event.UserProfileUpdatedEvent;
import com.horizon.rsvpservice.event.UserDeletionRequestedEvent;
import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.repository.RsvpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class UserEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventListener.class);

    public static final String RSVP_USER_EVENTS_QUEUE = "rsvp.service.user.updates.queue"; // As per plan

    private final RsvpRepository rsvpRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserEventListener(RsvpRepository rsvpRepository, RabbitTemplate rabbitTemplate) {
        this.rsvpRepository = rsvpRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RSVP_USER_EVENTS_QUEUE)
    @Transactional
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        LOGGER.info("Received UserRegisteredEvent: {}", event);
        if (event == null || event.getKeycloakId() == null || event.getUsername() == null) {
            LOGGER.warn("Received incomplete UserRegisteredEvent: {}", event);
            return;
        }

        String keycloakId = event.getKeycloakId();
        String displayName = event.getUsername(); // Username from event is the displayName

        rsvpRepository.updateUserDisplayName(keycloakId, displayName);
        LOGGER.info("Updated displayNames for user {} from UserRegisteredEvent.", keycloakId);
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
    @Transactional
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
                    rsvpRepository.updateUserDisplayName(keycloakId, newUsername);
                    LOGGER.info("Finished updating displayNames for user {} from UserProfileUpdatedEvent.", keycloakId);
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

    @RabbitListener(queues = "user.deletion.requested.rsvpservice.queue")
    @Transactional
    public void handleUserDeletionRequested(UserDeletionRequestedEvent event) {
        LOGGER.info("Received UserDeletionRequestedEvent: {}", event);
        if (event == null || event.getKeycloakId() == null) {
            LOGGER.warn("Received incomplete UserDeletionRequestedEvent: {}", event);
            // Consider sending a negative acknowledgement or logging to a specific error queue
            return;
        }

        String keycloakId = event.getKeycloakId();
        
        try {
            rsvpRepository.deleteByUserId(keycloakId);
            LOGGER.info("Successfully deleted RSVPs for user {}.", keycloakId);
            rabbitTemplate.convertAndSend("user.deletion.exchange", "user.deletion.rsvps.confirmed", event.getKeycloakId());
        } catch (Exception e) {
            LOGGER.error("Failed to delete RSVPs for user {}: {}", keycloakId, e.getMessage());
            // Send a failure event
            rabbitTemplate.convertAndSend("user.deletion.exchange", "user.deletion.rsvps.failed", event.getKeycloakId());
        }
    }
} 