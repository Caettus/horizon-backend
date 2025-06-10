package com.horizon.rsvpservice.service;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;

import java.util.List;
import java.util.UUID;

public interface RsvpService {
    Rsvp createRsvp(UUID eventId, String userId, RsvpStatus status, String userDisplayName);
    Rsvp updateRsvp(Long rsvpId, RsvpStatus status);
    void deleteRsvp(Long rsvpId);
    Rsvp getRsvp(Long rsvpId);
    List<Rsvp> getRsvpsByEvent(UUID eventId);
    List<Rsvp> getRsvpsByUser(String userId);
    void deleteRsvpsByUserId(String userId);
    Rsvp getRsvpByEventAndUser(UUID eventId, String userId);
    List<Rsvp> getRsvpsByEventAndStatus(UUID eventId, RsvpStatus status);
    long countRsvpsByEventAndStatus(UUID eventId, RsvpStatus status);
} 