package com.horizon.userservice.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EventCreatedListener {
    private static final AtomicInteger eventCount = new AtomicInteger(0);
    private static final Logger logger = LoggerFactory.getLogger(EventCreatedListener.class);

    @RabbitListener(queues = EventbusRabbitMQConfig.QUEUE_EVENT_CREATED)
    public void onEventCreated(EventCreatedMessage event) {
        int newCount = eventCount.incrementAndGet();
        logger.info("[UserService] New event created: {} (total so far: {})", event.getTitle(), newCount);
    }

    public int getEventCount() {
        return eventCount.get();
    }
}

