package com.horizon.userservice.model;

public enum UserDeletionSagaStatus {
    STARTED,
    AWAITING_CONFIRMATION,
    COMPLETED,
    FAILED
} 