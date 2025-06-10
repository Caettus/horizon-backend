package com.horizon.userservice.saga;

import com.horizon.userservice.config.RabbitMQConfig;
import com.horizon.userservice.model.User;
import com.horizon.userservice.model.UserDeletionSagaState;
import com.horizon.userservice.model.UserDeletionSagaStatus;
import com.horizon.userservice.repository.UserDeletionSagaRepository;
import com.horizon.userservice.repository.UserRepository;
import com.horizon.userservice.resource.UsersResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class UserDeletionSaga {
    private static final Logger logger = LoggerFactory.getLogger(UserDeletionSaga.class);

    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;
    private final UserDeletionSagaRepository sagaRepository;
    private final UsersResource usersResource;

    public UserDeletionSaga(
            RabbitTemplate rabbitTemplate,
            UserRepository userRepository,
            UserDeletionSagaRepository sagaRepository,
            UsersResource usersResource) {
        this.rabbitTemplate = rabbitTemplate;
        this.userRepository = userRepository;
        this.sagaRepository = sagaRepository;
        this.usersResource = usersResource;
    }

    public void startSaga(String keycloakId) {
        String sagaId = UUID.randomUUID().toString();
        UserDeletionSagaState saga = new UserDeletionSagaState();
        saga.setSagaId(sagaId);
        saga.setStatus(UserDeletionSagaStatus.STARTED);
        saga.setCreatedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                "user.deletion.requested",
                keycloakId
        );
    }

    @RabbitListener(queues = RabbitMQConfig.REQUESTED_QUEUE)
    @Transactional
    public void handleUserDeletionRequested(String keycloakId) {
        logger.info("Received user deletion request for keycloakId: {}", keycloakId);
        UserDeletionSagaState saga = sagaRepository.findBySagaId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Saga not found for keycloakId: " + keycloakId));

        try {
            // Send request to events service
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "user.deletion.events.requested",
                    keycloakId
            );

            // Send request to rsvps service
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "user.deletion.rsvps.requested",
                    keycloakId
            );

            saga.setStatus(UserDeletionSagaStatus.AWAITING_CONFIRMATION);
            sagaRepository.save(saga);
        } catch (Exception e) {
            logger.error("Error processing user deletion request", e);
            saga.setStatus(UserDeletionSagaStatus.FAILED);
            saga.setFailureReason(e.getMessage());
            sagaRepository.save(saga);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.EVENTS_CONFIRMED_QUEUE)
    @Transactional
    public void handleEventsDeletionConfirmed(String keycloakId) {
        logger.info("Received events deletion confirmation for keycloakId: {}", keycloakId);
        UserDeletionSagaState saga = sagaRepository.findBySagaId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Saga not found for keycloakId: " + keycloakId));

        String confirmedServices = saga.getConfirmedServices();
        if (confirmedServices == null) {
            confirmedServices = "events";
        } else {
            confirmedServices += ",events";
        }
        saga.setConfirmedServices(confirmedServices);

        if (confirmedServices.contains("rsvps")) {
            completeSaga(saga, keycloakId);
        } else {
            sagaRepository.save(saga);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.RSVPS_CONFIRMED_QUEUE)
    @Transactional
    public void handleRsvpsDeletionConfirmed(String keycloakId) {
        logger.info("Received rsvps deletion confirmation for keycloakId: {}", keycloakId);
        UserDeletionSagaState saga = sagaRepository.findBySagaId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Saga not found for keycloakId: " + keycloakId));

        String confirmedServices = saga.getConfirmedServices();
        if (confirmedServices == null) {
            confirmedServices = "rsvps";
        } else {
            confirmedServices += ",rsvps";
        }
        saga.setConfirmedServices(confirmedServices);

        if (confirmedServices.contains("events")) {
            completeSaga(saga, keycloakId);
        } else {
            sagaRepository.save(saga);
        }
    }

    private void completeSaga(UserDeletionSagaState saga, String keycloakId) {
        try {
            // Delete user from Keycloak
            usersResource.delete(keycloakId);

            // Delete user from database
            userRepository.deleteByKeycloakId(keycloakId);

            // Update saga status
            saga.setStatus(UserDeletionSagaStatus.COMPLETED);
            sagaRepository.save(saga);

            // Send final confirmation
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "user.deletion.confirmed",
                    keycloakId
            );
        } catch (Exception e) {
            logger.error("Error completing user deletion saga", e);
            saga.setStatus(UserDeletionSagaStatus.FAILED);
            saga.setFailureReason(e.getMessage());
            sagaRepository.save(saga);
        }
    }
} 