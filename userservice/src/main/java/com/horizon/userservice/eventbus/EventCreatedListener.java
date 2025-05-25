package com.horizon.userservice.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.model.User;

@Service
public class EventCreatedListener {
    // private static final Map<UUID, AtomicInteger> userEventCounts = new ConcurrentHashMap<>(); // No longer needed for count logic
    private static final Logger logger = LoggerFactory.getLogger(EventCreatedListener.class);
    private final UserDAL userDAL;

    public EventCreatedListener(UserDAL userDAL) {
        this.userDAL = userDAL;
    }

    @RabbitListener(queues = EventbusRabbitMQConfig.QUEUE_EVENT_CREATED)
    public void onEventCreated(EventCreatedMessage event) {
        logger.info("[EventCreatedListener] Received event: {}", event.toString());
        UUID organizerId = event.getOrganizerId();
        if (organizerId == null) {
            logger.warn("[EventCreatedListener] Received EventCreatedMessage with null organizerId. Event ID: {}", event.getId());
            return;
        }
        logger.info("[EventCreatedListener] Processing event for organizerId: {}", organizerId);

        User user = userDAL.findByKeycloakId(organizerId.toString()).orElse(null);

        if (user != null) {
            int currentCount = user.getEventsCreatedCount(); // Relies on User.java getter handling null
            int newCount = currentCount + 1;
            user.setEventsCreatedCount(newCount);
            userDAL.save(user);
            logger.info("[EventCreatedListener] Incremented and saved event count for user: {}. New count: {}", organizerId, newCount);
        } else {
            logger.warn("[EventCreatedListener] User not found with keycloakId: {}. Cannot increment event count.", organizerId);
        }
    }

    public int getEventCount(UUID organizerId) {
        logger.info("[EventCreatedListener] getEventCount called for organizerId: {}", organizerId);
        if (organizerId == null) {
            logger.warn("[EventCreatedListener] getEventCount called with null organizerId. Returning 0.");
            return 0;
        }
        User user = userDAL.findByKeycloakId(organizerId.toString()).orElse(null);
        if (user != null) { // The getEventsCreatedCount() != null check is removed as User.java handles it
            logger.info("[EventCreatedListener] Count for organizerId {}: {}.", organizerId, user.getEventsCreatedCount());
            return user.getEventsCreatedCount();
        }
        logger.warn("[EventCreatedListener] User not found for organizerId: {}. Returning 0.", organizerId);
        return 0;
    }
}

