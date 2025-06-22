package com.horizon.userservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID sagaId;

    private String correlationId; // keycloakId

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    private boolean eventsCleaned = false;
    private boolean rsvpsCleaned = false;

    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public UUID getSagaId() {
        return sagaId;
    }

    public void setSagaId(UUID sagaId) {
        this.sagaId = sagaId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    public boolean isEventsCleaned() {
        return eventsCleaned;
    }

    public void setEventsCleaned(boolean eventsCleaned) {
        this.eventsCleaned = eventsCleaned;
    }

    public boolean isRsvpsCleaned() {
        return rsvpsCleaned;
    }

    public void setRsvpsCleaned(boolean rsvpsCleaned) {
        this.rsvpsCleaned = rsvpsCleaned;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 