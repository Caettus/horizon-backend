package com.horizon.userservice.saga;

import com.horizon.userservice.Interface.UserService;
import com.horizon.userservice.model.SagaState;
import com.horizon.userservice.model.SagaStatus;
import com.horizon.userservice.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SagaCompletionService {

    private final SagaStateRepository sagaStateRepository;
    private final UserService userService;

    @Transactional
    public void failSaga(SagaState sagaState, String reason) {
        if (sagaState.getStatus() != SagaStatus.FAILED && sagaState.getStatus() != SagaStatus.COMPLETED) {
            sagaState.setStatus(SagaStatus.FAILED);
            sagaState.setErrorMessage(reason);
            sagaStateRepository.save(sagaState);
            userService.revertUserDeletion(sagaState.getCorrelationId());
            System.out.println("Saga " + sagaState.getSagaId() + " failed. User deletion reverted.");
        }
    }

    @Transactional
    public void checkForSagaCompletion(SagaState sagaState) {
        if (sagaState.isEventsCleaned() && sagaState.isRsvpsCleaned()) {
            System.out.println("All steps completed for saga " + sagaState.getSagaId() + ". Committing final action.");
            userService.completeUserDeletion(sagaState.getCorrelationId());
            sagaState.setStatus(SagaStatus.COMPLETED);
            sagaStateRepository.save(sagaState);
            System.out.println("Saga " + sagaState.getSagaId() + " completed successfully.");
        }
    }
} 