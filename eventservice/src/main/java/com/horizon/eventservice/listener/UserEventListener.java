package com.horizon.eventservice.listener;

import com.horizon.eventservice.DAL.EventDAL;
import com.horizon.eventservice.event.UserDeletionRequestedEvent;
import com.horizon.eventservice.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);
    private final EventDAL eventDAL;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserEventListener(EventDAL eventDAL, RabbitTemplate rabbitTemplate) {
        this.eventDAL = eventDAL;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "user.deletion.requested.eventservice.queue")
    @Transactional
    public void handleUserDeletionRequested(UserDeletionRequestedEvent event) {
        String keycloakId = event.getKeycloakId();
        logger.info("Handling user deletion request for keycloakId: {}", keycloakId);

        try {
            // Delete all events organized by the user
            eventDAL.deleteByOrganizerId(keycloakId);
            logger.info("Deleted events organized by user {}", keycloakId);

            // Find all events where the user is a participant (attendee, waitlisted, or allowed)
            List<Event> participantEvents = eventDAL.findEventsWithUserAsParticipant(keycloakId);

            // Remove the user from the collections in the found events
            for (Event e : participantEvents) {
                boolean modified = e.getAttendees().remove(keycloakId);
                modified |= e.getWaitlist().remove(keycloakId);
                modified |= e.getAllowedUsers().remove(keycloakId);

                if (modified) {
                    eventDAL.save(e);
                }
            }
            logger.info("Removed user {} from all event participation lists", keycloakId);

            // Confirm that the deletion process for this service is complete
            rabbitTemplate.convertAndSend("user.deletion.exchange", "user.deletion.events.confirmed", keycloakId);
            logger.info("Confirmed user deletion for keycloakId: {}", keycloakId);

        } catch (Exception e) {
            logger.error("Error processing user deletion for keycloakId: {}. Sending failure event.", keycloakId, e);
            rabbitTemplate.convertAndSend("user.deletion.exchange", "user.deletion.events.failed", keycloakId);
            // We do not rethrow, to avoid the message being requeued. The saga will handle the failure.
        }
    }
} 