package com.horizon.userservice.model;

public enum SagaStatus {
    STARTED,
    USER_MARKED_FOR_DELETION,
    EVENTS_CLEANED,
    RSVPS_CLEANED,
    COMPLETED,
    FAILED
} 