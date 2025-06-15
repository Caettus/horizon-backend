package com.horizon.userservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSyncRequestDTO {
    private String keycloakId;
    private String username;
    private String email;
    // Lombok will generate getters and setters
} 