package com.horizon.rsvpservice.event;

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
    // Lombok @Data should cover toString() for logging.
} 