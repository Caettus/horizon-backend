package com.horizon.userservice.saga;

public enum SagaStatus {
    STARTED,
    AWAITING_CONFIRMATION,
    COMPLETED,
    FAILED
} 