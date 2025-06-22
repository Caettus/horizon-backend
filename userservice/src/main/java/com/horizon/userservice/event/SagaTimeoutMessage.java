package com.horizon.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaTimeoutMessage {
    private UUID sagaId;
    private String keycloakId;
} 