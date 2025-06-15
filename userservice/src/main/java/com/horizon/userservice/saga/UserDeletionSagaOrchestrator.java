package com.horizon.userservice.saga;

import com.horizon.common.events.UserForgottenEvent;
import com.horizon.userservice.service.UserService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserDeletionSagaOrchestrator implements UserDeletionSaga {

    private final UserService userService;
    private final RabbitTemplate rabbitTemplate;
    private static final Logger logger = LoggerFactory.getLogger(UserDeletionSagaOrchestrator.class);

    @Autowired
    public UserDeletionSagaOrchestrator(UserService userService, RabbitTemplate rabbitTemplate) {
        this.userService = userService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void execute(String keycloakId) {
        // In a more complex scenario, we would have state management for the saga.
        // For now, we just perform the local deletion and publish the event.

        // 1. Delete user from local database
        userService.deleteUserByKeycloakId(keycloakId);

        // 2. Publish event for other services
        UserForgottenEvent event = new UserForgottenEvent(keycloakId);
        rabbitTemplate.convertAndSend("horizon.users.exchange", "user.forgotten", event);
        logger.info("Published UserForgottenEvent for keycloakId: {}", keycloakId);
    }
} 