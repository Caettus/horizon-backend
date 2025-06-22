package com.horizon.userservice.saga;

import com.horizon.userservice.event.SagaTimeoutMessage;
import com.horizon.userservice.model.SagaState;
import com.horizon.userservice.model.SagaStatus;
import com.horizon.userservice.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SagaTimeoutListener {

    private final SagaStateRepository sagaStateRepository;
    private final SagaCompletionService sagaCompletionService;

    @RabbitListener(queues = "saga.timeout.queue")
    @Transactional
    public void handleTimeout(SagaTimeoutMessage message) {
        System.out.println("Received saga timeout message: " + message);

        SagaState sagaState = sagaStateRepository.findById(message.getSagaId())
                .orElse(null);

        if (sagaState == null) {
            System.err.println("SagaState not found for sagaId: " + message.getSagaId() + ". Cannot process timeout.");
            return;
        }

        if (sagaState.getStatus() == SagaStatus.USER_MARKED_FOR_DELETION) {
            System.out.println("Saga " + sagaState.getSagaId() + " has timed out. Triggering compensation.");
            sagaCompletionService.failSaga(sagaState, "Saga timed out");
        } else {
            System.out.println("Saga " + sagaState.getSagaId() + " has already completed or failed. Ignoring timeout message.");
        }
    }
} 