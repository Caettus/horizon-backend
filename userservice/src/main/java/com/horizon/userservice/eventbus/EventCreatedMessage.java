package com.horizon.userservice.eventbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message die wordt gepubliceerd wanneer een nieuwe Event is aangemaakt.
 */
public class EventCreatedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String title;
    private final LocalDateTime startDate;
    private final LocalDateTime createdAt;
    private final UUID organizerId;

    @JsonCreator
    public EventCreatedMessage(
            @JsonProperty("id") UUID id,
            @JsonProperty("title") String title,
            @JsonProperty("startDate") LocalDateTime startDate,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("organizerId") UUID organizerId
    ) {
        this.id = id;
        this.title = title;
        this.startDate = startDate;
        this.createdAt = createdAt;
        this.organizerId = organizerId;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    @Override
    public String toString() {
        return "EventCreatedMessage{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDate=" + startDate +
                ", createdAt=" + createdAt +
                ", organizerId=" + organizerId +
                '}';
    }
}
