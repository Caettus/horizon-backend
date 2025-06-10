package com.horizon.userservice.saga;

import com.horizon.userservice.Interface.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SagaCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(SagaCoordinator.class);

    // In-memory store for active sagas. In a production environment, this should be a persistent store.
    private final Map<String, SagaData> activeSagas = new ConcurrentHashMap<>();

    private final UserService userService;

    public SagaCoordinator(UserService userService) {
        this.userService = userService;
    }

    /**
     * Starts a new user deletion saga.
     * @param sagaId The unique ID for this saga instance.
     * @param userId The ID of the user to be deleted.
     */
    public void startSaga(String sagaId, String userId) {
        Set<String> participants = new HashSet<>();
        participants.add("eventservice");
        participants.add("rsvpservice");
        // Add other services that need to participate in the deletion
        // participants.add("notificationservice");

        activeSagas.put(sagaId, new SagaData(userId, participants));
        logger.info("Saga {} started for user {}", sagaId, userId);
    }

    /**
     * Listens for replies from participating services.
     * @param message The reply message from a service.
     */
    @RabbitListener(queues = "user.deletion.reply.queue")
    public void handleReply(SagaReplyMessage message) {
        logger.info("Received reply for saga {}: {}", message.getSagaId(), message);
        SagaData sagaData = activeSagas.get(message.getSagaId());

        if (sagaData == null) {
            logger.warn("Received reply for unknown or completed saga: {}", message.getSagaId());
            return;
        }

        if (message.isSuccess()) {
            sagaData.onParticipantSuccess(message.getServiceName());
        } else {
            sagaData.onParticipantFailure(message.getServiceName());
            // Implement compensation logic if necessary
            logger.error("Saga {} failed in service {}. Manual intervention may be required.", message.getSagaId(), message.getServiceName());
        }

        if (sagaData.isComplete()) {
            logger.info("Saga {} completed successfully. Finalizing user deletion.", message.getSagaId());
            userService.finalizeUserDeletion(sagaData.getUserId());
            activeSagas.remove(message.getSagaId());
        } else if (sagaData.hasFailed()) {
            logger.error("Saga {} has failed. Not all participants succeeded.", message.getSagaId());
            // Handle saga failure, e.g., notify admin
            activeSagas.remove(message.getSagaId());
        }
    }

    // Inner class to hold the state of a running saga
    private static class SagaData {
        private final String userId;
        private final Set<String> participants;
        private final Set<String> completedParticipants = Collections.synchronizedSet(new HashSet<>());
        private final Set<String> failedParticipants = Collections.synchronizedSet(new HashSet<>());


        public SagaData(String userId, Set<String> participants) {
            this.userId = userId;
            this.participants = Collections.unmodifiableSet(participants);
        }

        public String getUserId() {
            return userId;
        }

        void onParticipantSuccess(String serviceName) {
            completedParticipants.add(serviceName);
        }

        void onParticipantFailure(String serviceName) {
            failedParticipants.add(serviceName);
        }

        boolean isComplete() {
            return failedParticipants.isEmpty() && completedParticipants.containsAll(participants);
        }

        boolean hasFailed() {
            return !failedParticipants.isEmpty();
        }
    }
}
