package com.horizon.userservice.repository;

import com.horizon.userservice.model.UserDeletionSagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDeletionSagaRepository extends JpaRepository<UserDeletionSagaState, Long> {
    Optional<UserDeletionSagaState> findBySagaId(String sagaId);
} 