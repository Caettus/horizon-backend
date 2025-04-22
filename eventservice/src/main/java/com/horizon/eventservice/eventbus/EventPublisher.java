package com.horizon.eventservice.eventbus;

import com.horizon.eventservice.model.Event;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishEventCreated(Event event) {
        EventCreatedMessage message = new EventCreatedMessage(
                event.getId(), event.getTitle(), event.getStartDate(), event.getCreatedAt()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_EVENT_CREATED,
                message
        );
    }
}
