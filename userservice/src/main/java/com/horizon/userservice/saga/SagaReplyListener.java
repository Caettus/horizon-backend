package com.horizon.userservice.saga;

import com.horizon.userservice.Interface.UserService;
import com.horizon.userservice.event.SagaReplyMessage;
import com.horizon.userservice.model.SagaState;
import com.horizon.userservice.model.SagaStatus;
import com.horizon.userservice.repository.SagaStateRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SagaReplyListener {

    private final SagaStateRepository sagaStateRepository;
    private final UserService userService;

    private static final String EVENT_SERVICE = "eventservice";
    private static final String RSVP_SERVICE = "rsvpservice";

    @Autowired
    public SagaReplyListener(SagaStateRepository sagaStateRepository, UserService userService) {
        this.sagaStateRepository = sagaStateRepository;
        this.userService = userService;
    }

    @RabbitListener(queues = "saga.replies.queue")
    @Transactional
    public void handleReply(SagaReplyMessage message) {
        System.out.println("Received saga reply: " + message);

        SagaState sagaState = sagaStateRepository.findById(message.getSagaId())
                .orElse(null);

        if (sagaState == null) {
            System.err.println("SagaState not found for sagaId: " + message.getSagaId() + ". Cannot process reply.");
            return;
        }
        
        if (sagaState.getStatus() == SagaStatus.COMPLETED || sagaState.getStatus() == SagaStatus.FAILED) {
            System.out.println("Saga " + sagaState.getSagaId() + " is already completed or failed. Ignoring message.");
            return;
        }

        if (!message.isSuccess()) {
            handleFailure(sagaState, message.getSourceService(), message.getFailureReason());
            return;
        }

        handleSuccess(sagaState, message.getSourceService());
    }

    private void handleFailure(SagaState sagaState, String sourceService, String reason) {
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setErrorMessage("Failure from " + sourceService + ": " + reason);
        sagaStateRepository.save(sagaState);
        userService.revertUserDeletion(sagaState.getCorrelationId());
        System.out.println("Saga " + sagaState.getSagaId() + " failed. User deletion reverted.");
    }

    private void handleSuccess(SagaState sagaState, String sourceService) {
        if (EVENT_SERVICE.equals(sourceService)) {
            sagaState.setEventsCleaned(true);
        } else if (RSVP_SERVICE.equals(sourceService)) {
            sagaState.setRsvpsCleaned(true);
        }
        
        sagaStateRepository.save(sagaState);
        System.out.println("Saga " + sagaState.getSagaId() + " processed success from " + sourceService);

        checkForSagaCompletion(sagaState);
    }

    private void checkForSagaCompletion(SagaState sagaState) {
        if (sagaState.isEventsCleaned() && sagaState.isRsvpsCleaned()) {
            System.out.println("All steps completed for saga " + sagaState.getSagaId() + ". Committing final action.");
            userService.completeUserDeletion(sagaState.getCorrelationId());
            sagaState.setStatus(SagaStatus.COMPLETED);
            sagaStateRepository.save(sagaState);
            System.out.println("Saga " + sagaState.getSagaId() + " completed successfully.");
        }
    }
} 