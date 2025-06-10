package com.horizon.userservice.saga;

import com.horizon.userservice.Interface.UserService;
import com.horizon.userservice.event.UserForgottenEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserDeletionSagaOrchestrator implements UserDeletionSaga {

    private final UserService userService;
    private final RabbitTemplate rabbitTemplate;

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
        System.out.println("Published UserForgottenEvent for keycloakId: " + keycloakId);
    }
} 