package com.horizon.eventservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;

    @Column(length = 2000)
    private String description;

    private String location;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean isPrivate;

    @ElementCollection
    private List<UUID> allowedUsers; // voor als de event private is

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer maxAttendees;

    @ElementCollection
    private List<UUID> attendees;

    @ElementCollection
    private List<UUID> waitlist; // misschien handig idk we zullen zien

    private String imageUrl;

    private String category;

    @ElementCollection
    private List<String> tags;

    private UUID organizerId;

    @Enumerated(EnumType.STRING)
    private OrganizerType organizerType;

    private boolean isFlagged;

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public List<UUID> getAllowedUsers() { return allowedUsers; }
    public void setAllowedUsers(List<UUID> allowedUsers) { this.allowedUsers = allowedUsers; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }

    public List<UUID> getAttendees() { return attendees; }
    public void setAttendees(List<UUID> attendees) { this.attendees = attendees; }

    public List<UUID> getWaitlist() { return waitlist; }
    public void setWaitlist(List<UUID> waitlist) { this.waitlist = waitlist; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public UUID getOrganizerId() { return organizerId; }
    public void setOrganizerId(UUID organizerId) { this.organizerId = organizerId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public OrganizerType getOrganizerType() { return organizerType; }
    public void setOrganizerType(OrganizerType organizerType) { this.organizerType = organizerType; }

    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean isFlagged) { this.isFlagged = isFlagged; }

    // Enums
    public enum EventStatus {
        UPCOMING, CANCELLED, COMPLETED
    }

    public enum OrganizerType {
        INDIVIDUAL, BUSINESS
    }
}
