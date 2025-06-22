package com.horizon.eventservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForgottenEvent {
    private UUID sagaId;
    private String keycloakId;
} 