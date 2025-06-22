package com.horizon.userservice.saga;

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
    private final SagaCompletionService sagaCompletionService;

    private static final String EVENT_SERVICE = "eventservice";
    private static final String RSVP_SERVICE = "rsvpservice";

    @Autowired
    public SagaReplyListener(SagaStateRepository sagaStateRepository, SagaCompletionService sagaCompletionService) {
        this.sagaStateRepository = sagaStateRepository;
        this.sagaCompletionService = sagaCompletionService;
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
        String failureReason = "Failure from " + sourceService + ": " + reason;
        sagaCompletionService.failSaga(sagaState, failureReason);
    }

    private void handleSuccess(SagaState sagaState, String sourceService) {
        if (EVENT_SERVICE.equals(sourceService)) {
            sagaState.setEventsCleaned(true);
        } else if (RSVP_SERVICE.equals(sourceService)) {
            sagaState.setRsvpsCleaned(true);
        }
        
        sagaStateRepository.save(sagaState);
        System.out.println("Saga " + sagaState.getSagaId() + " processed success from " + sourceService);

        sagaCompletionService.checkForSagaCompletion(sagaState);
    }
} 