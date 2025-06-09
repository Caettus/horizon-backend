package com.horizon.userservice.DAL;

import com.horizon.userservice.model.UserDeletionSagaState;
import com.horizon.userservice.saga.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeletionSagaStateRepository extends JpaRepository<UserDeletionSagaState, Long> {

    Optional<UserDeletionSagaState> findBySagaId(String sagaId);

    List<UserDeletionSagaState> findByStatusAndCreatedAtBefore(SagaStatus status, LocalDateTime timeout);
} 