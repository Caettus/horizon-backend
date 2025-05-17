package com.horizon.rsvpservice.event;

// Assuming Lombok is available in rsvpservice from its build.gradle
// If not, getters, setters, constructors would be needed explicitly.
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private String keycloakId;
    private String username;
    private String email;
    // Add a toString() method for easy logging if Lombok doesn't provide a good one by default for records or simple DTOs.
    // Lombok @Data should cover this.
} 