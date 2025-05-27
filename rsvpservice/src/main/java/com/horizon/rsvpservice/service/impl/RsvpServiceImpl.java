package com.horizon.rsvpservice.service.impl;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;
import com.horizon.rsvpservice.repository.RsvpRepository;
import com.horizon.rsvpservice.service.RsvpService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RsvpServiceImpl implements RsvpService {
    private final RsvpRepository rsvpRepository;

    @Override
    @Transactional
    public Rsvp createRsvp(UUID eventId, String userId, RsvpStatus status, String userDisplayName) {
        if (eventId == null || userId == null || userId.trim().isEmpty() || status == null) {
            throw new IllegalArgumentException("Event ID, User ID, and Status cannot be null or empty.");
        }

        Optional<Rsvp> existingRsvp = rsvpRepository.findByEventIdAndUserId(eventId, userId);
        if (existingRsvp.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has already RSVP'd to this event.");
        }

        Rsvp rsvp = new Rsvp();
        rsvp.setEventId(eventId);
        rsvp.setUserId(userId);
        rsvp.setUserDisplayName(userDisplayName);
        rsvp.setStatus(status);
        return rsvpRepository.save(rsvp);
    }

    @Override
    @Transactional
    public Rsvp updateRsvp(Long rsvpId, RsvpStatus status) {
        Rsvp rsvp = rsvpRepository.findById(rsvpId)
                .orElseThrow(() -> new EntityNotFoundException("RSVP not found with id: " + rsvpId));
        rsvp.setStatus(status);
        return rsvpRepository.save(rsvp);
    }

    @Override
    @Transactional
    public void deleteRsvp(Long rsvpId) {
        if (!rsvpRepository.existsById(rsvpId)) {
            throw new EntityNotFoundException("RSVP not found with id: " + rsvpId);
        }
        rsvpRepository.deleteById(rsvpId);
    }

    @Override
    public Rsvp getRsvp(Long rsvpId) {
        return rsvpRepository.findById(rsvpId)
                .orElseThrow(() -> new EntityNotFoundException("RSVP not found with id: " + rsvpId));
    }

    @Override
    public List<Rsvp> getRsvpsByEvent(UUID eventId) {
        return rsvpRepository.findByEventId(eventId);
    }

    @Override
    public List<Rsvp> getRsvpsByUser(String userId) {
        return rsvpRepository.findByUserId(userId);
    }

    @Override
    public Rsvp getRsvpByEventAndUser(UUID eventId, String userId) {
        return rsvpRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("RSVP not found for event %s and user %s", eventId.toString(), userId)));
    }

    @Override
    public List<Rsvp> getRsvpsByEventAndStatus(UUID eventId, RsvpStatus status) {
        return rsvpRepository.findByEventIdAndStatus(eventId, status);
    }

    @Override
    public long countRsvpsByEventAndStatus(UUID eventId, RsvpStatus status) {
        return rsvpRepository.countByEventIdAndStatus(eventId, status);
    }
} 