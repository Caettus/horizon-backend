package com.horizon.eventservice.saga;

import com.horizon.eventservice.Interface.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserDeletionSagaListener {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletionSagaListener.class);

    private final EventService eventService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.application.name}")
    private String serviceName;

    public UserDeletionSagaListener(EventService eventService, RabbitTemplate rabbitTemplate) {
        this.eventService = eventService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = SagaRabbitMQConfig.QUEUE_NAME)
    public void onUserDeletionRequest(UserDeletionMessage message) {
        logger.info("Received user deletion request for saga {} and user {}", message.getSagaId(), message.getUserId());
        SagaReplyMessage reply;
        try {
            eventService.deleteEventsByUserId(message.getUserId());
            logger.info("Successfully deleted events for user {}", message.getUserId());
            reply = new SagaReplyMessage(message.getSagaId(), serviceName, true);
        } catch (Exception e) {
            logger.error("Failed to delete events for user {}", message.getUserId(), e);
            reply = new SagaReplyMessage(message.getSagaId(), serviceName, false);
        }

        rabbitTemplate.convertAndSend("user.deletion.reply.queue", reply);
        logger.info("Sent reply for saga {}", message.getSagaId());
    }
}
