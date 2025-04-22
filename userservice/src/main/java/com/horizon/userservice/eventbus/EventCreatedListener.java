package com.horizon.userservice.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class EventCreatedListener {

    private static final Logger logger = LoggerFactory.getLogger(EventCreatedListener.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EVENT_CREATED)
    public void onEventCreated(EventCreatedEvent event) {
        logger.info("[UserService] New event created: {}", event.getTitle());
    }
}
