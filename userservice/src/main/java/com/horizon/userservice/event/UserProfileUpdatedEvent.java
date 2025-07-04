package com.horizon.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdatedEvent {
    private String keycloakId;
    private Map<String, Object> updatedFields;
} 