package com.horizon.userservice.saga;

public interface UserDeletionSaga {
    void execute(String keycloakId);
} 