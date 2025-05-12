package com.horizon.rsvpservice.service.impl;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;
import com.horizon.rsvpservice.repository.RsvpRepository;
import com.horizon.rsvpservice.service.RsvpService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RsvpServiceImpl implements RsvpService {
    private final RsvpRepository rsvpRepository;

    @Override
    @Transactional
    public Rsvp createRsvp(Long eventId, Long userId, RsvpStatus status) {
        Rsvp rsvp = new Rsvp();
        rsvp.setEventId(eventId);
        rsvp.setUserId(userId);
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
    public List<Rsvp> getRsvpsByEvent(Long eventId) {
        return rsvpRepository.findByEventId(eventId);
    }

    @Override
    public List<Rsvp> getRsvpsByUser(Long userId) {
        return rsvpRepository.findByUserId(userId);
    }

    @Override
    public Rsvp getRsvpByEventAndUser(Long eventId, Long userId) {
        return rsvpRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("RSVP not found for event %d and user %d", eventId, userId)));
    }

    @Override
    public List<Rsvp> getRsvpsByEventAndStatus(Long eventId, RsvpStatus status) {
        return rsvpRepository.findByEventIdAndStatus(eventId, status);
    }

    @Override
    public long countRsvpsByEventAndStatus(Long eventId, RsvpStatus status) {
        return rsvpRepository.countByEventIdAndStatus(eventId, status);
    }
} 