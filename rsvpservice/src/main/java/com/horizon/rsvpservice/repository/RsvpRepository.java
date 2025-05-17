package com.horizon.rsvpservice.repository;

import com.horizon.rsvpservice.model.Rsvp;
import com.horizon.rsvpservice.model.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RsvpRepository extends JpaRepository<Rsvp, Long> {
    List<Rsvp> findByEventId(Long eventId);
    List<Rsvp> findByUserId(String userId);
    Optional<Rsvp> findByEventIdAndUserId(Long eventId, String userId);
    List<Rsvp> findByEventIdAndStatus(Long eventId, RsvpStatus status);
    long countByEventIdAndStatus(Long eventId, RsvpStatus status);
} 