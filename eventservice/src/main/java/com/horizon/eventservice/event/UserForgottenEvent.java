package com.horizon.eventservice.event;

public class UserForgottenEvent {
    private String keycloakId;

    public UserForgottenEvent() {
    }

    public UserForgottenEvent(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }
} 