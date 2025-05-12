package com.horizon.rsvpservice.service;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;

import java.util.List;

public interface RsvpService {
    Rsvp createRsvp(Long eventId, Long userId, RsvpStatus status);
    Rsvp updateRsvp(Long rsvpId, RsvpStatus status);
    void deleteRsvp(Long rsvpId);
    Rsvp getRsvp(Long rsvpId);
    List<Rsvp> getRsvpsByEvent(Long eventId);
    List<Rsvp> getRsvpsByUser(Long userId);
    Rsvp getRsvpByEventAndUser(Long eventId, Long userId);
    List<Rsvp> getRsvpsByEventAndStatus(Long eventId, RsvpStatus status);
    long countRsvpsByEventAndStatus(Long eventId, RsvpStatus status);
} 