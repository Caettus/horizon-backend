package com.horizon.rsvpservice.repository;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RsvpRepository extends JpaRepository<Rsvp, Long> {
    List<Rsvp> findByEventId(UUID eventId);
    List<Rsvp> findByUserId(String userId);
    Optional<Rsvp> findByEventIdAndUserId(UUID eventId, String userId);
    List<Rsvp> findByEventIdAndStatus(UUID eventId, RsvpStatus status);
    long countByEventIdAndStatus(UUID eventId, RsvpStatus status);
} 