package com.horizon.userservice.saga;

import com.horizon.userservice.Interface.UserService;
import com.horizon.userservice.configuration.UserExchangeRabbitMQConfig;
import com.horizon.userservice.event.SagaTimeoutMessage;
import com.horizon.userservice.event.UserForgottenEvent;
import com.horizon.userservice.model.SagaState;
import com.horizon.userservice.model.SagaStatus;
import com.horizon.userservice.repository.SagaStateRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDeletionSagaOrchestrator implements UserDeletionSaga {

    private final UserService userService;
    private final RabbitTemplate rabbitTemplate;
    private final SagaStateRepository sagaStateRepository;

    @Autowired
    public UserDeletionSagaOrchestrator(UserService userService, RabbitTemplate rabbitTemplate, SagaStateRepository sagaStateRepository) {
        this.userService = userService;
        this.rabbitTemplate = rabbitTemplate;
        this.sagaStateRepository = sagaStateRepository;
    }

    @Override
    @Transactional
    public void execute(String keycloakId) {
        // 1. Create and persist the initial saga state
        SagaState sagaState = new SagaState();
        sagaState.setCorrelationId(keycloakId);
        sagaState.setStatus(SagaStatus.STARTED);
        SagaState savedSagaState = sagaStateRepository.save(sagaState);

        // 2. Perform the first local transaction: mark user for deletion
        userService.markUserForDeletion(keycloakId);

        // 3. Update saga state
        savedSagaState.setStatus(SagaStatus.USER_MARKED_FOR_DELETION);
        sagaStateRepository.save(savedSagaState);

        // 4. Publish event for other services
        UserForgottenEvent event = new UserForgottenEvent(savedSagaState.getSagaId(), keycloakId);
        rabbitTemplate.convertAndSend("horizon.users.exchange", "user.forgotten", event);
        System.out.println("Published UserForgottenEvent for keycloakId: " + keycloakId + " with sagaId: " + savedSagaState.getSagaId());

        // 5. Dispatch saga timeout message
        SagaTimeoutMessage timeoutMessage = new SagaTimeoutMessage(savedSagaState.getSagaId(), keycloakId);
        rabbitTemplate.convertAndSend(UserExchangeRabbitMQConfig.SAGA_TIMEOUT_EXCHANGE, UserExchangeRabbitMQConfig.SAGA_TIMEOUT_ROUTING_KEY, timeoutMessage, message -> {
            message.getMessageProperties().setHeader("x-delay", 30000);
            return message;
        });
    }
} 