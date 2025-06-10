package com.horizon.userservice.saga;

import java.io.Serializable;

// TODO: You should create these two classes in each participating microservice
// or in a shared library.

/**
 * Message sent to initiate a data deletion for a user.
 */
public class UserDeletionMessage implements Serializable {
    private String sagaId;
    private String userId;

    public UserDeletionMessage() {}

    public UserDeletionMessage(String sagaId, String userId) {
        this.sagaId = sagaId;
        this.userId = userId;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "UserDeletionMessage{" +
                "sagaId='" + sagaId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}


/**
 * Message sent back from a participant to the saga coordinator.
 */
class SagaReplyMessage implements Serializable {
    private String sagaId;
    private String serviceName;
    private boolean success;

    public SagaReplyMessage() {}

    public SagaReplyMessage(String sagaId, String serviceName, boolean success) {
        this.sagaId = sagaId;
        this.serviceName = serviceName;
        this.success = success;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "SagaReplyMessage{" +
                "sagaId='" + sagaId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", success=" + success +
                '}';
    }
}
