package com.horizon.userservice.saga;

import com.horizon.userservice.DAL.UserDAL;
import com.horizon.userservice.DAL.UserDeletionSagaStateRepository;
import com.horizon.userservice.configuration.UserDeletionRabbitMQConfig;
import com.horizon.userservice.event.UserDeletionConfirmedEvent;
import com.horizon.userservice.event.UserDeletionRequestedEvent;
import com.horizon.userservice.model.UserDeletionSagaState;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class UserDeletionSaga {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletionSaga.class);

    private final RabbitTemplate rabbitTemplate;
    private final Keycloak keycloak;
    private final UserDAL userDAL;
    private final UserDeletionSagaStateRepository sagaStateRepository;
    private final Set<String> expectedConfirmations;
    private final long sagaTimeoutMinutes;

    @Autowired
    public UserDeletionSaga(RabbitTemplate rabbitTemplate,
                            Keycloak keycloak,
                            UserDAL userDAL,
                            UserDeletionSagaStateRepository sagaStateRepository,
                            @Value("${saga.deletion.participating-services}") String participatingServices,
                            @Value("${saga.deletion.timeout-minutes}") long sagaTimeoutMinutes) {
        this.rabbitTemplate = rabbitTemplate;
        this.keycloak = keycloak;
        this.userDAL = userDAL;
        this.sagaStateRepository = sagaStateRepository;
        this.expectedConfirmations = new HashSet<>(Arrays.asList(participatingServices.split(",")));
        this.sagaTimeoutMinutes = sagaTimeoutMinutes;
    }

    public void startSaga(UserDeletionRequestedEvent event) {
        String keycloakId = event.getKeycloakId();
        logger.info("Starting user deletion saga for keycloakId: {}", keycloakId);

        // Check if a saga is already in progress
        if (sagaStateRepository.findBySagaId(keycloakId).isPresent()) {
            logger.warn("User deletion saga for keycloakId: {} is already in progress. Ignoring new request.", keycloakId);
            return;
        }

        UserDeletionSagaState sagaState = new UserDeletionSagaState();
        sagaState.setSagaId(keycloakId);
        sagaState.setStatus(SagaStatus.STARTED);
        sagaStateRepository.save(sagaState);

        // Publish event to trigger deletion in other services
        rabbitTemplate.convertAndSend(UserDeletionRabbitMQConfig.EXCHANGE_NAME, "user.deletion.requested", event);
        logger.info("Published UserDeletionRequestedEvent for keycloakId: {}", keycloakId);

        sagaState.setStatus(SagaStatus.AWAITING_CONFIRMATION);
        sagaStateRepository.save(sagaState);
    }

    @RabbitListener(queues = "user.deletion.events.confirmed")
    public void onEventsConfirmed(String keycloakId) {
        handleConfirmation(keycloakId, "events");
    }

    @RabbitListener(queues = "user.deletion.rsvps.confirmed")
    public void onRsvpsConfirmed(String keycloakId) {
        handleConfirmation(keycloakId, "rsvps");
    }

    @RabbitListener(queues = "user.deletion.events.failed")
    public void onEventsFailed(String keycloakId) {
        handleFailure(keycloakId, "events");
    }

    @RabbitListener(queues = "user.deletion.rsvps.failed")
    public void onRsvpsFailed(String keycloakId) {
        handleFailure(keycloakId, "rsvps");
    }

    private void handleFailure(String keycloakId, String service) {
        logger.error("Received failure from service: {} for keycloakId: {}. Marking saga as FAILED.", service, keycloakId);
        sagaStateRepository.findBySagaId(keycloakId).ifPresent(sagaState -> {
            if (sagaState.getStatus() == SagaStatus.FAILED || sagaState.getStatus() == SagaStatus.COMPLETED) {
                logger.warn("Received failure for a saga that is already terminated. SagaId: {}, Status: {}", keycloakId, sagaState.getStatus());
                return;
            }
            sagaState.setStatus(SagaStatus.FAILED);
            sagaState.setFailureReason("Service '" + service + "' reported failure.");
            sagaStateRepository.save(sagaState);
            triggerAlert(sagaState);
        });
    }

    private void handleConfirmation(String keycloakId, String service) {
        logger.info("Received confirmation from service: {} for keycloakId: {}", service, keycloakId);
        sagaStateRepository.findBySagaId(keycloakId).ifPresent(sagaState -> {
            if (sagaState.getStatus() != SagaStatus.AWAITING_CONFIRMATION) {
                logger.warn("Received confirmation for a saga that is not awaiting confirmation. SagaId: {}, Status: {}", keycloakId, sagaState.getStatus());
                return;
            }

            String currentConfirmations = sagaState.getConfirmedServices();
            Set<String> confirmedSet = currentConfirmations == null || currentConfirmations.isEmpty()
                    ? new HashSet<>()
                    : new HashSet<>(Arrays.asList(currentConfirmations.split(",")));

            if (confirmedSet.contains(service)) {
                logger.warn("Received duplicate confirmation from service: {} for keycloakId: {}", service, keycloakId);
                return;
            }
            
            confirmedSet.add(service);
            sagaState.setConfirmedServices(String.join(",", confirmedSet));

            if (confirmedSet.containsAll(expectedConfirmations)) {
                logger.info("All confirmations received for keycloakId: {}. Proceeding with final deletion.", keycloakId);
                completeSaga(sagaState);
            } else {
                sagaStateRepository.save(sagaState);
            }
        });
    }

    @Retryable(
        value = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    private void completeSaga(UserDeletionSagaState sagaState) {
        String keycloakId = sagaState.getSagaId();
        logger.info("Attempting to complete saga for keycloakId: {}", keycloakId);
        
        // This method will now rethrow exceptions to trigger the retry mechanism.
        // The @Recover method will handle the final failure.

        // Delete from Keycloak
        RealmResource realmResource = keycloak.realm("horizon-realm");
        UsersResource usersResource = realmResource.users();
        usersResource.delete(keycloakId);
        logger.info("User {} deleted from Keycloak.", keycloakId);

        // Delete from user service DB
        userDAL.findByKeycloakId(keycloakId).ifPresent(user -> {
            userDAL.delete(user);
            logger.info("User {} deleted from user service database.", keycloakId);
        });

        // Publish confirmation event
        rabbitTemplate.convertAndSend(UserDeletionRabbitMQConfig.EXCHANGE_NAME, "user.deletion.confirmed", new UserDeletionConfirmedEvent(keycloakId));
        logger.info("Published UserDeletionConfirmedEvent for {}", keycloakId);

        sagaState.setStatus(SagaStatus.COMPLETED);
        sagaStateRepository.save(sagaState);
    }

    @Recover
    private void recoverCompleteSaga(Exception e, UserDeletionSagaState sagaState) {
        String keycloakId = sagaState.getSagaId();
        logger.error("Final attempt to complete saga for {} failed after multiple retries: {}. Marking as FAILED.", keycloakId, e.getMessage(), e);
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setFailureReason("Final deletion step failed after multiple retries: " + e.getMessage());
        sagaStateRepository.save(sagaState);
        triggerAlert(sagaState);
    }

    private void triggerAlert(UserDeletionSagaState sagaState) {
        logger.warn("ALERT: Saga {} failed. Reason: {}. Manual intervention required.", sagaState.getSagaId(), sagaState.getFailureReason());
        // In a real system, this would publish to a dedicated alerting queue, send an email, or create a ticket.
        rabbitTemplate.convertAndSend("user.deletion.saga.failed", "", sagaState);
    }

    @Scheduled(fixedRateString = "${saga.deletion.timeout-check-rate-ms:300000}") // Run every 5 minutes by default
    public void checkForTimedOutSagas() {
        logger.debug("Checking for timed-out user deletion sagas...");
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(sagaTimeoutMinutes);
        List<UserDeletionSagaState> timedOutSagas = sagaStateRepository
                .findByStatusAndCreatedAtBefore(SagaStatus.AWAITING_CONFIRMATION, timeoutThreshold);

        for (UserDeletionSagaState saga : timedOutSagas) {
            logger.warn("Saga for keycloakId: {} has timed out. Marking as FAILED.", saga.getSagaId());
            saga.setStatus(SagaStatus.FAILED);
            saga.setFailureReason("Saga timed out waiting for confirmations.");
            sagaStateRepository.save(saga);
            triggerAlert(saga);
        }
    }
} 